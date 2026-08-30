package events;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

public class old_PlayerControl extends ListenerAdapter
{
    public static final int DEFAULT_VOLUME = 35; //(0 - 150, where 100 is default max volume)

    private final AudioPlayerManager playerManager;
    private final Map<String, GuildMusicManager> musicManagers;

    private Timer timer;
    private TimerTask timerTask;
    private boolean hasStarted = false;

    public old_PlayerControl()
    {
        java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").setLevel(Level.OFF);

        this.playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        //playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager());
        playerManager.registerSourceManager(new LocalAudioSourceManager());

        musicManagers = new HashMap<String, GuildMusicManager>();
    }

    //Prefix for all commands: !
    //Example:  !play
    //Current commands
    // join [name]  - Joins a voice channel that has the provided name
    // join [id]    - Joins a voice channel based on the provided id.
    // leave        - Leaves the voice channel that the bot is currently in.
    // play         - Plays songs from the current queue. Starts playing again if it was previously paused
    // play [url]   - Adds a new song to the queue and starts playing if it wasn't playing already
    // pplay        - Adds a playlist to the queue and starts playing if not already playing
    // pause        - Pauses audio playback
    // stop         - Completely stops audio playback, skipping the current song.
    // skip         - Skips the current song, automatically starting the next
    // nowplaying   - Prints information about the currently playing song (title, current time)
    // np           - alias for nowplaying
    // list         - Lists the songs in the queue
    // volume [val] - Sets the volume of the MusicPlayer [10 - 100]
    // restart      - Restarts the current song or restarts the previous song if there is no current song playing.
    // repeat       - Makes the player repeat the currently playing song
    // reset        - Completely resets the player, fixing all errors and clearing the queue.
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        Channel connectedChannel = event.getMember().getVoiceState().getChannel();
        //MessageChannelUnion channel = event.getChannel();
        //VoiceChannel connectedChannel = channel.asVoiceChannel();

        //VoiceChannel voiceChannel = event.getGuild().getVoiceChannelById("1073341579097550929");
        if (!event.isFromType(ChannelType.TEXT))
            return;

        try
        {
            List<String> allowedIds = Files.readAllLines(Paths.get("admins.txt"));
            if (!allowedIds.contains(event.getAuthor().getId()))
                return;
        }
        catch (IOException ignored)
        {
            //If we encounter an ioe, it is due to the file not existing.
            //In that case, we treat the music system as not having admin restrictions.
        }

        String[] command = event.getMessage().getContentDisplay().split(" ", 2);
        if (!command[0].startsWith("!"))    //message doesn't start with prefix.
            return;

        Guild guild = event.getGuild();
        GuildMusicManager mng = getMusicManager(guild);
        AudioPlayer player = mng.player;
        TrackScheduler scheduler = mng.scheduler;

