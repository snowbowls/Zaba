import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import events.*;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.audio.dave.DaveSessionFactory;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Bot {

  public static Dotenv dotenv = Dotenv.load();

  public static void main(String[] args) throws Exception {

    String TOKEN = dotenv.get("TOKEN");

    DaveSessionFactory daveFactory = new JDaveSessionFactory();

    JDABuilder.createDefault(TOKEN)
        .setAudioModuleConfig(new AudioModuleConfig().withDaveSessionFactory(daveFactory))

        // JDA Builder Properties
        .enableIntents(GatewayIntent.GUILD_MEMBERS)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setChunkingFilter(ChunkingFilter.ALL)

        // Social Credit Events
        .addEventListeners(new SocialCreditEvent())

        // Audio Player Events
        .addEventListeners(new PlayerControl())

        // Other Events
        .addEventListeners(new GenResponseEvent())
        // .addEventListeners(new PollEvent())
        .addEventListeners(new JonEvent())
        // .addEventListeners(new AntiJonPostingEvent())
        .addEventListeners(new PeriodicEvent())
        .addEventListeners(new UserStatEvent())
        // .addEventListeners(new ContribroEvent())
        .addEventListeners(new ButtonTest())
        // .addEventListeners(new AIchat())

        .build();
  }
}
