package events;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class HelloEvent extends ListenerAdapter {
    public void onMessageReceived(MessageReceivedEvent event)
    {

       // System.out.printf("[%s]: %s\n", event.getAuthor().getName(), event.getMessage().getContentDisplay());
       // System.out.printf("Server: [%s]\n", event.getGuild().getId());
        if(event.getGuild().getId().equals(("944254135476305980"))){
            System.out.print("SUC\n");
        }
        //event.getMessage().addReaction("boatyvv:854825460494761994").queue();
    }
}