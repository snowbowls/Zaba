package events;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ShowCommandEvent extends ListenerAdapter {

  public static Dotenv dotenv = Dotenv.load();
  String uri = dotenv.get("URI");
}
