package events;

import static events.UserStatEvent.dotenv;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.*;
import dev.lavalink.youtube.http.YoutubeOauth2Handler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerControl extends ListenerAdapter {
  // ########################################### //
  private static String getTimestamp(long milliseconds) {
    int seconds = (int) (milliseconds / 1000) % 60;
    int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
    int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

    if (hours > 0) return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    else return String.format("%02d:%02d", minutes, seconds);
  }

  private final AudioPlayerManager playerManager;
  private boolean isLooping = false; // Add a flag to track looping status
  private AudioTrack currentTrack = null; // Keep track of the current track

  private final Map<Long, GuildMusicManager> musicManagers;
  private static final Logger log = LoggerFactory.getLogger(YoutubeOauth2Handler.class);

  public PlayerControl() {
    this.musicManagers = new HashMap<>();
    this.playerManager = new DefaultAudioPlayerManager();

    YoutubeSourceOptions options =
        new YoutubeSourceOptions().setRemoteCipher("https://cipher.kikkia.dev", null, "zaba-bot");

    YoutubeAudioSourceManager source =
        new YoutubeAudioSourceManager(
            options,
            new MusicWithThumbnail(),
            new Tv(),
            new TvHtml5SimplyWithThumbnail(),
            new AndroidVrWithThumbnail(),
            new WebEmbeddedWithThumbnail());

    String oauth_key = dotenv.get("OAUTH");
    source.useOauth2(oauth_key, true);

    playerManager.registerSourceManager(source);

    AudioSourceManagers.registerRemoteSources(
        playerManager,
        com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
    AudioSourceManagers.registerLocalSource(playerManager);
  }

  private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
    long guildId = Long.parseLong(guild.getId());
    GuildMusicManager musicManager = musicManagers.get(guildId);

    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager);
      musicManagers.put(guildId, musicManager);
    }

    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

    return musicManager;
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {

    String[] command = event.getMessage().getContentRaw().split(" ", 2);
    TextChannel connectedChannel = event.getChannel().asTextChannel();

    if ("!join".equals(command[0])) {
      join(connectedChannel.getGuild(), event);
    } else if ("!leave".equals(command[0])) {
      leave(connectedChannel.getGuild(), event);
    } else if ("!play".equals(command[0]) && command.length == 2) {
      loadAndPlay(connectedChannel, command[1], event);
    } else if ("!stop".equals(command[0])) {
      stopTrack(connectedChannel, event);
    } else if ("!skip".equals(command[0])) {
      skipTrack(connectedChannel, event);
    } else if ("!loop".equals(command[0])) {
      repeatTrack(connectedChannel, event);
    } else if ("!clear".equals(command[0]) || "!reset".equals(command[0])) {
      resetQueue(connectedChannel, event);
    }
    if ("!queue".equals(command[0]) || "!list".equals(command[0])) {
      printQueue(connectedChannel, event);
    }

    super.onMessageReceived(event);
  }

  private void printQueue(final TextChannel channel, MessageReceivedEvent event) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    TrackScheduler scheduler = musicManager.scheduler;

    BlockingQueue<AudioTrack> queue = scheduler.queue;
    synchronized (queue) {
      if (queue.isEmpty()) {
        event.getChannel().sendMessage("The queue is currently empty!").queue();
      } else {
        int trackCount = 0;
        long queueLength = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("Current Queue: ").append(queue.size()).append("\n");
        for (AudioTrack track : queue) {
          queueLength += track.getDuration();
          if (trackCount < 10) {
            sb.append("`[").append(getTimestamp(track.getDuration())).append("]` ");
            sb.append(track.getInfo().title).append("\n");
            trackCount++;
          }
        }
        sb.append("\n").append("Total Queue Length: ").append(getTimestamp(queueLength));

        event.getChannel().sendMessage(sb.toString()).queue();
      }
    }
  }

  private void loadAndPlay(
      final TextChannel channel, final String trackUrl, MessageReceivedEvent event) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

    playerManager.loadItemOrdered(
        musicManager,
        trackUrl,
        new AudioLoadResultHandler() {
          @Override
          public void trackLoaded(AudioTrack track) {

            play(channel.getGuild(), musicManager, track, event);
          }

          @Override
          public void playlistLoaded(AudioPlaylist playlist) {
            AudioTrack firstTrack = playlist.getSelectedTrack();

            if (firstTrack == null) {
              firstTrack = playlist.getTracks().get(0);
            }

            event.getMessage().addReaction(Emoji.fromUnicode("polcow:1228764066047066252")).queue();

            play(channel.getGuild(), musicManager, firstTrack, event);
          }

          @Override
          public void noMatches() {
            System.out.println("Borked play command");
            channel.sendMessage("Nah").queue();
          }

          @Override
          public void loadFailed(FriendlyException exception) {
            channel.sendMessage("Could not play: " + exception.getMessage()).queue();
          }
        });
  }

  private void play(
      Guild guild, GuildMusicManager musicManager, AudioTrack track, MessageReceivedEvent event) {
    connectToSendersVoiceChannel(event);

    musicManager.scheduler.queue(track);
    event.getMessage().addReaction(Emoji.fromUnicode("polcow:1228764066047066252")).queue();
  }

  private void leave(Guild guild, MessageReceivedEvent event) {
    guild.getAudioManager().closeAudioConnection();

    event.getMessage().addReaction(Emoji.fromUnicode("squid:979113110029889546")).queue();
  }

  private void join(Guild guild, MessageReceivedEvent event) {
    try {
      getGuildAudioPlayer(guild);
      connectToSendersVoiceChannel(event);
      event.getMessage().addReaction(Emoji.fromUnicode("polcow:1228764066047066252")).queue();
    } catch (Exception e) {
      event.getMessage().addReaction(Emoji.fromUnicode("squid:979113110029889546")).queue();
    }
  }

  private void repeatTrack(final TextChannel channel, MessageReceivedEvent event) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    TrackScheduler scheduler = musicManager.scheduler;
    boolean foo = !scheduler.isRepeating();
    String foo_2 = Boolean.toString(foo);

    scheduler.setRepeating(!scheduler.isRepeating());

    event.getMessage().addReaction(Emoji.fromUnicode("➰")).queue();
  }

  private void resetQueue(TextChannel channel, MessageReceivedEvent event) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    TrackScheduler scheduler = musicManager.scheduler;
    Long guildId = channel.getGuild().getIdLong();
    musicManager.player.destroy();
    channel.getGuild().getAudioManager().setSendingHandler(null);
    musicManagers.remove(guildId);

    GuildMusicManager mng = musicManagers.get(guildId);
    mng = musicManagers.get(guildId);
    if (mng == null) {
      mng = new GuildMusicManager(playerManager);
      musicManagers.put(guildId, mng);
    }

    channel.getGuild().getAudioManager().setSendingHandler(mng.getSendHandler());
    event.getMessage().addReaction(Emoji.fromUnicode("rdj:860593603033432064")).queue();
  }

  private void skipTrack(TextChannel channel, MessageReceivedEvent event) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    musicManager.scheduler.nextTrack();

    event
        .getMessage()
        .addReaction(Emoji.fromUnicode("skipper_deadinside:870900445168681010"))
        .queue();
  }

  public void setLooping(boolean looping) {
    isLooping = looping;
  }

  public boolean isLooping() {
    return isLooping;
  }

  private void stopTrack(TextChannel channel, MessageReceivedEvent event) {
    GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
    musicManager.player.stopTrack();

    event.getMessage().addReaction(Emoji.fromUnicode("squid:979113110029889546")).queue();
  }

  private static void connectToFirstVoiceChannel(AudioManager audioManager) {
    if (!audioManager.isConnected()) {
      for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
        audioManager.openAudioConnection(voiceChannel);
        break;
      }
    }
  }

  private static void connectToSendersVoiceChannel(MessageReceivedEvent event) {
    Member member = event.getMember();
    if (member == null) {
      return;
    }
    GuildVoiceState voiceState = member.getVoiceState();

    if (voiceState == null) {
      return;
    }

    AudioChannel voiceChannel = voiceState.getChannel().asVoiceChannel();

    AudioManager audioManager = event.getGuild().getAudioManager();
    audioManager.openAudioConnection(voiceChannel);
  }
}
