//Link: https://www.cs.uic.edu/~troy/spring05/cs450/sockets/EchoServer2c.java
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import jdk.nashorn.api.scripting.JSObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ChatServer extends Thread
{
    protected static boolean serverContinue = true;
    protected Socket clientSocket;
    protected String clientName;
    protected static Map<String, PrintWriter> users = Collections.synchronizedMap(new HashMap<String, PrintWriter>());
    protected static Map<String, String> userPasswords = Collections.synchronizedMap(new HashMap<String, String>());
    protected static PrintWriter usersOut;

    private static void getUsersFromFile() throws IOException {
        try {
            for (String line : Files.readAllLines(Paths.get("users.txt"))) {
                String[] a = line.split(" ");
                if (a.length != 2) continue;
                String user = a[0]; String password = a[1];
                userPasswords.put(user, password);
            }

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        try {
            usersOut = new PrintWriter(new FileWriter(new File("users.txt"), true));
        } catch (FileNotFoundException ex) {
            System.out.println("Cannot create users.txt");
            System.out.println(ex.getMessage());
        }
    }

    public static void main(String[] args) throws IOException
    {
        getUsersFromFile();
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(10008);
            System.out.println ("Connection Socket Created");
            try {
                while (serverContinue)
                {
                    serverSocket.setSoTimeout(10000);
                    System.out.println ("Waiting for Connection");
                    try {
                        new ChatServer (serverSocket.accept());
                    }
                    catch (SocketTimeoutException ste)
                    {
                        System.out.println ("Timeout Occurred");
                    }
                }
            }
            catch (IOException e)
            {
                System.err.println("Accept failed.");
                System.exit(1);
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port: 10008.");
            System.exit(1);
        }
        finally
        {
            try {
                System.out.println ("Closing Server Connection Socket");
                serverSocket.close();
            }
            catch (IOException e)
            {
                System.err.println("Could not close port: 10008.");
                System.exit(1);
            }
        }
    }

    private ChatServer (Socket clientSoc)
    {
        clientSocket = clientSoc;
        start();
    }

    private void sendMessage(String content, String user, PrintWriter destination) {
        if (destination == null || destination.checkError()) return;
        JSONObject obj = new JSONObject();
        obj.put("type", "MESSAGE");
        obj.put("from", user);
        obj.put("content", content);
        destination.println(obj.toJSONString());
    }

    private void notifyUser(String newUser) {
        for (Map.Entry<String, PrintWriter> entry : users.entrySet()) {
            String user = entry.getKey(); PrintWriter destination = entry.getValue();
            if (newUser.equals(user)) continue;
            if (destination == null || destination.checkError()) continue;
            JSONObject obj = new JSONObject();
            obj.put("type", "NEW USER");
            obj.put("from", newUser);
            destination.println(obj.toJSONString());
        }

    }

    private void removeUser(String removedUser) {
        users.remove(removedUser);
        for (Map.Entry<String, PrintWriter> entry : users.entrySet()) {
            String user = entry.getKey();
            PrintWriter destination = entry.getValue();
            if (removedUser.equals(user)) continue;
            if (destination == null || destination.checkError()) continue;
            JSONObject obj = new JSONObject();
            obj.put("type", "REMOVE USER");
            obj.put("from", removedUser);
            destination.println(obj.toJSONString());
        }
    }


    private String getMD5(String toHash) {
        StringBuffer hexString = new StringBuffer();

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }

        byte[] hash;

        try {
            hash = md.digest(toHash.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            return null;
        }

        for (byte aHash : hash) {
            if ((0xff & aHash) < 0x10) {
                hexString.append("0"
                        + Integer.toHexString((0xFF & aHash)));
            } else {
                hexString.append(Integer.toHexString(0xFF & aHash));
            }
        }
        return hexString.toString();
    }

    private boolean checkUsername(String username) {
        for (int i=0; i<username.length(); i++) {
            char c = username.charAt(i);
            if (!(Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' || c == '.')) return false;
            if (i == 0 && (c == '_' || c == '.')) return false;
        }
        return true;
    }


    // FORMAT
    // type: MESSAGE or COMMAND
    // from: username
    // password: //optional -> for login, sign up
    // to: userid // optional
    // broadcast: true/false
    // content: message content // optional

    public void run()
    {
        System.out.println ("New Communication Thread Started");

        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),
                    true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader( clientSocket.getInputStream()));

            StringBuilder currentMessage = new StringBuilder();
            String inputLine;

            JSONParser parser = new JSONParser();
            boolean isBreak = false;

            while ((inputLine = in.readLine()) != null)
            {
                if (isBreak) break;
                currentMessage.append(inputLine);
                try {
                    JSONObject obj = (JSONObject)parser.parse(currentMessage.toString());
                    currentMessage.setLength(0);
                    String type = (String)obj.get("type");
                    String username = clientName;
                    if (username == null && obj.get("from") != null) {
                        username = (String)obj.get("from");
                    }

                    if (username == null) {
                        JSONObject denyObj = new JSONObject();
                        denyObj.put("type", "DENY");
                        denyObj.put("content", "User not logged in");
                        out.println(denyObj.toJSONString());
                        continue;
                    }
                    if (!checkUsername(username)) {
                        JSONObject denyObj = new JSONObject();
                        denyObj.put("type", "DENY");
                        denyObj.put("content", "Username is invalid");
                        out.println(denyObj.toJSONString());
                        continue;
                    }

                    System.out.println("Receive message " + obj.toJSONString());

                    switch (type) {
                        case "MESSAGE":
                            if (clientName == null) {
                                JSONObject denyObj = new JSONObject();
                                denyObj.put("type", "DENY");
                                denyObj.put("content", "User not logged in");
                                out.println(denyObj.toJSONString());
                                continue;
                            }
                            synchronized (users) {
                                boolean isBroadcast = false;
                                if (obj.get("broadcast") != null) {
                                    isBroadcast = (boolean)obj.get("broadcast");
                                }
                                String message = (String) obj.get("content");
                                if (isBroadcast) {
                                    for (Map.Entry<String, PrintWriter> entry : users.entrySet()) {
                                        if (entry.getKey().equals(clientName)) continue;
                                        sendMessage(message, clientName, entry.getValue());
                                    }
                                    JSONObject denyObj = new JSONObject();
                                    denyObj.put("type", "SUCCESS");
                                    out.println(denyObj.toJSONString());
                                } else {
                                    String target = (String) obj.get("to");
                                    if (users.get(target) == null) {
                                        JSONObject denyObj = new JSONObject();
                                        denyObj.put("type", "DENY");
                                        denyObj.put("content", "Target user is not logged in");
                                        out.println(denyObj.toJSONString());
                                    } else {
                                        sendMessage(message, clientName, users.get(target));
                                        JSONObject denyObj = new JSONObject();
                                        denyObj.put("type", "SUCCESS");
                                        out.println(denyObj.toJSONString());
                                    }
                                }
                            }
                            break;
                        case "SIGN IN":
                            String serverPassword = userPasswords.get(username);
                            if (serverPassword == null) {
                                JSONObject denyObj = new JSONObject();
                                denyObj.put("type", "DENY");
                                denyObj.put("content", "No username exists");
                                out.println(denyObj.toJSONString());
                            } else {
                                String clientPassword = (String)obj.get("password");
                                if (serverPassword.equals(getMD5(clientPassword))) {

                                    synchronized (users) {
                                        if (users.get(username) == null) {
                                            clientName = username;
                                            users.put(clientName, out);
                                            notifyUser(clientName);
                                            JSONObject denyObj = new JSONObject();
                                            denyObj.put("type", "SUCCESS");
                                            out.println(denyObj.toJSONString());
                                        } else {
                                            JSONObject denyObj = new JSONObject();
                                            denyObj.put("type", "DENY");
                                            denyObj.put("content", "User already signed in");
                                            out.println(denyObj.toJSONString());
                                        }
                                    }

                                } else {
                                    JSONObject denyObj = new JSONObject();
                                    denyObj.put("type", "DENY");
                                    denyObj.put("content", "Wrong password");
                                    out.println(denyObj.toJSONString());
                                }
                            }

                            break;
                        case "SIGN UP":
                            synchronized (userPasswords) {
                                String pass = userPasswords.get(username);
                                if (pass == null) {
                                    String password = getMD5((String)obj.get("password"));
                                    userPasswords.put(username, password);
                                    usersOut.println(username + " " + password);
                                    usersOut.flush();
                                    JSONObject denyObj = new JSONObject();
                                    denyObj.put("type", "SUCCESS");
                                    out.println(denyObj.toJSONString());
                                } else {
                                    JSONObject denyObj = new JSONObject();
                                    denyObj.put("type", "DENY");
                                    denyObj.put("content", "User already exists");
                                    out.println(denyObj.toJSONString());
                                }
                            }
                            break;
                        case "GET USERS":
                            synchronized (users) {
                                JSONObject resObj = new JSONObject();
                                resObj.put("type", "LIST");
                                JSONArray arr = new JSONArray();

                                for (Map.Entry<String, PrintWriter> entry : users.entrySet()) {
                                    arr.add(entry.getKey());
                                }

                                resObj.put("list", arr);
                                out.println(resObj.toJSONString());
                            }
                            break;
                        case "SIGN OUT":
                            if (clientName != null) {
                                synchronized (users) {
                                    removeUser(clientName);
                                }
                            }
                            clientName = null;
                            break;
                    }
                } catch (ParseException ignored) {
                }
            }

            if (clientName != null) {
                synchronized (users) {
                    removeUser(clientName);
                }
            }
            clientName = null;
            out.close();
            in.close();

            clientSocket.close();
        }
        catch (IOException e)
        {
            System.err.println("Problem with Communication Server");
            System.exit(1);
        }
    }
}
