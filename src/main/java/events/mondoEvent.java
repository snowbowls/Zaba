package events;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.bson.types.ObjectId;

public class mondoEvent extends ListenerAdapter {

  public static final String uri = System.getenv("URI");

  public void onMessageReceived(MessageReceivedEvent event) {

    String username = event.getAuthor().getName();

    try (MongoClient mongoClient = MongoClients.create(uri)) {
      MongoDatabase database = mongoClient.getDatabase("ChillGrill");
      MongoCollection<Document> collection = database.getCollection("socialcredit");
      try {
        InsertOneResult result =
            collection.insertOne(
                new Document()
                    .append("_id", new ObjectId())
                    .append("user", username)
                    .append("score", 0));
        System.out.println("Success! Inserted document id: " + result.getInsertedId());
      } catch (MongoException me) {
        System.err.println("Unable to insert due to an error: " + me);
      }
    }
  }
}
