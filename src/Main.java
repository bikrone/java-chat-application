import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Main {

    public static HashMap<String, ArrayList<ChatMessage> > messagesWith;
    public static HashMap<String, Integer> messagesCountFrom;
    public static HashSet<String> users = new HashSet<>();

    public static String username;

    public static ChatClient client;

    public static void addMessage(String userWith, ChatMessage message) {
        if (messagesWith.get(userWith) == null) {
            messagesWith.put(userWith, new ArrayList<ChatMessage>());
        }
        messagesWith.get(userWith).add(message);
    }

    public static void newUser(String username) {
        users.add(username);
    }

    public static void removeUser(String username) {
        users.remove(username);
    }

    public static boolean isSuccess(JSONObject obj) {
        return ((String)obj.get("type")).equals("SUCCESS");
    }

    public static String getContent(JSONObject obj) {
        return (String)obj.get("content");
    }

    public static Object[] getList(JSONObject obj) {

        Object o = obj.get("list");
        if (o != null) {
            JSONArray arr = (JSONArray)o;
            System.out.println(arr.toJSONString());
            return arr.toArray();
        }
        return null;
    }

    private static void setupClient() {
        client = new ChatClient("127.0.0.1", 10008);
        client.connect();
    }

    public static String getHtmlMessageFrom(String user) {
        ArrayList<ChatMessage> messages = getMessageFrom(user);
        if (messages == null) return "";
        StringBuilder html = new StringBuilder();
        for (ChatMessage message: messages) {
            //html.append("<b>");
            html.append(message.sender);
            //html.append(":</b> ");
            html.append(": ");
            html.append(message.content);
            html.append("\n");
        }
        return html.toString();
    }
    public static ArrayList<ChatMessage> getMessageFrom(String user) {
        return messagesWith.get(user);
    }

    public static void main(String[] args) {
        messagesWith = new HashMap<>();
        messagesCountFrom = new HashMap<>();
        setupClient();
        StartForm app = new StartForm();
        NavigationController.getInstance().pushFrame(app);
    }
}