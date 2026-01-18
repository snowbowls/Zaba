package events;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static events.UserStatEvent.dotenv;

public class GenResponseEvent extends ListenerAdapter {
    String apiKey = dotenv.get("JWT");
    private final List<AIchat.ChatMessage> chatHistory = new ArrayList<>();
    private final int MAX_HISTORY = 20;
    public static boolean isJarvisOnline() {
        try (java.net.Socket socket = new java.net.Socket()) {
            // Try to connect to localhost:3000 with a 1-second timeout
            socket.connect(new java.net.InetSocketAddress("192.168.1.223", 3000), 1000);
            return true;
        } catch (java.io.IOException e) {
            return false; // Server is down or port is blocked
        }
    }
    static class ChatMessage {
        String role;
        String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    private String callGatekeeperAI(String prompt){
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("model", "gatekeeper");
            payload.addProperty("stream", false);

            JsonArray messages = new JsonArray();
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", prompt); // Gson handles escaping automatically!
            messages.add(userMessage);

            payload.add("messages", messages);

            String jsonBody = new Gson().toJson(payload);

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://192.168.1.223:3000/api/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choicesArr = jsonObject.getAsJsonArray("choices");
            JsonObject choicesObj = choicesArr.get(0).getAsJsonObject();
            JsonObject messageObj = choicesObj.getAsJsonObject("message");
            return messageObj.get("content").getAsString().trim().toUpperCase();

        } catch (Exception e) {
            System.out.println(e);
            return "LOW_VALUE";
        }
    }
    private void callZabaAI(MessageReceivedEvent event){
        String content = event.getMessage().getContentRaw().toLowerCase();
        boolean startsWithTrigger = content.startsWith("zaba");
        String userPrompt = event.getMessage().getContentRaw();
        if (startsWithTrigger) {
            userPrompt = userPrompt.substring(4).trim();
        }

        chatHistory.add(new AIchat.ChatMessage("user", userPrompt));

        while (chatHistory.size() > MAX_HISTORY) {
            chatHistory.remove(0);
        }

        //Convert the entire history list to a JSON array using Gson
        Gson gson = new Gson();
        JsonArray messagesArray = new JsonArray();

        // Always start with your System Prompt (Personality)
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are Zaba, an AI Discord bot with personality.");
        messagesArray.add(systemMsg);

        // Add the rolling history
        for (AIchat.ChatMessage msg : chatHistory) {
            JsonObject m = new JsonObject();
            m.addProperty("role", msg.role);
            m.addProperty("content", msg.content);
            messagesArray.add(m);
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messagesArray.add(userMsg);

        JsonObject finalBody = new JsonObject();
        finalBody.addProperty("model", "zaba");
        finalBody.add("messages", messagesArray);
        finalBody.addProperty("stream", false);

        String jsonBody = new Gson().toJson(finalBody);

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://192.168.1.223:3000/api/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Send the request and reply to Discord
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                    String aiResponse = jsonObject.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .get("message").getAsJsonObject()
                            .get("content").getAsString();

                    // Add Jarvis's response to the history so he remembers what HE said
                    synchronized (chatHistory) {
                        chatHistory.add(new AIchat.ChatMessage("assistant", aiResponse));
                        if (chatHistory.size() > MAX_HISTORY) chatHistory.remove(0);
                    }
                    event.getChannel().sendMessage(aiResponse).queue();
                });
    }

    // GenResponseEvents are actions that can occur in casual conversation.
    // You can think of this class as where most of the 'personality' of the bot is handled.
    // Naming convention can be done better but the gist is this class is used for handling the JSON file
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String msg = event.getMessage().getContentRaw().toLowerCase();

        String[] help = msg.split("\\s");
        if(help[0].equalsIgnoreCase(("!help"))){
            if(help.length == 1){
                List<String> command = new ArrayList<>();
                List<String> use = new ArrayList<>();

                StringBuilder commands = new StringBuilder();
                StringBuilder uses = new StringBuilder();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setThumbnail("https://images-ext-1.discordapp.net/external/HwnSW1Qv1B0_ZkomUohZ7P-TYmFsX775K0H4CdQRbAw/https/e0.pxfuel.com/wallpapers/940/704/desktop-wallpaper-glass-animals-zaba-artwork-by-micah-lidberg-glass-animals-pool-thumbnail.jpg");
                eb.setTitle("Zaba", null);
                eb.setColor(new Color(114, 41, 54));

                command.add("!help player\n");
                use.add("Lists commands for the audio player\n");
                command.add("!help poll\n");
                use.add("Explains how the poll function works\n");
                command.add("!help socialcredit\n");
                use.add("Explains how social credit works\n");
                command.add("!help swearjar\n");
                use.add("Explains how the swear jar works\n");
                command.add("!help contra\n");
                use.add("Explains how the contribution tracker works\n");

                for(String s : command)
                    commands.append(s);
                for(String s: use)
                    uses.append(s);

                eb.addField("Command", commands.toString(), true);
                eb.addField("Function", uses.toString(), true);
                MessageCreateData data = new MessageCreateBuilder()
                        .addEmbeds(eb.build())
                        .build();
                event.getChannel().sendMessage(data).queue();
                return;
            }
            if(help[1].equalsIgnoreCase("player")){
                List<String> command = new ArrayList<>();
                List<String> use = new ArrayList<>();

                StringBuilder commands = new StringBuilder();
                StringBuilder uses = new StringBuilder();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setThumbnail("https://images-ext-1.discordapp.net/external/HwnSW1Qv1B0_ZkomUohZ7P-TYmFsX775K0H4CdQRbAw/https/e0.pxfuel.com/wallpapers/940/704/desktop-wallpaper-glass-animals-zaba-artwork-by-micah-lidberg-glass-animals-pool-thumbnail.jpg");
                eb.setTitle("Audio Player", null);
                eb.setColor(new Color(114, 41, 54));

                command.add("!play\n");
                use.add("Adds or plays songs from the current queue\n");
//                command.add("!leave\n");
//                use.add("Leaves the voice channel\n");
//                command.add("!pplay\n");
//                use.add("Adds a playlist to the queue\n");
//                command.add("!pause\n");
//                use.add("Pauses audio playback\n");
                command.add("!stop\n");
                use.add("Completely stops audio playback\n");
                command.add("!skip\n");
                use.add("Skips the current song\n");
//                command.add("!nowplaying\n");
//                use.add("Prints information about the current song\n");
//                command.add("!np\n");
//                use.add("Alias for nowplaying\n");
                command.add("!queue\n");
                use.add("Lists the songs in the queue\n");
//                command.add("!volume [val]\n");
//                use.add("Sets the volume of the MusicPlayer [10 - 100]\n");
//                command.add("!restart\n");
//                use.add("Restarts the current song\n");
                command.add("!loop\n");
                use.add("Toggles the player to repeat current song\n");
                command.add("!clear\n");
                use.add("Completely clears the queue\n");
                command.add("!join\n");
                use.add("Joins the voice channel\n");
                command.add("!leave\n");
                use.add("Leaves the voice channel\n");

//                command.add("!reset\n");
//                use.add("Completely resets the player for a quick fix\n");

                for(String s : command)
                    commands.append(s);
                for(String s: use)
                    uses.append(s);

                eb.addField("Command", commands.toString(), true);
                eb.addField("Function", uses.toString(), true);
                MessageCreateData data = new MessageCreateBuilder()
                        .addEmbeds(eb.build())
                        .build();
                event.getChannel().sendMessage(data).queue();
            }
            if(help[1].equalsIgnoreCase("poll")){
                List<String> command = new ArrayList<>();
                List<String> call = new ArrayList<>();
                List<String> calldesc = new ArrayList<>();


                StringBuilder commands = new StringBuilder();
                StringBuilder calls = new StringBuilder();
                StringBuilder calldescs = new StringBuilder();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setThumbnail("https://images-ext-1.discordapp.net/external/HwnSW1Qv1B0_ZkomUohZ7P-TYmFsX775K0H4CdQRbAw/https/e0.pxfuel.com/wallpapers/940/704/desktop-wallpaper-glass-animals-zaba-artwork-by-micah-lidberg-glass-animals-pool-thumbnail.jpg");
                eb.setTitle("Poll Function", null);
                eb.setColor(new Color(114, 41, 54));

                command.add("Send '!poll' followed by up to 10 options separated by commas\n\n");
                command.add("Example: !poll TITLE HERE, Option 1, Option 2, Option 3, ...\n");
                call.add("!poll headcount\n");
                calldesc.add("Options for going, not going, and don't know yet");

                for(String s : command)
                    commands.append(s);
                for(String s: call)
                    calls.append(s);
                for(String s: calldesc)
                    calldescs.append(s);

                eb.addField("Command Explanation", commands.toString(), false);
                eb.addField("Premade Polls", calls.toString(), true);
                eb.addField("Description", calldescs.toString(), true);
                MessageCreateData data = new MessageCreateBuilder()
                        .addEmbeds(eb.build())
                        .build();
                event.getChannel().sendMessage(data).queue();
            }
            if(help[1].equalsIgnoreCase("socialcredit")){
                event.getChannel().sendMessage("多黨制\t\t**Social Credit - How to perform your 中国共产党 Duty**"
                        + "\n*李洪志*\t   ● React to a comment with <:15_plus:900119408859578451> or <:15_minus:934919187787288597> to contribute to the author's social credit"
                        + "\n*六四天*\t   ● If the author and the reactor are the same user, their social credit is left unchanged"
                        + "\n*劉曉波*\t   ● If the react is removed, the author's social credit will update accordingly"
                        + "\n*李洪志*\t   ● Use '!show all' or '!show mine' to view social credit scores").queue();
                return;
            }
            if(help[1].equalsIgnoreCase("contra")){
                List<String> command = new ArrayList<>();
                List<String> call = new ArrayList<>();
                List<String> calldesc = new ArrayList<>();


                StringBuilder commands = new StringBuilder();
                StringBuilder calls = new StringBuilder();
                StringBuilder calldescs = new StringBuilder();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setThumbnail("https://images-ext-1.discordapp.net/external/HwnSW1Qv1B0_ZkomUohZ7P-TYmFsX775K0H4CdQRbAw/https/e0.pxfuel.com/wallpapers/940/704/desktop-wallpaper-glass-animals-zaba-artwork-by-micah-lidberg-glass-animals-pool-thumbnail.jpg");
                eb.setTitle("Contribution Tracker", null);
                eb.setColor(new Color(114, 41, 54));

                command.add("This function allows users to claim an item on a list, providing a comprehensive tracker for who brings or does what for an event.\n\n");
                command.add("Any user can add, remove, or reset the list. You can claim as many different items on the list as you want.\n");
                call.add("!contra\n\n");
                calldesc.add("Summons tracker and allows users to add their name\n");
                call.add("!contra add XYZ\n\n\n");
                calldesc.add("Replace XYZ with the name of the item you'd like to add to the list (spaces allowed)\n");
                call.add("!contra del XYZ\n\n\n");
                calldesc.add("Replace XYZ with the name of the item you'd like remove from the list (case sensitive)\n");
                call.add("!contra clear\n\n");
                calldesc.add("Resets the tracker, deleting all users and entries\n");

                for(String s : command)
                    commands.append(s);
                for(String s: call)
                    calls.append(s);
                for(String s: calldesc)
                    calldescs.append(s);

                eb.addField("Function Explanation", commands.toString(), false);
                eb.addField("Commands", calls.toString(), true);
                eb.addField("Description", calldescs.toString(), true);
                MessageCreateData data = new MessageCreateBuilder()
                        .addEmbeds(eb.build())
                        .build();
                event.getChannel().sendMessage(data).queue();
            }
            if(help[1].equalsIgnoreCase("swearjar")){
                List<String> command = new ArrayList<>();
                List<String> call = new ArrayList<>();
                List<String> calldesc = new ArrayList<>();


                StringBuilder commands = new StringBuilder();
                StringBuilder calls = new StringBuilder();
                StringBuilder calldescs = new StringBuilder();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setThumbnail("https://images-ext-1.discordapp.net/external/HwnSW1Qv1B0_ZkomUohZ7P-TYmFsX775K0H4CdQRbAw/https/e0.pxfuel.com/wallpapers/940/704/desktop-wallpaper-glass-animals-zaba-artwork-by-micah-lidberg-glass-animals-pool-thumbnail.jpg");
                eb.setTitle("Contribution Tracker", null);
                eb.setColor(new Color(114, 41, 54));

                command.add("Zaba tracks everyone's use of specific words. The master word list is unknown to everyone but Zaba. \n\n");
                call.add("!swearjar\n");
                calldesc.add("Shows all words used by the author\n");
                call.add("!swearjar all\n");
                calldesc.add("Shows top 3 most used words of all users in the server");

                for(String s : command)
                    commands.append(s);
                for(String s: call)
                    calls.append(s);
                for(String s: calldesc)
                    calldescs.append(s);

                eb.addField("Function Explanation", commands.toString(), false);
                eb.addField("Commands", calls.toString(), true);
                eb.addField("Description", calldescs.toString(), true);
                MessageCreateData data = new MessageCreateBuilder()
                        .addEmbeds(eb.build())
                        .build();
                event.getChannel().sendMessage(data).queue();
            }
        }

        List<String> unapprovedChannels = new ArrayList<>();
        unapprovedChannels.add("946443239630733322");
        unapprovedChannels.add("954377064247599154");

        JSONParser parser = new JSONParser();
        JSONObject gen = null;
        JSONObject genEx = null;
        JSONObject genCom = null;
        JSONObject jsonObject = null;
        JSONObject mto = null;
        JSONObject genEm = null;
        JSONObject emoteList = null;
        JSONObject responseList = null;
        JSONObject genProb = null;
        JSONObject genZaba = null;

        try {
            Object obj = parser.parse(new FileReader("keywords.json"));
            jsonObject = (JSONObject) obj;
            gen = (JSONObject) jsonObject.get("general");
            genEx = (JSONObject) jsonObject.get("generalExact");
            genCom = (JSONObject) jsonObject.get("generalComplex");
            mto = (JSONObject) jsonObject.get("manyToOne");
            genEm = (JSONObject) jsonObject.get("generalEmote");
            genProb = (JSONObject) jsonObject.get("generalProb");
            emoteList = (JSONObject) jsonObject.get("emotes");
            responseList = (JSONObject) jsonObject.get("responseList");
            genZaba = (JSONObject) jsonObject.get("generalZaba");


        } catch (Exception e) {
            e.printStackTrace();
        }


        // GenEm or General Emote will react with an emote whenever a trigger word / phrase is detected in a message
        assert genEm != null;
        assert emoteList != null;
        Set<String> scanGenEm = genEm.keySet();
        for (String str : scanGenEm) {
            if(msg.contains(str)){
                String emote = emoteList.get(genEm.get(str)).toString();
                event.getMessage().addReaction(Emoji.fromUnicode(emote)).queue();
            }
        }

        if(unapprovedChannels.contains(event.getChannel().getId()))
            return;

        if(isJarvisOnline()){
            String judgment = callGatekeeperAI(msg); // This method calls the prompt above
            System.out.println(judgment);
            switch (judgment) {
                case "INSULT":
                    callZabaAI(event);
                    break;
                case "TECH_HELP":
                case "DIRECT_QUERY":
                    callZabaAI(event);
                    break;

                case "LOW_VALUE":
                default:
                    break;
            }


        }



        // Gen or General will respond whenever a trigger word / phrase is detected in a message
        assert gen != null;
        Set<String> scanGen = gen.keySet();
        for (String str : scanGen) {
            if(msg.contains(str)){
                System.out.println( " #" + event.getChannel().getName() + " @" + event.getMessage().getAuthor().getName());
                event.getChannel().sendMessage(gen.get(str).toString()).queue();
            }
        }

        // GenEx or General Exact will respond only when the message in question matches exactly with the trigger list
        assert genEx != null;
        Set<String> scanGenEx = genEx.keySet();
        String msgClean= msg.replaceAll("[^a-zA-Z0-9 ]", "");
        for (String str : scanGenEx) {
            if(msgClean.equals(str)){
                System.out.println( " #" + event.getChannel().getName() + " @" + event.getMessage().getAuthor().getName());
                event.getChannel().sendMessage(genEx.get(msgClean).toString()).queue();
            }
        }

        // GenCom or General Complex will trigger from a list of words and pull a random response from another list
        assert genCom != null;
        for (int i = 1; i <= genCom.size(); i++){
            JSONObject genScan = (JSONObject) genCom.get(String.valueOf(i));
            String str = genScan.keySet().toString();
            String key = str.substring(1, str.length() - 1);
            if (msg.contains(key)) {
                JSONObject responses = (JSONObject) jsonObject.get(genScan.get(key));
                int rand = (int)((Math.random() * (responses.size() - 1)) + 1);
                String meme =  responses.get(String.valueOf(rand)).toString();

                if(key.equals("uwu")) {
                    if (Math.random() > .65) {
                        System.out.println(key + " #" + event.getChannel().getName() + " @" + event.getMessage().getAuthor().getName());
                        MessageCreateData data = new MessageCreateBuilder()
                                .setFiles(FileUpload.fromData(new File("videos/" + genScan.get(key) + "/" + meme)))
                                .build();
                        event.getChannel().sendMessage(data).queue();
                    }
                }
                else {
                    System.out.println(key + " #" + event.getChannel().getName() + " @" + event.getMessage().getAuthor().getName());
                    MessageCreateData data = new MessageCreateBuilder()
                            .setFiles(FileUpload.fromData(new File("videos/" + genScan.get(key) + "/" + meme)))
                            .build();
                    event.getChannel().sendMessage(data).queue();
                }
            }
        }

        // GenProb or General Probability when triggered will pull from a list of responses accompanied by a chance of the response being given
        assert genProb != null;
        Set<String> scanProb = genProb.keySet();
        for (String str : scanProb) {
            if(msg.contains(str)){
                String resp = genProb.get(str).toString();
                String[] split = resp.split("\\;");
                if(Math.random() < Float.parseFloat(split[1])/100)
                    event.getChannel().sendMessage(split[0]).queue();
            }
        }

        // GenZaba or General Zaba is like General but specifically when talking to Zaba, keeps things organized
        assert genZaba != null;
        Set<String> scanZaba = genZaba.keySet();
        for (String str : scanZaba) {
            if(msg.contains(str)){
                String resp = genZaba.get(str).toString();
                event.getChannel().sendMessage(resp).queue();
            }
        }


        // mto or Many To One is...
        assert mto != null;
        assert responseList != null;
        Set<String> scanMto = mto.keySet();
        for (String str : scanMto) {
            if(msg.contains(str)){
                String resp = responseList.get(mto.get(str)).toString();
                event.getChannel().sendMessage(resp).queue();
            }
        }
    }
}