        if ("!join".equals(command[0]))
        {
            if(connectedChannel == null) {
                event.getChannel().sendMessage("You are not connected to a voice channel!").queue();
                return;
            }
            else
            {
                Channel chan = null;
                try
                {
                    chan = connectedChannel;
                }
                catch (NumberFormatException ignored) {}
                //guild.getAudioManager().setSendingHandler(mng.sendHandler);

                try
                {
                    guild.getAudioManager().openAudioConnection((AudioChannel) connectedChannel);
                }
                catch (PermissionException e)
                {
                    if (e.getPermission() == Permission.VOICE_CONNECT)
                    {
                        event.getChannel().sendMessage("Zaba does not have permission to connect to: " + chan.getName()).queue();
                    }
                }

            }
        }
        else if ("!leave".equals(command[0]))
        {
            guild.getAudioManager().setSendingHandler(null);
            guild.getAudioManager().closeAudioConnection();
        }
        else if ("!play".equals(command[0]))
        {
            if(hasStarted){
                try {
                    timerTask.cancel();
                } catch (NullPointerException r){
                    System.out.println(r);
                }
                hasStarted = false;
            }
            if(connectedChannel == null) event.getChannel().sendMessage("You are not connected to a voice channel!").queue();
            if(!guild.getAudioManager().isConnected()){
                guild.getAudioManager().setSendingHandler(mng.getSendHandler());
                guild.getAudioManager().openAudioConnection((AudioChannel) connectedChannel);
            }
            if (command.length == 1) //It is only the command to start playback (probably after pause)
            {
                if (player.isPaused())
                {
                    player.setPaused(false);
                    event.getChannel().sendMessage("Playback as been resumed.").queue();
                }
                else if (player.getPlayingTrack() != null)
                {
                    event.getChannel().sendMessage("Player is already playing!").queue();
                }
                //else if (scheduler.queue.isEmpty())
                //{
                //    event.getChannel().sendMessage("The current audio queue is empty! Add something to the queue first!").queue();
               // }
            }
            else    //Commands has 2 parts, !play and url.
            {
                loadAndPlay(mng, event.getChannel(), command[1], event);
            }
        }
        else if ("!pplay".equals(command[0]) && command.length == 2)
        {
            timerTask.cancel();
            loadAndPlay(mng, event.getChannel(), command[1], event);
        }
        else if ("!skip".equals(command[0]))
        {
            scheduler.nextTrack();
            event.getChannel().sendMessage("The current track was skipped.").queue();
        }
        else if ("!pause".equals(command[0]))
        {
            if (player.getPlayingTrack() == null)
            {
                event.getChannel().sendMessage("Cannot pause or resume player because no track is loaded for playing.").queue();
                return;
            }

            player.setPaused(!player.isPaused());
            if (player.isPaused())
                event.getChannel().sendMessage("The player has been paused.").queue();
            else
                event.getChannel().sendMessage("The player has resumed playing.").queue();
        }
        else if ("!stop".equals(command[0]))
        {
            // scheduler.queue.clear();
            player.stopTrack();
            player.setPaused(false);
            event.getChannel().sendMessage("Playback has been completely stopped and the queue has been cleared.").queue();
            hasStarted = true;
        }
        else if ("!volume".equals(command[0]))
        {
            if (command.length == 1)
            {
                event.getChannel().sendMessage("Current player volume: **" + player.getVolume() + "**").queue();
            }
            else
            {
                try
                {
                    int newVolume = Math.max(10, Math.min(100, Integer.parseInt(command[1])));
                    int oldVolume = player.getVolume();
                    player.setVolume(newVolume);
                    event.getChannel().sendMessage("Player volume changed from `" + oldVolume + "` to `" + newVolume + "`").queue();
                }
                catch (NumberFormatException e)
                {
                    event.getChannel().sendMessage("`" + command[1] + "` is not a valid integer. (10 - 100)").queue();
                }
            }
        }
        else if ("!restart".equals(command[0]))
        {
            AudioTrack track = player.getPlayingTrack();
            if (track == null)
            // track = scheduler.lastTrack;

            if (track != null)
            {
                event.getChannel().sendMessage("Restarting track: " + track.getInfo().title).queue();
                player.playTrack(track.makeClone());
            }
            else
            {
                event.getChannel().sendMessage("No track has been previously started, so the player cannot replay a track!").queue();
            }
        }
        else if ("!repeat".equals(command[0]))
        {
            //scheduler.setRepeating(!scheduler.isRepeating());
            //event.getChannel().sendMessage("Player was set to: **" + (scheduler.isRepeating() ? "repeat" : "not repeat") + "**").queue();
        }
        else if ("!reset".equals(command[0]))
        {
            //synchronized (musicManagers)
            //{
               // scheduler.queue.clear();
               // player.destroy();
               // guild.getAudioManager().setSendingHandler(null);
              //  musicManagers.remove(guild.getId());
          //  }

           // mng = getMusicManager(guild);
           // guild.getAudioManager().setSendingHandler(mng.sendHandler);
           // event.getChannel().sendMessage("The player has been completely reset!").queue();

        }
        else if ("!nowplaying".equals(command[0]) || "!np".equals(command[0]))
        {
            AudioTrack currentTrack = player.getPlayingTrack();
            if (currentTrack != null)
            {
                String title = currentTrack.getInfo().title;
                String position = getTimestamp(currentTrack.getPosition());
                String duration = getTimestamp(currentTrack.getDuration());

                String nowplaying = String.format("**Playing:** %s\n**Time:** [%s / %s]",
                        title, position, duration);

                event.getChannel().sendMessage(nowplaying).queue();
            }
            else
                event.getChannel().sendMessage("The player is not currently playing anything!").queue();
        }
        /*else if ("!list".equals(command[0]))
        {
            Queue<AudioTrack> queue = scheduler.queue;
            synchronized (queue)
            {
                if (queue.isEmpty())
                {
                    event.getChannel().sendMessage("The queue is currently empty!").queue();
                }
                else
                {
                    int trackCount = 0;
                    long queueLength = 0;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Current Queue: Entries: ").append(queue.size()).append("\n");
                    for (AudioTrack track : queue)
                    {
                        queueLength += track.getDuration();
                        if (trackCount < 10)
                        {
                            sb.append("`[").append(getTimestamp(track.getDuration())).append("]` ");
                            sb.append(track.getInfo().title).append("\n");
                            trackCount++;
                        }
                    }
                    sb.append("\n").append("Total Queue Time Length: ").append(getTimestamp(queueLength));

                    event.getChannel().sendMessage(sb.toString()).queue();
                }
            }
        }*/
/*        else if ("!shuffle".equals(command[0]))
        {
            if (scheduler.queue.isEmpty())
            {
                event.getChannel().sendMessage("The queue is currently empty!").queue();
                return;
            }

            scheduler.shuffle();
         */  // event.getChannel().sendMessage("The queue has been shuffled!").queue();
      // }
    }

    private void loadAndPlayy(GuildMusicManager mng, final Channel channel, String url, final boolean addPlaylist, MessageReceivedEvent event)
    {
        final String trackUrl;

        //Strip <>'s that prevent discord from embedding link resources
        if (url.startsWith("<") && url.endsWith(">"))
            trackUrl = url.substring(1, url.length() - 1);
        else
            trackUrl = url;

        playerManager.loadItemOrdered(mng, trackUrl, new AudioLoadResultHandler()
        {
            @Override
            public void trackLoaded(AudioTrack track)
            {
                String msg = "Adding to queue: " + track.getInfo().title;
                if (mng.player.getPlayingTrack() == null)
                    msg += "\nand the bot has started playing;";

                mng.scheduler.queue(track);
                event.getChannel().sendMessage(msg).queue();

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist)
            {
                AudioTrack firstTrack = playlist.getSelectedTrack();
                List<AudioTrack> tracks = playlist.getTracks();


                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                if (addPlaylist)
                {
                    event.getChannel().sendMessage("Adding **" + playlist.getTracks().size() +"** tracks to queue from playlist: " + playlist.getName()).queue();
                    tracks.forEach(mng.scheduler::queue);
                }
                else
                {
                    event.getChannel().sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();
                    mng.scheduler.queue(firstTrack);
                }
            }

            @Override
            public void noMatches()
            {
                event.getChannel().sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception)
            {
                event.getChannel().sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void loadAndPlay(GuildMusicManager musicManager, final Channel channel, final String trackUrl, MessageReceivedEvent event) {

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                event.getChannel().sendMessage("Adding to queue " + track.getInfo().title).queue();

                play(event.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                event.getChannel().sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(event.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                event.getChannel().sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getChannel().sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }
    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        //connectToFirstVoiceChannel(guild.getAudioManager());

        musicManager.scheduler.queue(track);
    }

    private void skipTrack(Channel channel, GuildMusicManager musicManager) {
        //GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        //channel.sendMessage("Skipped to next track.").queue();
    }


    private GuildMusicManager getMusicManager(Guild guild)
    {
        String guildId = guild.getId();
        GuildMusicManager mng = musicManagers.get(guildId);
        if (mng == null)
        {
            synchronized (musicManagers)
            {
                mng = musicManagers.get(guildId);
                if (mng == null)
                {
                    mng = new GuildMusicManager(playerManager);
                    mng.player.setVolume(DEFAULT_VOLUME);
                    musicManagers.put(guildId, mng);
                }
            }
        }
        return mng;
    }

    private static String getTimestamp(long milliseconds)
    {
        int seconds = (int) (milliseconds / 1000) % 60 ;
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        int hours   = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

        if (hours > 0)
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        else
            return String.format("%02d:%02d", minutes, seconds);
    }
}