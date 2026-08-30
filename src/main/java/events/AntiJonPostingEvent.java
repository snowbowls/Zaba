package events;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Objects;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AntiJonPostingEvent extends ListenerAdapter {

  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getAuthor().getId().equals("222163619125788682")) {
      if (event.getChannel().getId().equals("874544313894318080")) return;

      String fullmsg = event.getMessage().getContentRaw().toLowerCase();

      if (containsJon(fullmsg)) {
        String msg = event.getMessage().getContentRaw();
        String mod = "From Jon: " + msg;
        System.out.println("JON TRIGGER");
        if (msg.contains("@everyone")) {
          String result = mod.replaceAll("@everyone", "");
          Objects.requireNonNull(event.getJDA().getTextChannelById("874544313894318080"))
              .sendMessage(result)
              .complete();
        }
        if (msg.contains("@")) {
          String result = mod.replaceAll("@", "");
          Objects.requireNonNull(event.getJDA().getTextChannelById("874544313894318080"))
              .sendMessage(result)
              .complete();
        } else
          Objects.requireNonNull(event.getJDA().getTextChannelById("874544313894318080"))
              .sendMessage(mod)
              .complete();
        event.getMessage().delete().complete();
      }
    }
  }

  public static boolean containsJon(String input) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("jon.txt"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    String[] jons = reader.lines().toArray(String[]::new);
    String sanitizedInput = input.replaceAll("[^a-zA-Z*!@#$%&]+", " ");
    for (String key : jons) {
      if (containsJonWord(sanitizedInput, key)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsJonWord(String input, String key) {
    String[] censors = {"*", "!", "@", "#", "$", "%", "&"};
    for (String censorChar : censors) {
      if (input.contains(censorChar)) {
        String[] inputWords = input.split("\\s+");
        for (String inputWord : inputWords) {
          String inptWord = inputWord.split("\\s", 2)[0];
          if (inptWord.contains(censorChar) && key.length() == inptWord.length()) {
            String modifiedKeyWord = replaceChar(key, inptWord, censorChar);
            if (modifiedKeyWord.equals(inptWord)) {
              return true;
            }
          }
        }
      }
    }

    String[] inputWords = input.split("\\W+");
    String combinedString = String.join("", inputWords);
    if (combinedString.replaceAll("\\W+", "").contains(key)) return true;

    return input.contains(key);
  }

  private static String replaceChar(String keyWord, String inputWord, String censorChar) {
    StringBuilder modifiedWord = new StringBuilder();

    if (keyWord.length() != inputWord.length()) return modifiedWord.toString();

    for (int i = 0; i < keyWord.length(); i++) {
      if (String.valueOf(inputWord.charAt(i)).equals(censorChar)) {
        modifiedWord.append(censorChar);
      } else {
        modifiedWord.append(keyWord.charAt(i));
      }
    }
    return modifiedWord.toString();
  }
}
