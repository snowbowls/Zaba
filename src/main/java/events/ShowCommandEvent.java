package events;

import com.mongodb.*;
import com.mongodb.client.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.awt.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ShowCommandEvent extends ListenerAdapter {

  public static Dotenv dotenv = Dotenv.load();
  String uri = dotenv.get("URI");
}
