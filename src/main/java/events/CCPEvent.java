package events;

import java.io.FileReader;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CCPEvent extends ListenerAdapter {
  // Might combine CCPEvent with the Credit Score classes
  @Override
  public void onMessageReceived(MessageReceivedEvent event) {

    if (!event.getMessage().getAuthor().isBot()) {
      String msg = event.getMessage().getContentRaw();

      JSONParser parser = new JSONParser();
      JSONObject mto = null;
      JSONObject jsonObject = null;

      try {
        Object obj = parser.parse(new FileReader("keywords.json"));
        jsonObject = (JSONObject) obj;
        mto = (JSONObject) jsonObject.get("manyToOne");

      } catch (Exception e) {
        e.printStackTrace();
      }
      assert mto != null;
      for (int i = 1; i <= mto.size(); i++) {
        JSONObject keyScan = (JSONObject) mto.get(String.valueOf(i));
        String str = keyScan.keySet().toString();
        String key = str.substring(1, str.length() - 1);
        if (msg.contains(key)) {
          String keyValue = keyScan.get(key).toString();
          String resKey = (String) jsonObject.get(keyValue);
          event.getChannel().sendMessage(resKey).queue();
        }
      }
    }
  }
}
