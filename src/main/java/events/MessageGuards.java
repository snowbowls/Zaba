package events;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** Shared entry guards for message listeners, so each one doesn't re-implement its own. */
public final class MessageGuards {
  private MessageGuards() {}

  /**
   * True if no listener should act on this message: it's from a bot, or not a guild text channel.
   */
  public static boolean shouldIgnore(MessageReceivedEvent event) {
    return event.getAuthor().isBot() || !event.isFromType(ChannelType.TEXT);
  }
}
