package events;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;

public class UserStatEvent extends ListenerAdapter {
  public static Dotenv dotenv = Dotenv.load();
  String uri = dotenv.get("URI");

  public void onMessageReceived(MessageReceivedEvent event) {

    if (event.getAuthor().isBot()) return;

    // Get author name, parse message for processing
    String userid = event.getMessage().getAuthor().getId();
    String username = event.getAuthor().getName();
    String fullmsg = event.getMessage().getContentRaw().toLowerCase();
    String[] msg = fullmsg.split(" ");

    // Database connection
    ConnectionString connectionString = new ConnectionString(uri);
    MongoClientSettings settings =
        MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
            .build();

    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("badwords.txt"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    String[] words = reader.lines().toArray(String[]::new);

    Map<String, Integer> wordCnt = new HashMap<>();

    // Check if the input string contains any of the words from the array
    for (String cuss : words) {
      String regex = "\\b" + cuss + "\\b";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(fullmsg);
      int count = 0;
      while (matcher.find()) {
        count++;
      }
      if (count > 0) {
        wordCnt.put(cuss, count);
      }
    }

    // Checks if HashMap isn't empty
    if (!wordCnt.isEmpty()) {
      try (MongoClient mongoClient = MongoClients.create(settings)) {
        MongoDatabase database = mongoClient.getDatabase("ChillGrill");
        MongoCollection<Document> collection = database.getCollection("UserStats");

        // Finds document with userid of author
        Document query = new Document("userid", userid);
        Document update = new Document();

        Document document = collection.find(query).first();

        if (document != null) {
          Document naughties = (Document) document.get("naughties");
          for (Map.Entry<String, Integer> entry : wordCnt.entrySet()) {
            String word = entry.getKey();
            Integer value = entry.getValue();
            if (naughties.containsKey(word)) {
              Integer total = naughties.getInteger(word) + value;
              naughties.put(word, total);
              if (total == 100)
                event
                    .getChannel()
                    .sendMessage("Congrats on your 100th use of the word: **" + word + "**!")
                    .complete();
              if (total == 50)
                event
                    .getChannel()
                    .sendMessage("Congrats on your 50th use of the word: **" + word + "**!")
                    .complete();
            } else {
              naughties.put(word, value);
            }
          }
          update.append("$set", new Document("naughties", naughties));
          collection.updateOne(query, update);

          System.out.println("NAUGHTY: " + wordCnt + " Spoken by: " + username);

        } else {

          // No document was found, creating one
          System.out.println("NAUGHTY: " + wordCnt + " Spoken by: " + username);
          System.out.println("No document found for user ID: " + userid);

          Document userDoc = new Document("userid", userid);
          userDoc.append("username", username);
          Document newNaughties = new Document();
          for (Map.Entry<String, Integer> entry : wordCnt.entrySet()) {
            String word = entry.getKey();
            Integer value = entry.getValue();
            newNaughties.append(word, value);
          }
          userDoc.append("naughties", newNaughties);
          collection.insertOne(userDoc);

          System.out.println("Created new document for: " + username);
        }
      }
    }

    if (fullmsg.equalsIgnoreCase("!swearjar")) {
      try (MongoClient mongoClient = MongoClients.create(settings)) {
        MongoDatabase database = mongoClient.getDatabase("ChillGrill");
        MongoCollection<Document> collection = database.getCollection("UserStats");

        Document doc = collection.find(eq("userid", userid)).first();

        if (doc != null && doc.containsKey("naughties")) {
          Document naughties = doc.get("naughties", Document.class);

          EmbedBuilder builder =
              new EmbedBuilder()
                  .setTitle("Swear Jar for " + username)
                  .setThumbnail(
                      "https://raw.githubusercontent.com/snowbowls/Zaba/master/images/eyepop.png")
                  .setColor(Color.MAGENTA);

          StringBuilder cusses = new StringBuilder();
          StringBuilder cusscnt = new StringBuilder();

          for (String key : naughties.keySet()) {
            int count = naughties.getInteger(key);
            cusses.append(key + "\n");
            cusscnt.append(String.valueOf(count) + "\n");
          }
          builder.addField("Cuss", cusses.toString(), true);
          builder.addField("Count", cusscnt.toString(), true);
          event.getChannel().sendMessageEmbeds(builder.build()).queue();
        } else {
          event.getChannel().sendMessage("You haven't said any naughties yet!").queue();
        }
      }
    }
    if (fullmsg.equalsIgnoreCase("!swearjar all")) {
      try (MongoClient mongoClient = MongoClients.create(settings)) {
        MongoDatabase database = mongoClient.getDatabase("ChillGrill");

        // Get the "UserStats" collection
        MongoCollection<Document> collection = database.getCollection("UserStats");

        // Create a new embed builder
        EmbedBuilder builder =
            new EmbedBuilder()
                .setTitle("Swear Jar for " + event.getGuild().getName())
                .setThumbnail(
                    "https://raw.githubusercontent.com/snowbowls/Zaba/master/images/eyepop.png")
                .setColor(Color.MAGENTA);

        // Iterate over all documents in the collection
        MongoCursor<Document> cursor = collection.find().iterator();
        while (cursor.hasNext()) {
          Document document = cursor.next();

          // Get the username and naughties values from the document
          String name = document.getString("username");
          Document naughties = document.get("naughties", Document.class);

          StringBuilder cusses = new StringBuilder();

          // Get the highest naughties value
          int highestNaughties = 0;
          String cuss1 = null;
          int secNaughties = 0;
          String cuss2 = null;
          int thirdNaughties = 0;
          String cuss3 = null;
          for (String key : naughties.keySet()) {
            int value = naughties.getInteger(key);
            if (value > highestNaughties) {
              thirdNaughties = secNaughties;
              secNaughties = highestNaughties;
              highestNaughties = value;
              cuss3 = cuss2;
              cuss2 = cuss1;
              cuss1 = key;
            } else if (value > secNaughties) {
              thirdNaughties = secNaughties;
              secNaughties = value;
              cuss3 = cuss2;
              cuss2 = key;
            } else if (value > thirdNaughties) {
              thirdNaughties = value;
              cuss3 = key;
            }
          }

          cusses.append(cuss1 + " - " + highestNaughties + "\n");
          if (secNaughties > 0) cusses.append(cuss2 + " - " + secNaughties + "\n");
          if (thirdNaughties > 0) cusses.append(cuss3 + " - " + thirdNaughties + "\n");

          // Add the username and highest naughties value to the embed builder
          builder.addField(name, cusses.toString(), true);
        }
        event.getChannel().sendMessageEmbeds(builder.build()).queue();
      }
    }
  }
}
