package events;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeriodicEvent extends ListenerAdapter {
    // PeriodicEvents are for routine functions based on time
    public static Dotenv dotenv = Dotenv.load();
    String uri = dotenv.get("URI");
    // Scheduler
    public void onReady(@NotNull ReadyEvent event) {
        fridayScheduling(event.getJDA());
        holidayScheduler(event.getJDA());
        moodScheduler(event.getJDA());
        birthdayScheduler(event.getJDA());
        statusSet(event.getJDA());
        //leaveServer(event.getJDA());
        //creditCheckScheduler(event.getJDA());
    }
    public void statusSet(JDA jda){
        jda.getPresence().setActivity(Activity.watching("you"));
    }

    // creditCheck was for sending private messages upon reaching a specific credit
    // this didn't work out because I waited to long to get it working
    public void creditCheckScheduler(JDA jda){
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        ZonedDateTime nextFirstLesson = now.withHour(8).withMinute(0).withSecond(0);
        if (now.compareTo(nextFirstLesson) > 0) {
            nextFirstLesson = nextFirstLesson.plusDays(1);
        }
        Duration durationUntilFirstLesson = Duration.between(now, nextFirstLesson);
        long initialDelayFirstLesson = durationUntilFirstLesson.getSeconds();
        ScheduledExecutorService schedulerFirstLesson = Executors.newScheduledThreadPool(1);
        schedulerFirstLesson.scheduleAtFixedRate(() -> creditCheck(jda),
                initialDelayFirstLesson,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }
    public void creditCheck(JDA jda) {
        JSONParser parser = new JSONParser();
        JSONObject response = null;
        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .serverApi(ServerApi.builder()
                        .version(ServerApiVersion.V1)
                        .build())
                .build();
        System.out.println("* * * * * CREDIT CHECK * * * *");
        try {
            Object obj = parser.parse(new FileReader("keywords.json"));
            JSONObject jsonObject = (JSONObject) obj;
            response = (JSONObject) jsonObject.get("socialCredit");

        } catch (Exception e) {
            e.printStackTrace();
        }

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            MongoDatabase database = mongoClient.getDatabase("ChillGrill");
            MongoCollection<Document> collection = database.getCollection("socialcredit");

            try {
                Bson projectionFields = Projections.fields(
                        Projections.include("username", "userid", "score"),
                        Projections.excludeId());
                try (MongoCursor<Document> cursor = collection.find()
                        .projection(projectionFields)
                        .sort(Sorts.descending("score")).iterator()) {
                    while (cursor.hasNext()) {
                        Document doc = cursor.next();
                        int credit = doc.getInteger("score");
                        if (credit == 150) {
                            String userid = doc.getString("userid");
                            JSONObject finalResponse = response;
                            Objects.requireNonNull(jda.getUserById(userid)).openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(finalResponse.get("250").toString()))
                                    .queue();
                            System.out.println("************************************************************");
                            System.out.println(doc.getString("username") + "reached 150!");
                            System.out.println("************************************************************");
                        }
                        if (credit == 300) {
                            String userid = doc.getString("userid");
                            JSONObject finalResponse = response;
                            Objects.requireNonNull(jda.getUserById(userid)).openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(finalResponse.get("250").toString()))
                                    .queue();
                            System.out.println("************************************************************");
                            System.out.println(doc.getString("username") + "reached 300!");
                            System.out.println("************************************************************");
                        }
                        if (credit == 600) {
                            String userid = doc.getString("userid");
                            JSONObject finalResponse = response;
                            Objects.requireNonNull(jda.getUserById(userid)).openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(finalResponse.get("500").toString()))
                                    .queue();
                            System.out.println("************************************************************");
                            System.out.println(doc.getString("username") + "reached 600!");
                            System.out.println("************************************************************");
                        }
                        if (credit == 750) {
                            String userid = doc.getString("userid");
                            JSONObject finalResponse = response;
                            Objects.requireNonNull(jda.getUserById(userid)).openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(finalResponse.get("750").toString()))
                                    .queue();
                            System.out.println("************************************************************");
                            System.out.println(doc.getString("username") + "reached 750!");
                            System.out.println("************************************************************");
                        }
                        if (credit == 840) {
                            String userid = doc.getString("userid");
                            JSONObject finalResponse = response;
                            Objects.requireNonNull(jda.getUserById(userid)).openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(finalResponse.get("850").toString()))
                                    .queue();
                            System.out.println("************************************************************");
                            System.out.println(doc.getString("username") + "reached 840!");
                            System.out.println("************************************************************");
                        }
                        if (credit == 990) {
                            String userid = doc.getString("userid");
                            JSONObject finalResponse = response;
                            Objects.requireNonNull(jda.getUserById(userid)).openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(finalResponse.get("1000").toString()))
                                    .queue();
                            System.out.println("************************************************************");
                            System.out.println(doc.getString("username") + "reached 990!");
                            System.out.println("************************************************************");
                        }
                    }
                }
            } catch (MongoException me) {
                System.err.println("ERROR");
            }
        }
    }

    // Friday meme posting
    public void fridayScheduling(JDA jda){
        int scheduleHour = 8;

        Date aDate = new Date();
        Calendar with = Calendar.getInstance();
        with.setTime(aDate);
        Map<Integer, Integer> dayToDelay = new HashMap<>();
        dayToDelay.put(Calendar.FRIDAY, 6);
        dayToDelay.put(Calendar.SATURDAY, 5);
        dayToDelay.put(Calendar.SUNDAY, 4);
        dayToDelay.put(Calendar.MONDAY, 3);
        dayToDelay.put(Calendar.TUESDAY, 2);
        dayToDelay.put(Calendar.WEDNESDAY, 1);
        dayToDelay.put(Calendar.THURSDAY, 0);
        int dayOfWeek = with.get(Calendar.DAY_OF_WEEK);
        int hour = with.get(Calendar.HOUR_OF_DAY);
        int minute = with.get(Calendar.MINUTE);
        int second = with.get(Calendar.SECOND);
        int delayInDays = dayToDelay.get(dayOfWeek);
        int delayInHours;
        int delayInMinutes;
        int delayInSeconds;
        if(delayInDays == 6 && hour<scheduleHour){
            delayInHours = (scheduleHour - hour) - 1;
            delayInMinutes = ((60 - minute) + (delayInHours * 60));
            delayInSeconds = (delayInMinutes*60 - second);
        }else{
            delayInHours = (delayInDays*24+((24-hour)+scheduleHour)) - 1;
            delayInMinutes = (60 - minute) + (delayInHours * 60);
            delayInSeconds = (delayInMinutes*60 - second);
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                fridayPosting(jda);
            } catch (Exception ex) {
                ex.printStackTrace(); // or loggger would be better
            }
        }, delayInSeconds, 604800, TimeUnit.SECONDS);
    }
    public void fridayPosting(JDA jda){
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            Object obj = parser.parse(new FileReader("keywords.json"));
            jsonObject = (JSONObject) obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert jsonObject != null;
        JSONObject friday = (JSONObject) jsonObject.get("friday");
        int max = friday.size();
        int min = 1;
        int range = max - min + 1;
        int rand = (int)(Math.random() * range) + min;
        String rng = String.valueOf(rand);
        String meme= friday.get(rng).toString();

        if(Math.random() > .85) {
            MessageCreateData data = new MessageCreateBuilder()
                    .setContent("Behold, everyone! It's **Friday**")
                    .setFiles(FileUpload.fromData(new File("videos/friday/"  + meme)))
                    .build();
            Objects.requireNonNull(jda.getTextChannelById("944254315630035005")).sendMessage(data).queue();
            Objects.requireNonNull(jda.getTextChannelById("816125354875944964")).sendMessage(data).queue();
            //Objects.requireNonNull(jda.getTextChannelById("165246172892495872")).sendMessage("Behold, everyone! \nIt's **Friday**").addFile(new File("videos/friday/" + meme)).queue();
            System.out.println("------------------- Friday: " + meme);
        }
        else{
            System.out.println("------------------- No Friday for today");
        }
    }
    public void moodScheduler(JDA jda){
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        ZonedDateTime nextFirstLesson = now.withHour(11).withMinute(0).withSecond(0);
        if (now.compareTo(nextFirstLesson) > 0) {
            nextFirstLesson = nextFirstLesson.plusDays(1);
        }
        Duration durationUntilFirstLesson = Duration.between(now, nextFirstLesson);
        long initialDelayFirstLesson = durationUntilFirstLesson.getSeconds();

        ScheduledExecutorService schedulerFirstLesson = Executors.newScheduledThreadPool(1);
        schedulerFirstLesson.scheduleAtFixedRate(() -> moodPosting(jda),
                initialDelayFirstLesson,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }
    public void moodPosting(JDA jda){
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            Object obj = parser.parse(new FileReader("keywords.json"));
            jsonObject = (JSONObject) obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert jsonObject != null;

        JSONObject moods = (JSONObject) jsonObject.get("moods");
        int max = moods.size();
        int min = 1;
        int range = max - min + 1;
        int rand = (int)(Math.random() * range) + min;
        String rng = String.valueOf(rand);
        JSONObject mood =(JSONObject) moods.get(rng);

        String str = mood.keySet().toString();
        String key = str.substring(1, str.length() - 1);

        Guild guild = jda.getGuildById("816125354875944960");
        assert guild != null;
        if(Math.random() > .98 || Math.random() < .02) {
            //Objects.requireNonNull(jda.getTextChannelById("816125354875944964")).sendMessage("Behold, everyone! \nToday's mood is: **" + key + "**").addFiles((Collection<? extends FileUpload>) new File("videos/moods/" + mood.get(key).toString())).queue();
            System.out.println("------------------- Mood: " + key);
        }
        else{
            System.out.println("------------------- No mood for today");
        }
    }
    public void holidayScheduler(JDA jda){
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        ZonedDateTime next = now.withHour(7).withMinute(0).withSecond(0);
        if (now.compareTo(next) > 0) {
            next = next.plusDays(1);
        }
        Duration durationUntil = Duration.between(now, next);
        long initialDelay = durationUntil.getSeconds();

        ScheduledExecutorService schedulerFirstLesson = Executors.newScheduledThreadPool(1);
        schedulerFirstLesson.scheduleAtFixedRate(() -> holidayPosting(jda),
                initialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }
    public void holidayPosting(JDA jda) {
        String chatID = "816125354875944964";
        String today = LocalDate.now().toString().substring(5);

        JSONParser parser = new JSONParser();
        JSONObject jsonObject;

        // Load JSON
        JSONObject dates = null;
        JSONObject poi = null;
        try {
            Object obj = parser.parse(new FileReader("keywords.json"));
            jsonObject = (JSONObject) obj;
            dates = (JSONObject) jsonObject.get("holidays");

        } catch (Exception e) {
            e.printStackTrace();
        }
        assert dates != null;
        Set<String> holidays = dates.keySet();
        if(holidays.contains(today)) {
            Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("Behold, everyone..").queue();
            Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage(dates.get(today).toString()).queue();
            System.out.print(dates.get(today).toString());
        }
    }
    public void birthdayScheduler(JDA jda){
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        ZonedDateTime next = now.withHour(8).withMinute(0).withSecond(0);
        if (now.compareTo(next) > 0) {
            next = next.plusDays(1);
        }
        Duration durationUntilFirstLesson = Duration.between(now, next);
        long initialDelay = durationUntilFirstLesson.getSeconds();

        ScheduledExecutorService schedulerFirstLesson = Executors.newScheduledThreadPool(1);
        schedulerFirstLesson.scheduleAtFixedRate(() -> birthdayPosting(jda),
                initialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }
    public void birthdayPosting(JDA jda) {
        String chatID = "816125354875944964";
        String today = LocalDate.now().toString().substring(5);
        System.out.println("Today's Date: " + today);

        JSONParser parser = new JSONParser();
        JSONObject jsonObject;

        // Load JSON
        JSONObject dates = null;
        JSONObject poi = null;
        try {
            Object obj = parser.parse(new FileReader("keywords.json"));
            jsonObject = (JSONObject) obj;
            dates = (JSONObject) jsonObject.get("birthdays");
            poi = (JSONObject) jsonObject.get("poi");

        } catch (Exception e) {
            e.printStackTrace();
        }
        assert dates != null;
        assert poi != null;
        Set<String> birthDates = dates.keySet();
        if(birthDates.contains(today)) {
            Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("Behold, everyone!").queue();
            String id = poi.get(dates.get(today)).toString();
            StringBuilder mention = new StringBuilder();
            mention.append("<@");
            mention.append(id);
            mention.append(">");
            System.out.print("Today's birthday is: " + dates.get(today).toString());
            if(dates.get(today).toString().equals("Jon")){

                Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage(":green_circle: Today is " + mention + "'s birthday! :purple_circle:").queue();
                Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("\n⣿⣿⠿⠿⠿⠿⣿⣷⣂⠄⠄⠄⠄⠄⠄⠈⢷⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿\n" +
                        "⣷⡾⠯⠉⠉⠉⠉⠚⠑⠄⡀⠄⠄⠄⠄⠄⠈⠻⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿\n" +
                        "⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄⡀⠄⠄⠄⠄⠄⠄⠄⠄⠉⠻⣿⣿⣿⣿⣿⣿⣿⣿⣿\n" +
                        "⠄⠄⠄⠄⠄⠄⠄⠄⠄⢀⠎⠄⠄⣀⡀⠄⠄⠄⠄⠄⠄⠄⠘⠋⠉⠉⠉⠉⠭⠿⣿\n" +
                        "⡀⠄⠄⠄⠄⠄⠄⠄⠄⡇⠄⣠⣾⣳⠁⠄⠄⢺⡆⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄⠄\n" +
                        "⣿⣷⡦⠄⠄⠄⠄⠄⢠⠃⢰⣿⣯⣿⡁⢔⡒⣶⣯⡄⢀⢄⡄⠄⠄⠄⠄⠄⣀⣤⣶\n" +
                        "⠓⠄⠄⠄⠄⠄⠸⠄⢀⣤⢘⣿⣿⣷⣷⣿⠛⣾⣿⣿⣆⠾⣷⠄⠄⠄⠄⢀⣀⣼⣿\n" +
                        "⠄⠄⠄⠄⠄⠄⠄⠑⢘⣿⢰⡟⣿⣿⣷⣫⣭⣿⣾⣿⣿⣴⠏⠄⠄⢀⣺⣿⣿⣿⣿\n" +
                        "⣿⣿⣿⣿⣷⠶⠄⠄⠄⠹⣮⣹⡘⠛⠿⣫⣾⣿⣿⣿⡇⠑⢤⣶⣿⣿⣿⣿⣿⣿⣿\n" +
                        "⣿⣿⣿⣯⣤⣤⣤⣤⣀⣀⡹⣿⣿⣷⣯⣽⣿⣿⡿⣋⣴⡀⠈⣿⣿⣿⣿⣿⣿⣿⣿\n" +
                        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣾⣝⡻⢿⣿⡿⠋⡒⣾⣿⣧⢰⢿⣿⣿⣿⣿⣿⣿⣿\n" +
                        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠃⣏⣟⣼⢋⡾⣿⣿⣿⣘⣔⠙⠿⠿⠿⣿⣿⣿\n" +
                        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⠿⠿⣛⡵⣻⠿⠟⠁⠛⠰⠿⢿⠿⡛⠉⠄⠄⢀⠄⠉⠉⢉\n" +
                        "⣿⣿⣿⣿⡿⢟⠩⠉⣠⣴⣶⢆⣴⡶⠿⠟⠛⠋⠉⠩⠄⠉⢀⠠⠂⠈⠄⠐⠄⠄⠄").queue();
            }
            if(dates.get(today).toString().equals("Gustavo")){

                Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("<:bowsette:510714047595806738> Today is " + mention + "'s birthday! <:2D:250844355429007370>").queue();
                Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("https://media.discordapp.net/attachments/261297258803363850/995463962516787232/Goose.png").queue();
            }
            if(dates.get(today).toString().equals("Corey")){
                Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("<:sus:802264386026340403> Today is " + mention + "'s birthday! <:justatheory:971167345437462589>").queue();
                Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("https://cdn.discordapp.com/attachments/261297258803363850/997717479537246278/unknown.png").queue();

                Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("Today is also " + "<@267697069643399168>" + "'s birthday!").queue();
            }

            else{
                Objects.requireNonNull(jda.getTextChannelById(chatID)).sendMessage("Today is " + mention + "'s birthday!").queue();
            }
        }
    }


    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        AudioChannelUnion channelLeft = event.getChannelLeft();
        AudioChannelUnion channelJoined = event.getChannelJoined();

        if (channelLeft == null || channelJoined != null) {
            return;
        }

        // A user has disconnected from the voice channel

        Guild guild = event.getGuild();
        Member selfMember = guild.getSelfMember();

        AudioChannelUnion botChannel = selfMember.getVoiceState().getChannel();

        if (botChannel == null || !botChannel.equals(channelLeft)) {
            // The bot is either not connected or connected to a different channel.
            return;
        }

        List<Member> remainingMembers = channelLeft.getMembers();

        long humansPresent = remainingMembers.stream()
                .filter(member -> !member.getUser().isBot())
                .count();

        if (humansPresent == 0) {

            // Disconnect
            guild.getAudioManager().closeAudioConnection();

            // Log
            System.out.printf("Disconnected from Voice Channel '%s' in Guild '%s' because I was alone (zero human users).%n",
                    channelLeft.getName(), guild.getName());
        }
    }
    public void leaveServer(JDA jda) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        ZonedDateTime next = now.withHour(3).withMinute(0).withSecond(0);
        if (now.compareTo(next) > 0) {
            next = next.plusDays(1);
        }
        Duration durationUntil = Duration.between(now, next);
        long initialDelay = durationUntil.getSeconds();

        ScheduledExecutorService schedulerFirstLesson = Executors.newScheduledThreadPool(1);
        schedulerFirstLesson.scheduleAtFixedRate(() -> {
                    System.out.println("It's 3 AM!");
                    jda.getGuildById("816125354875944960").getAudioManager().setSendingHandler(null);
                    jda.getGuildById("816125354875944960").getAudioManager().closeAudioConnection();

                },
                initialDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);

    }
}
