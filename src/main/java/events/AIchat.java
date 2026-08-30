package events;

import static events.UserStatEvent.dotenv;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AIchat extends ListenerAdapter {
  String apiKey = dotenv.get("JWT");
  private final List<ChatMessage> chatHistory = new ArrayList<>();
  private final int MAX_HISTORY = 20;

  static class ChatMessage {
    String role;
    String content;

    ChatMessage(String role, String content) {
      this.role = role;
      this.content = content;
    }
  }

  public static boolean isJarvisOnline() {
    try (java.net.Socket socket = new java.net.Socket()) {
      // Try to connect to localhost:3000 with a 1-second timeout
      socket.connect(new java.net.InetSocketAddress("192.168.1.223", 3000), 1000);
      return true;
    } catch (java.io.IOException e) {
      return false; // Server is down or port is blocked
    }
  }

  // Inside your ListenerAdapter class
  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) return;

    if (!isJarvisOnline()) {
      return;
    }

    String content = event.getMessage().getContentRaw().toLowerCase();
    long botId = event.getJDA().getSelfUser().getIdLong();

    boolean startsWithTrigger = content.startsWith("zaba");
    boolean isReplyToBot = false;
    if (event.getMessage().getReferencedMessage() != null) {
      isReplyToBot = event.getMessage().getReferencedMessage().getAuthor().getIdLong() == botId;
    }

    if (!startsWithTrigger && !isReplyToBot) {
      return;
    }

    String userPrompt = event.getMessage().getContentRaw();
    if (startsWithTrigger) {
      userPrompt = userPrompt.substring(4).trim();
    }

    chatHistory.add(new ChatMessage("user", userPrompt));

    while (chatHistory.size() > MAX_HISTORY) {
      chatHistory.remove(0);
    }

    // Convert the entire history list to a JSON array using Gson
    Gson gson = new Gson();
    JsonArray messagesArray = new JsonArray();

    // Always start with your System Prompt (Personality)
    JsonObject systemMsg = new JsonObject();
    systemMsg.addProperty("role", "system");
    systemMsg.addProperty("content", "You are Zaba, an AI Discord bot with personality.");
    messagesArray.add(systemMsg);

    // Add the rolling history
    for (ChatMessage msg : chatHistory) {
      JsonObject m = new JsonObject();
      m.addProperty("role", msg.role);
      m.addProperty("content", msg.content);
      messagesArray.add(m);
    }

    // Build the final JSON body
    JsonObject finalBody = new JsonObject();
    finalBody.addProperty("model", "zaba");
    finalBody.add("messages", messagesArray);
    finalBody.addProperty("stream", false);

    String jsonBody = gson.toJson(finalBody);

    HttpClient client =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1) // This is the magic line
            .build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://192.168.1.223:3000/api/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

    // Send the request and reply to Discord
    client
        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(HttpResponse::body)
        .thenAccept(
            responseBody -> {
              JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
              String aiResponse =
                  jsonObject
                      .getAsJsonArray("choices")
                      .get(0)
                      .getAsJsonObject()
                      .get("message")
                      .getAsJsonObject()
                      .get("content")
                      .getAsString();

              // Add Jarvis's response to the history so he remembers what HE said
              synchronized (chatHistory) {
                chatHistory.add(new ChatMessage("assistant", aiResponse));
                if (chatHistory.size() > MAX_HISTORY) chatHistory.remove(0);
              }
              event.getChannel().sendMessage(aiResponse).queue();
            });
  }
}
