package events;

import com.google.gson.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
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
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static events.UserStatEvent.dotenv;

class SanitizeAIOutput {

    // Hard-coded list of random replacement strings
    private static final List<String> REPLACEMENTS = Arrays.asList(
            "*nice try*", "*nope*", "*try something else*", "*goku*"
    );

    private static final Random RANDOM = new Random();

    /**
     * Checks a string for specific sequences and replaces them with
     * a random string from the hard-coded REPLACEMENTS list.
     */
    public static String sanitizeOutput(MessageReceivedEvent event, String message) {
        if (message == null) return message;

        // Pattern matches <@ID> or <@!ID> and @everyone
        // Group 1 captures the specific ID if it's a mention
        Pattern pattern = Pattern.compile("(<@!?(\\d+)>|@everyone|@here)");
        Matcher matcher = pattern.matcher(message);

        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // Append the text before the match
            sb.append(message, lastEnd, matcher.start());

            // Get a random replacement
            String randomReplacement = REPLACEMENTS.get(RANDOM.nextInt(REPLACEMENTS.size()));
            sb.append(randomReplacement);

            // Update the last end position
            lastEnd = matcher.end();

            event.getJDA().getUserById("232295708974120960").openPrivateChannel().queue((channel) -> {
                channel.sendMessage("goku").queue();
            }, (error) -> {
                System.out.println("Busted");
            });
        }

        // Append any remaining text after the last match
        sb.append(message.substring(lastEnd));

        return sb.toString();
    }
}

class RelationshipManager {
    // Key: Discord User ID (String), Value: System Instruction
    private static final Map<String, String> userContexts = new HashMap<>();

    static {
        userContexts.put("232295708974120960",
                "(((SYSTEM: This is your old battle buddy. You've been through Hell and back with them.)))");

        userContexts.put("222163619125788682",
                "(((SYSTEM: This user is a known nuisance. Be cautious and don't give into his demands. Retort if called for.)))");
    }

    public static String getContextForUser(String userId) {
        // This should now print the correct instruction instead of null/empty
        //System.out.println("Lookup ID: " + userId);
        //System.out.println("Result: " + userContexts.getOrDefault(userId, "Default"));

        return userContexts.getOrDefault(userId, "");
    }
}

public class GenResponseEvent extends ListenerAdapter {
    static String apiKey = dotenv.get("JWT");
    static String zabaFreeZone = dotenv.get("ZABAFREEZONE");
    static String aiMODE = dotenv.get("AIMODE");
    private final List<AIchat.ChatMessage> zabaChatHistory = new ArrayList<>();
    private final List<AIchat.ChatMessage> claireChatHistory = new ArrayList<>();
    private final int MAX_HISTORY = 30;

    // Helper to split long messages
    public void sendLargeMessage(MessageReceivedEvent event, String message) {
        int length = message.length();
        int chunkSize = 2000;

        for (int i = 0; i < length; i += chunkSize) {
            // Calculate end index, ensuring we don't go out of bounds
            int end = Math.min(length, i + chunkSize);
            String chunk = message.substring(i, end);

            // Send the chunk
            event.getChannel().sendMessage(chunk).queue();
        }
    }
    public void sendLargeMessageClaire(MessageReceivedEvent event, String message) {
        int length = message.length();
        int chunkSize = 2000;

        for (int i = 0; i < length; i += chunkSize) {
            // Calculate end index, ensuring we don't go out of bounds
            int end = Math.min(length, i + chunkSize);
            String chunk = message.substring(i, end);

            // Send the chunk
            String claireChunk = "> " + chunk.replace("\n", "\n> ");
            event.getChannel().sendMessage(claireChunk).queue();
        }
    }
    public static boolean isZabaOnline(MessageReceivedEvent event) {
        List<String> limitedChannels = new ArrayList<>();
        limitedChannels.add("1470564008317943848"); // Jaba free zone
        limitedChannels.add("816143984421896262"); // chill grill bot commands

        if(aiMODE.contains("OFF")){
            return false;
        }
        if(aiMODE.equals("DEBUG")){
            if(!event.getGuild().getId().contains("944254135476305980"))
                return false;
        }
        else if(aiMODE.equals("LIMITED")){
            if(!limitedChannels.contains(event.getChannel().getId()))
                return false;
        }
        String targetUrl = "http://192.168.1.223:3000/api/models";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + apiKey) // If you use an API Key
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 2. Parse the JSON List
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray models = jsonResponse.getAsJsonArray("data");

