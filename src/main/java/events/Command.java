package events;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/** The first whitespace-delimited token of a message and everything after it. */
public record Command(String name, String rest) {
  public static Command parse(MessageReceivedEvent event) {
    String[] parts = event.getMessage().getContentRaw().split(" ", 2);
    return new Command(parts[0], parts.length > 1 ? parts[1] : "");
  }

  /** Case-insensitive match against the command name, e.g. {@code cmd.is("!show")}. */
  public boolean is(String prefix) {
    return name.equalsIgnoreCase(prefix);
  }
}
