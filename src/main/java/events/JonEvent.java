package events;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.concurrent.TimeUnit;

public class JonEvent extends ListenerAdapter {
    // JonEvents are various actions with the intent of annoying someone. Nothing about this class is
    // organized well, it usually serves as a half-measure attempt at handling silly responses
    public void onMessageReceived(MessageReceivedEvent event) {

        String msg = event.getMessage().getContentRaw().toLowerCase();

        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        JSONArray triggers;

        // Load JSON
        try {
            Object obj = parser.parse(new FileReader("keywords.json"));
            jsonObject = (JSONObject) obj;

        } catch (Exception e) {
            e.printStackTrace();
        }

        assert jsonObject != null;

        // Dragonball triggers
        triggers = (JSONArray) jsonObject.get("dragonball");
        for (Object key : triggers) {
            if (msg.contains(key.toString())) {
               // event.getChannel().sendMessage("**SHUTUPSHUTUPSHUTUPSHUTUPSHUTUP**").queue();

                if(event.getAuthor().getId().equals("222163619125788682")) {
                    event.getMessage().addReaction(Emoji.fromUnicode("15_neg:934919187787288597")).queue();
                }
                System.out.println("dragonball stuff" + " #" + event.getChannel().getName() + " @" + event.getMessage().getAuthor().getName());
                return;
            }
        }

        // Sus triggers
        triggers = (JSONArray) jsonObject.get("sus");
        for (Object key : triggers) {
            if (msg.contains(key.toString())) {
                event.getMessage().addReaction(Emoji.fromUnicode("amongass:854818205624827935")).queue();
                System.out.println("sus stuff" + " #" + event.getChannel().getName() + " @" + event.getMessage().getAuthor().getName());
                return;
            }
        }

        if (msg.contains("grill")) {
            event.getMessage().addReaction(Emoji.fromUnicode("justagriller:816352491386044426")).queue();
            System.out.println("grill" + " #" + event.getChannel().getName() + " @" + event.getMessage().getAuthor().getName());
        }

        if (msg.equals("n")) {
            event.getMessage().addReaction(Emoji.fromUnicode("disintegrate:829491387824865321")).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("touch_grass:936036425789493269")).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("soyjak:921475501023977573")).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("bravo:250845632141590528")).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("3dHyperThink:676990578793381937")).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("grabs_you:666497981742186506")).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("shiba_not_amused:866064465359011880")).queue();
            if(!event.getAuthor().getId().equals("222163619125788682")) {
                event.getMessage().addReaction(Emoji.fromUnicode("15_neg:934919187787288597")).queue();
            }
            System.out.println("n" + " #" + event.getChannel().getName() + " @" + event.getMessage().getAuthor().getName());
            return;
        }

        if (msg.contains("rrrr mom")){
            JSONObject response = null;
            try {
                Object obj = parser.parse(new FileReader("keywords.json"));
                jsonObject = (JSONObject) obj;
                response = (JSONObject) jsonObject.get("socialCredit");

            } catch (Exception e) {
                e.printStackTrace();
            }
                System.out.println("************************************************************");
                System.out.println(event.getAuthor().getName() + " had their family kidnapped!" );

                String userid = event.getAuthor().getName();
                JSONObject finalResponse = response;
                if (Math.random() < .90)
                    event.getJDA().getUserById(event.getMessage().getAuthor().getId()).openPrivateChannel()
                            .flatMap(channel -> channel.sendMessage(finalResponse.get("1").toString()))
                            .queue();
                else {
                    event.getJDA().getUserById(event.getMessage().getAuthor().getId()).openPrivateChannel()
                            .flatMap(channel -> channel.sendMessage(finalResponse.get("2").toString()))
                            .queue();
                    System.out.println(" U W U " );
                }


                System.out.println("************************************************************");
        }

        if(msg.contains("!zaba")){
            String message = msg.substring(5);
            char lastChar = message.charAt(message.length() - 1);
            if(message.length() > 19 && Character.isDigit(lastChar)){
                lastChar = message.charAt(message.length() - 18);
                if(Character.isDigit(lastChar)) {
                    String content = message.substring(0, message.length() - 19).trim();
                    String replyID = message.substring(message.length() - 19);
                    try {
                        Message replyMsg = event.getChannel().retrieveMessageById(replyID).complete();
                        replyMsg.reply(content).queue();
                    }catch (IllegalArgumentException r){
                        event.getChannel().sendMessage(message).queue();
                    }
                }
            }
            else {
                event.getChannel().sendMessage(message).queue();
            }
            event.getChannel().deleteMessageById(event.getMessageId()).queueAfter(700, TimeUnit.MILLISECONDS);
        }


        // Delete me
        //event.getChannel().sendMessage("https://cdn.discordapp.com/attachments/858416918586851368/874480650332307516/SHUTTHEFUCKUP-1.mp4").queue();
    }
}
