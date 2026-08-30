package events;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class TagUserEvent extends ListenerAdapter {
  // Gonna use this to annoy people eventually
  public void onMessageReceived(MessageReceivedEvent event) {
    String msg = event.getMessage().getContentRaw().toLowerCase();

    String jonid = "222163619125788682";
    String tagjon = "<@222163619125788682>";

    String snowid = "232720241099014144";

    if (event.getMessage().getAuthor().getId().equals(snowid)) {
      event.getChannel().sendMessage("insert tag here").complete();
      event
          .getChannel()
          .getHistory()
          .retrievePast(1)
          .queue(
              messages -> {
                System.out.println(messages.get(0).getContentRaw());
                messages.get(0).delete().queue();
              });
      // event.getChannel().getHistory().retrievePast(1).complete().get(1).delete().complete();
    }
  }
}
