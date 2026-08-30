package events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.util.Arrays;

public class PollEvent extends ListenerAdapter {
    private final  String[] reactions = {"\u0031\u20E3", "\u0032\u20E3", "\u0033\u20E3", "\u0034\u20E3", "\u0035\u20E3",
            "\u0036\u20E3", "\u0037\u20E3", "\u0038\u20E3", "\u0039\u20E3", "\uD83D\uDD1F"};
            //":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:", ":ten:"

    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot())
            return;
        String msg = null;

        try{msg = event.getMessage().getContentRaw();}
        catch(ErrorResponseException r){
            System.out.println(r);
        }
        String[] args = msg.split(",");

        String cmd = null;

        if(msg.length() < 5)
            return;

        try{cmd = args[0].substring(0,5);}
        catch (StringIndexOutOfBoundsException r){
            System.out.println("Not a poll command, but close");
            return;
        }

        if(event.getMessage().getContentRaw().equalsIgnoreCase("!poll headcount")){
            args = new String[4];
            args[0] = "Headcount";
            args[1] = "I'm definitely going!";
            args[2] = "I will not be able to attend :(";
            args[3] = "I'm unsure yet..";
        }
        else args[0] = args[0].substring(5);

        // Check if the message starts with the poll command, in this case "!poll"
        if (cmd.equalsIgnoreCase("!poll")) {
            System.out.println("Poll created by " + event.getAuthor().getName());

            // Check if the user provided a poll question
            if (args.length < 2) {
                event.getChannel().sendMessage("Invalid format, check '!help poll'").queue();
                return;
            }

            // Create a new EmbedBuilder and set the title and color
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Poll: " + args[0]);
            builder.setColor(Color.magenta);

            // Add the poll options as fields in the EmbedBuilder
            for (int i = 1; i < args.length; i++) {
                String reaction = getReaction(i - 1);
                builder.addField(reaction + " " + args[i], "", false);
            }

            // Send the poll as a message in the channel
            MessageCreateData data = new MessageCreateBuilder()
                    .addEmbeds(builder.build())
                    .build();

            String[] finalArgs = args;

            try{
                event.getChannel().sendMessage(data)
                        .submit()
                        .thenAccept(message -> {
                            for (int i = 1; i < finalArgs.length; i++) {
                                message.addReaction(Emoji.fromUnicode(Arrays.asList(reactions).get(i-1))).queue();
                            }
                        });
            } catch (ClassCastException r) {
                // I think this catch should be deleted
                System.out.println("In thread, preforming catch");
                String threadId = String.valueOf(event.getChannelType().getId());
                System.out.println(threadId);
                ThreadChannel thread = event.getGuild().getThreadChannelById("1017548168038187008");
                thread.sendMessage(data)
                        .submit()
                        .thenAccept(message -> {
                            for (int i = 1; i < finalArgs.length; i++) {
                                message.addReaction(Emoji.fromUnicode(Arrays.asList(reactions).get(i - 1))).queue();
                            }
                        });

            }
            event.getChannel().deleteMessageById(event.getMessageId()).queue();

        }
    }
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        // Get the message that the reaction was added to
        Message msg = null;
        try{msg = event.getChannel().retrieveMessageById(event.getMessageId()).complete();}
        catch(ErrorResponseException r){
            System.out.println(r);
        }
        Message message = msg;//event.getChannel().retrieveMessageById(event.getMessageId()).complete();

        // Check if the message is a poll and the reaction is valid
        try {
            if (!message.getEmbeds().isEmpty() && message.getAuthor().isBot() && Arrays.asList(reactions).contains(event.getEmoji().getAsReactionCode())
                    && message.getEmbeds().get(0).getTitle().substring(0, 4).equalsIgnoreCase("poll")) {

                // Get the poll question and options from the message
                MessageEmbed originalEmbed = message.getEmbeds().get(0);
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle(originalEmbed.getTitle())
                        .setDescription(originalEmbed.getDescription())
                        .setColor(originalEmbed.getColor());

                int index = 0;
                for (MessageEmbed.Field field : originalEmbed.getFields()) {
                    int finalIndex = index;

                    int count = message.getReactions().stream()
                            .filter(reaction -> reaction.getEmoji().getName().equals(Arrays.asList(reactions).get(finalIndex)))
                            .mapToInt(reaction -> reaction.getCount() - 1)
                            .sum();

                    String countString = count + " vote" + (count != 1 ? "s" : "");
                    builder.addField(field.getName(), countString, false);
                    index = index + 1;
                }

                // Update the poll message in the channel
                MessageCreateData data = new MessageCreateBuilder()
                        .addEmbeds(builder.build())
                        .build();
                message.editMessage(MessageEditData.fromCreateData(data)).complete();
            }
        }catch (NullPointerException e){
            System.out.println("OMG JON SHUT UP");
        }

    }
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {

        Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();

        if (!message.getEmbeds().isEmpty() && message.getAuthor().isBot() && Arrays.asList(reactions).contains(event.getEmoji().getAsReactionCode())
                && message.getEmbeds().get(0).getTitle().substring(0,4).equalsIgnoreCase("poll")) {

            // Get the poll question and options from the message
            MessageEmbed originalEmbed = message.getEmbeds().get(0);
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle(originalEmbed.getTitle())
                    .setDescription(originalEmbed.getDescription())
                    .setColor(originalEmbed.getColor());
            int index = 0;
            for (MessageEmbed.Field field : originalEmbed.getFields()) {
                int finalIndex = index;

                int count = message.getReactions().stream()
                        .filter(reaction -> reaction.getEmoji().getName().equals(Arrays.asList(reactions).get(finalIndex)))
                        .mapToInt(reaction -> reaction.getCount() - 1)
                        .sum();

                String countString = count + " vote" + (count != 1 ? "s" : "");
                builder.addField(field.getName(), countString, false);
                index = index + 1;
            }

            // Update the poll message in the channel
            MessageCreateData data = new MessageCreateBuilder()
                    .addEmbeds(builder.build())
                    .build();
            message.editMessage(MessageEditData.fromCreateData(data)).complete();
        }

    }
    private String getReaction(int index) {
        String[] reactions = {"\u0031\u20E3", "\u0032\u20E3", "\u0033\u20E3", "\u0034\u20E3", "\u0035\u20E3",
                "\u0036\u20E3", "\u0037\u20E3", "\u0038\u20E3", "\u0039\u20E3", "\uD83D\uDD1F"};
        return reactions[index];
    }
}