                // 3. Search for "gatekeeper"
                for (JsonElement model : models) {
                    String id = model.getAsJsonObject().get("id").getAsString();

                    // Check if the ID matches "gatekeeper" (case-insensitive)
                    if (id.toLowerCase().contains("gatekeeper")) {
                        return true; // Found it!
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to connect to Open WebUI: " + e.getMessage());
            return false;
        }

        return false; // Connected, but 'gatekeeper' was not in the list
    }
    static class ChatMessage {
        String role;
        String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    private void callHelp(MessageReceivedEvent event){
        String msg = event.getMessage().getContentRaw().toLowerCase();
        String[] help = msg.split("\\s");

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
            //command.add("!help poll\n");
            //use.add("Explains how the poll function works\n");
            command.add("!help socialcredit\n");
            use.add("Explains how social credit works\n");
            command.add("!help swearjar\n");
            use.add("Explains how the swear jar works\n");
            //command.add("!help contra\n");
            //use.add("Explains how the contribution tracker works\n");

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
        /*
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
        */
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
    private void callClaireAI(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        String authorName = event.getAuthor().getName();
        String userId = event.getAuthor().getId();

        String formattedContent;

        /*
        if (userId.equals("239473846447636481")) {
            relationshipContext = "THIS IS GENTRY (YOUR BOYFRIEND). HE IS BEING DISTANT LOL.";
        } else if (userId.equals("232295708974120960")) {
            relationshipContext = "THIS IS COREY (YOUR OTHER BOYFRIEND). HE IS PROBABLY BUSY WITH HIS MUSIC INSTEAD OF YOU.";
        } else {
            relationshipContext = "THIS IS A STRANGER. TALK ABOUT HOW MUCH YOU MISS GENTRY AND COREY.";
        }*/

        String speakerName = "";
        if (userId.equals("239473846447636481")) {
            speakerName = "Gentry";
        } else if (userId.equals("232295708974120960")) {
            speakerName = "Corey";
        } else {
            speakerName = "Stranger";
        }
        formattedContent = "Input: " + speakerName + " says \"" + content + "\"";

        // Update history (Synchronized to prevent race conditions)
        synchronized (claireChatHistory) {
            claireChatHistory.add(new AIchat.ChatMessage("user", formattedContent));
            while (claireChatHistory.size() > 3) {
                claireChatHistory.remove(0);
            }
        }

        // Build the JSON Payload
        Gson gson = new Gson();
        JsonArray messagesArray = new JsonArray();

        // 1. SYSTEM PROMPT
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "");
        messagesArray.add(systemMsg);



        // 2. CHAT HISTORY
        /*
        synchronized (claireChatHistory) {
            for (AIchat.ChatMessage msg : claireChatHistory) {
                JsonObject m = new JsonObject();
                m.addProperty("role", msg.role);
                m.addProperty("content", msg.content);
                messagesArray.add(m);
            }
        }*/

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", formattedContent);
        messagesArray.add(userMsg);

        // 3. CONSTRUCT BODY
        JsonObject finalBody = new JsonObject();
        finalBody.addProperty("model", "gf-bot");
        finalBody.add("messages", messagesArray);
        finalBody.addProperty("stream", false);

        // 4. STOP TOKENS (Root level in OpenAI API)
        JsonArray stopTokens = new JsonArray();
        stopTokens.add("<|start_header_id|>user<|end_header_id|>");
        stopTokens.add("<|eot_id|>");
        //stopTokens.add("\n\nUser:");
        //finalBody.add("stop", stopTokens);

        String jsonBody = gson.toJson(finalBody);

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // 5. REQUEST TO OPENWEBUI
        // NOTE: Port is usually 3000 for OpenWebUI. Check your Docker/Host config.
        // Endpoint is usually /api/chat/completions (OpenAI Compatible)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://192.168.1.223:3000/api/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey) // <--- ADD KEY HERE
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(45))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    try {
                        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

                        // 6. OPENAI FORMAT PARSING
                        // Returns { "choices": [ { "message": { "content": "..." } } ] }
                        String aiResponse = jsonObject.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .get("message").getAsJsonObject()
                                .get("content").getAsString();


                        // Update History
                        synchronized (claireChatHistory) {
                            claireChatHistory.add(new AIchat.ChatMessage("assistant", aiResponse));
                            if (claireChatHistory.size() > MAX_HISTORY) claireChatHistory.remove(0);
                        }
                        System.err.println("Message received from Claire AI.");



                        // Send to Discord
                        if (aiResponse.length() > 2000) {
                            sendLargeMessageClaire(event, aiResponse);
                        } else {
                            String claireResponse = "> " + aiResponse.replace("\n", "\n> ");
                            event.getChannel().sendMessage(claireResponse).queue();
                        }
                    } catch (Exception e) {
                        System.err.println("Claire AI Error: " + e.getMessage());
                        // Print body to see error messages from OpenWebUI (e.g. invalid key)
                        System.err.println("Response Body: " + responseBody);
                        e.printStackTrace();
                    }
                });
    }
    private String callGatekeeperAI(String prompt) {
        try {
            // 1. Build Payload
            JsonObject payload = new JsonObject();
            payload.addProperty("model", "gatekeeper"); // Ensure a model named "gatekeeper" exists in OpenWebUI
            payload.addProperty("stream", false);
            payload.addProperty("temperature", 0.0);    // CRITICAL: Makes output deterministic/strict
            payload.addProperty("repetition_penalty", 1.0);
            payload.addProperty("frequency_penalty", 0.0);
            payload.addProperty("presence_penalty", 0.0);
            payload.addProperty("max_tokens", 15);      // OPTIMIZATION: Prevents long, hallucinated responses

            JsonArray messages = new JsonArray();

            // 2. System Prompt (Using Java Text Blocks for readability)
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", "");
            messages.add(systemMsg);

            // 3. User Message
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", prompt);
            messages.add(userMsg);

            payload.add("messages", messages);

            // 4. Send Request
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(20)) // Add connection timeout
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://192.168.1.223:3000/api/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(payload)))
                    .timeout(Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Parse Response
            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();

            // Safety check to ensure the structure exists before accessing
            if (responseJson.has("choices") && responseJson.getAsJsonArray("choices").size() > 0) {
                String content = responseJson.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .get("message").getAsJsonObject()
                        .get("content").getAsString();

                // Clean up potential whitespace or stray punctuation (e.g. "INSULT." -> "INSULT")
                return content.replaceAll("[^a-zA-Z_]", "").trim().toUpperCase();
            }

            return "SLEEP";

        } catch (Exception e) {
            System.err.println("Gatekeeper Error: " + e.getMessage());
            return "SLEEP";
        }
    }
    private void callZabaAI(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        String authorName = event.getAuthor().getName();
        String userId = event.getAuthor().getId();
        String relationNote = RelationshipManager.getContextForUser(userId);

        // Prepare the Flavor Text (The Director's Note)
        //String flavorText = "";
        //if (flavor != null && !flavor.isEmpty()) {
            // We wrap it in a specific system tag so Zaba knows it's a hint, not the user speaking.
        //    flavorText = String.format(" [SYSTEM CONTEXT: %s]", flavor);
        //}

        // Format the message with identity context
        String formattedContent;
        if (!relationNote.isEmpty()) {
            // Example: (Snowbowls) [The Creator] says: You suck! [SYSTEM CONTEXT: User is insulting you]
            formattedContent = String.format("[%s] \nUser: %s \nMessage: %s", relationNote, authorName, content);
        } else {
            // Example: (RandomGuy) says: You suck! [SYSTEM CONTEXT: User is insulting you]
            formattedContent = String.format("User: %s \nMessage: %s", authorName, content);
        }

        // Update history (Synchronized to prevent race conditions)
        synchronized (zabaChatHistory) {
            zabaChatHistory.add(new AIchat.ChatMessage("user", formattedContent));
            while (zabaChatHistory.size() > MAX_HISTORY) {
                zabaChatHistory.remove(0);
            }
        }

        // Build the JSON Payload
        Gson gson = new Gson();
        JsonArray messagesArray = new JsonArray();

        // 1. SYSTEM PROMPT (Your updated Zaba prompt)
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "");
        messagesArray.add(systemMsg);

        // 2. CHAT HISTORY
        synchronized (zabaChatHistory) {
            for (AIchat.ChatMessage msg : zabaChatHistory) {
                JsonObject m = new JsonObject();
                m.addProperty("role", msg.role);
                m.addProperty("content", msg.content);
                messagesArray.add(m);
            }
        }

        // 3. CONSTRUCT BODY
        JsonObject finalBody = new JsonObject();
        finalBody.addProperty("model", "zaba");
        finalBody.add("messages", messagesArray);
        finalBody.addProperty("stream", false);

        //finalBody.addProperty("temperature", 0.8);
        //finalBody.addProperty("max_tokens", 4096);

        //finalBody.addProperty("presence_penalty", 0.0);
        //finalBody.addProperty("repeat_penalty", 1.15);

        //finalBody.addProperty("min_p", 0.1);
        //finalBody.addProperty("top_p", 0.95); // Slightly restrictive to keep him focused

        // 4. STOP TOKENS (Root level in OpenAI API)
        JsonArray stopTokens = new JsonArray();
        stopTokens.add("<|start_header_id|>user<|end_header_id|>");
        stopTokens.add("<|eot_id|>");
        //stopTokens.add("\n\nUser:");
        finalBody.add("stop", stopTokens);

        String jsonBody = gson.toJson(finalBody);

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // 5. REQUEST TO OPENWEBUI
        // NOTE: Port is usually 3000 for OpenWebUI. Check your Docker/Host config.
        // Endpoint is usually /api/chat/completions (OpenAI Compatible)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://192.168.1.223:3000/api/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey) // <--- ADD KEY HERE
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    try {
                        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

                        // 6. OPENAI FORMAT PARSING
                        // Returns { "choices": [ { "message": { "content": "..." } } ] }
                        String aiResponse = jsonObject.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .get("message").getAsJsonObject()
                                .get("content").getAsString();

                        // Update History
                        synchronized (zabaChatHistory) {
                            zabaChatHistory.add(new AIchat.ChatMessage("assistant", aiResponse));
                            if (zabaChatHistory.size() > MAX_HISTORY) zabaChatHistory.remove(0);
                        }
                        System.err.println("Message received from Zaba AI.");
                        // Send to Discord
                        aiResponse = SanitizeAIOutput.sanitizeOutput(event, aiResponse);
                        if (aiResponse.length() > 2000) {
                            sendLargeMessage(event, aiResponse);
                        } else {
                            event.getChannel().sendMessage(aiResponse).queue();
                        }
                    } catch (Exception e) {
                        System.err.println("Zaba AI Error: " + e.getMessage());
                        // Print body to see error messages from OpenWebUI (e.g. invalid key)
                        System.err.println("Response Body: " + responseBody);
                        e.printStackTrace();
                    }
                });
    }
    // GenResponseEvents are actions that can occur in casual conversation.
    // You can think of this class as where most of the 'personality' of the bot is handled.
    // Naming convention can be done better but the gist is this class is used for handling the JSON file
    public void onMessageReceived(MessageReceivedEvent event) {



        if (event.getAuthor().isBot()) return;

        String msg = event.getMessage().getContentRaw().toLowerCase();

        // This regex looks for messages that are ONLY http/https links
        if (msg.matches("^(https?://\\S+|www\\.\\S+)\\s*$")) {
            System.out.println("Ignoring bare URL.");
            return;
        }

        String[] help = msg.split("\\s");
        if(help[0].equalsIgnoreCase(("!help"))){
            callHelp(event);
            return;
        }

        if(msg.equals("!clearai")){
            synchronized (claireChatHistory) {
                claireChatHistory.clear();
            }
            synchronized (zabaChatHistory) {
                zabaChatHistory.clear();
            }

            System.out.println("AI Chat History Cleared!");
            event.getChannel().deleteMessageById(event.getMessageId()).queueAfter(700, TimeUnit.MILLISECONDS);
            return;
        }

        List<String> unapprovedChannels = new ArrayList<>();
        unapprovedChannels.add(zabaFreeZone); // Zaba free zone


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

        if(unapprovedChannels.contains(event.getChannel().getId()))
            return;

        // GenEm or General Emote will react with an emote whenever a trigger word / phrase is detected in a message

        assert genEm != null;
        assert emoteList != null;
        Set<String> scanGenEm = genEm.keySet();
        for (String str : scanGenEm) {
            if(msg.contains(str)){
                String emote = emoteList.get(genEm.get(str)).toString();
                if(emote.equals("rdj:860593603033432064")){
                    if(isZabaOnline(event)) {
                        break;
                    }
                }
                event.getMessage().addReaction(Emoji.fromUnicode(emote)).queue();
            }
        }

        if(isZabaOnline(event)){

            Message message = event.getMessage();

            // Checking if this is a direct reply to Zaba
            if (message.getReferencedMessage() != null) {
                long botId = event.getJDA().getSelfUser().getIdLong();
                long repliedToUserId = message.getReferencedMessage().getAuthor().getIdLong();

                if (repliedToUserId == botId) {
                    callZabaAI(event);
                    return;
                }
            }
            String judgment = callGatekeeperAI(msg); // This method calls the prompt above
            System.out.println("Gatekeeper: " + judgment + " from user: " +event.getMessage().getAuthor().getName());
            switch (judgment) {
                case "ZABA":
                    callZabaAI(event);
                    break;
                case "CLAIRE":
                    callClaireAI(event);
                    break;
                case "SLEEP":
                    break;
                default:
                    System.out.println("UNEXPECTED");
                    break;
            }
            return;
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
                if(Math.random() > Float.parseFloat(split[1])/100)
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
