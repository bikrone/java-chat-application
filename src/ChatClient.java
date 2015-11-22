//Link: https://www.cs.uic.edu/~troy/spring05/cs450/sockets/EchoClient2.java
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UnknownFormatConversionException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatClient extends Thread {
    Integer port = 10008;
    String address = "127.0.0.1";
    Socket echoSocket = null;
    PrintWriter out = null;
    BufferedReader in = null;

    private ChatClient receiver;

    public ChatClient getSender() {
        return this;
    }

    public ChatClient getReceiver() {
        return receiver;
    }

    private HashMap<String, ArrayList<String> > messagesFrom = new HashMap<>();
    private static ConcurrentLinkedQueue<Callback> requestCallbacks = new ConcurrentLinkedQueue<>();
    private static boolean isStop = false;

    public ChatClient(Integer port) {
        this.port = port;
    }

    public ChatClient(String address, Integer port) {
        this.address = address;
        this.port = port;
    }

    private Callback messageCallback, newUserCallback, removeUserCallback;

    public void setMessageCallback(Callback cb) {
        if (receiver != null) {
            receiver.setMessageCallback(cb);
            return;
        }
        messageCallback = cb;
    }

    public void setNewUserCallback(Callback cb) {
        if (receiver != null) {
            receiver.setNewUserCallback(cb);
            return;
        }
        newUserCallback = cb;
    }

    public void setRemoveUserCallback(Callback cb) {
        if (receiver != null) {
            receiver.setRemoveUserCallback(cb);
            return;
        }
        removeUserCallback = cb;
    }

    public void receiveResponse(JSONObject obj) throws IOException{
        Callback cb = requestCallbacks.poll();
        cb.run(obj);
    }

    public void receiveMessage(JSONObject obj) throws IOException{
        String type = (String)obj.get("type");
        System.out.println("get message: " + obj.toJSONString());
        switch (type) {
            case "MESSAGE":
                if (messageCallback != null) messageCallback.run(obj);
                break;
            case "NEW USER":
                System.out.println("GET NEW USER");
                if (newUserCallback != null) {
                    newUserCallback.run(obj);
                }
                break;
            case "REMOVE USER":
                if (removeUserCallback != null) removeUserCallback.run(obj);
                break;
            default:
                receiveResponse(obj);
                break;
        }
    }

    public void startReceiving() {
        StringBuilder str = new StringBuilder();
        String inputLine;
        JSONParser parser = new JSONParser();
        try {
            while ((inputLine = in.readLine()) != null)
            {
                str.append(inputLine);
                try {
                    JSONObject obj = (JSONObject)parser.parse(str.toString());
                    str.setLength(0);
                    receiveMessage(obj);
                } catch (ParseException ignored) { }
            }
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "
                    + "the connection to: ");
            System.exit(1);
        }
    }

    private boolean isReceiving() {
        return (receiver == null);
    }

    public void connect() throws IOException, UnknownHostException{
        String serverHostname = address;

        echoSocket = new Socket(serverHostname, this.port);

        out = new PrintWriter(echoSocket.getOutputStream(), true);

        receiver = new ChatClient(address, port);
        receiver.in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        receiver.start();

    }

    public void run() {
        if (!isReceiving()) return;
        startReceiving();
    }


    public boolean isSuccess(JSONObject obj) {
        if (obj == null) return false;
        String response = (String)obj.get("type");
        return response.equals("SUCCESS");
    }

    public String getResponseContent(JSONObject obj) {
        if (obj == null) return null;
        String response = (String)obj.get("content");
        return response;
    }

    public void sendMessage(String to, String message, Callback cb) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("to", to);
        obj.put("type", "MESSAGE");
        obj.put("content", message);
        out.println(obj.toJSONString());
        requestCallbacks.add(cb);
    }

    public void broadcast(String message, Callback cb) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("broadcast", true);
        obj.put("type", "MESSAGE");
        obj.put("content", message);
        out.println(obj.toJSONString());
        requestCallbacks.add(cb);
    }

    public void signIn(String username, String password, Callback cb) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("from", username);
        obj.put("password", password);
        obj.put("type", "SIGN IN");
        out.println(obj.toJSONString());
        requestCallbacks.add(cb);
    }

    public void signUp(String username, String password, Callback cb) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("from", username);
        obj.put("password", password);
        obj.put("type", "SIGN UP");
        out.println(obj.toJSONString());
        requestCallbacks.add(cb);
    }

    public void signOut() throws IOException {

        JSONObject obj = new JSONObject();
        obj.put("type", "SIGN OUT");
        out.println(obj.toJSONString());
    }

    public void getUserList(Callback cb) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("type", "GET USERS");
        out.println(obj.toJSONString());
        requestCallbacks.add(cb);
    }

    public void disconnect() throws IOException{
        out.close();
        in.close();
        echoSocket.close();
    }
}
