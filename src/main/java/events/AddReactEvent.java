package events;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;


import static com.mongodb.client.model.Filters.eq;

public class AddReactEvent extends ListenerAdapter {
    static Dotenv dotenv = Dotenv.load();
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        Message msg = event.retrieveMessage().complete();
        String username = msg.getAuthor().getName();
        String userid = msg.getAuthor().getId();
        String reactor = "g";//msg.getAuthor().getName();

        // Trigger when message add react +15
        if(event.getReactionEmote().getId().equals("900119408859578451") && !username.equals(reactor)) {
            try (MongoClient mongoClient = MongoClients.create(dotenv.get("URI"))) {

                MongoDatabase database = mongoClient.getDatabase("ChillGrill");
                MongoCollection<Document> collection = database.getCollection("socialcredit");
                Bson projectionFields = Projections.fields(
                        Projections.include("username", "score", "userid"),
                        Projections.excludeId());
                Document doc = collection.find(eq("userid", userid))
                        .projection(projectionFields)
                        .first();
                if (doc == null) {

                    try {
                        InsertOneResult result = collection.insertOne(new Document()
                                .append("_id", new ObjectId())
                                .append("username", username)
                                .append("userid", userid)
                                .append("score", 15));

                        System.out.println("\nSuccess! Inserted document id: " + result.getInsertedId() + "add15");
                    } catch (MongoException me) {
                        System.err.println("\nUnable to insert due to an error: " + me);
                    }
                } else {
                    int currVal = doc.getInteger("score");
                    doc.append("score", currVal + 15);
                    System.out.println("\nold: " + currVal + " new " + doc.getInteger("score"));
                    try {
                        Bson query = eq("userid", userid);
                        ReplaceOptions opts = new ReplaceOptions().upsert(true);

                        UpdateResult result = collection.replaceOne(query, doc, opts);

                        System.out.println("\nModified document count: " + result.getModifiedCount());
                    } catch (MongoException me) {
                        System.err.println("\nUnable to update due to an error: " + me);
                    }
                }
            }
        }

        // Trigger when message add react -15
        if(event.getReactionEmote().getId().equals("934919187787288597") && !username.equals(reactor)) {

            try (MongoClient mongoClient = MongoClients.create(dotenv.get("URI"))) {
                MongoDatabase database = mongoClient.getDatabase("ChillGrill");
                MongoCollection<Document> collection = database.getCollection("socialcredit");
                Bson projectionFields = Projections.fields(
                        Projections.include("username", "score", "userid"),
                        Projections.excludeId());
                Document doc = collection.find(eq("userid", userid))
                        .projection(projectionFields)
                        .first();

                if (doc == null) {
                    try {
                        InsertOneResult result = collection.insertOne(new Document()
                                .append("_id", new ObjectId())
                                .append("username", username)
                                .append("userid", userid)
                                .append("score", -15));

                        System.out.println("\nSuccess! Inserted document id: " + result.getInsertedId() + "add-15");
                    } catch (MongoException me) {
                        System.err.println("\nUnable to insert due to an error: " + me);
                    }
                } else {
                    int currVal = doc.getInteger("score");
                    doc.append("score", currVal - 15);
                    System.out.println("\nold: " + currVal + " new " + doc.getInteger("score"));
                    try {
                        Bson query = eq("userid", userid);
                        ReplaceOptions opts = new ReplaceOptions().upsert(true);

                        UpdateResult result = collection.replaceOne(query, doc, opts);

                        System.out.println("\nModified document count: " + result.getModifiedCount());
                    } catch (MongoException me) {
                        System.err.println("Unable to update due to an error: " + me);
                    }
                }
            }
        }
    }
}
