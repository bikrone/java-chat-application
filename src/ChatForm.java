import javafx.scene.input.KeyCode;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

public class ChatForm extends JFrame {
    private JList userList;
    private JPanel rootPanel;
    private JSplitPane splitPaneVertical;
    private JTextArea chatTxa;
    private JTextField inputTxt;
    private JButton sendBtn;
    private JSplitPane splitPaneHorizontal;
    private JScrollPane userScrollPane;
    private JScrollPane scrollBar;
    private JButton broadcastBtn;
    DefaultListModel listModel;

    private String currentPartner = null;

    public ChatForm() throws IOException{
        super("Chat App");
        setContentPane(rootPanel);

        listModel = new DefaultListModel();

        Main.client.setMessageCallback(obj -> {
            newMessage((String)obj.get("from"), Main.getContent(obj));
        });

        Main.client.setNewUserCallback(obj -> {
            System.out.println("WE HERE");
            newUser((String)obj.get("from"));
        });

        Main.client.setRemoveUserCallback(obj -> {
            removeUser((String)obj.get("from"));
        });

        Main.client.getUserList(obj -> {
            Object[] list = Main.getList(obj);
            for (Object user : list) {
                if (((String)user).equals(Main.username)) continue;
                listModel.addElement((String)user);
                Main.newUser((String)user);
            }

        });

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        userList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        userList.setVisibleRowCount(-1);
        userList.setLayoutOrientation(JList.VERTICAL);

        // TODO: Apply list data with user objects and customize in UserCell.


        userList.setCellRenderer(new UserCell());
        userList.setModel(listModel);

        System.out.println(userList.getSize().height);

        userList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                if (e.getValueIsAdjusting() == false) {
                    if (userList.getSelectedIndex() == -1) {
                        currentPartner = null;
                        chatTxa.setText("");
                    } else {
                        // TODO: Start a chat session with a user.
                        //System.out.println("Selected index: " + userList.getSelectedIndex());
                        currentPartner = ((String)userList.getSelectedValue()).split(" ")[0];
                        chatTxa.setText(Main.getHtmlMessageFrom(currentPartner));
                        JScrollBar vertical = scrollBar.getVerticalScrollBar();
                        vertical.setValue( vertical.getMaximum() );
                        inputTxt.grabFocus();
                        Main.messagesCountFrom.put(currentPartner, 0);
                        updateMessageCount(currentPartner, 0);
                    }
                }
            }
        });

        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessageAction();
            }
        });

        broadcastBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = inputTxt.getText();

                if (!text.trim().equalsIgnoreCase("")) {
                    broadcast(text);
                    inputTxt.setText("");
                }
            }
        });
        inputTxt.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                // ENTER pressed
                if (e.getKeyCode() == 10) {
                    sendMessageAction();
                }
            }
        });
    }

    private void newUser(String user) {
        if (Main.users.contains(user)) return;
        Main.newUser(user);
        listModel.addElement(user);
    }

    private void removeUser(String user) {
        if (Main.users.contains(user)) {
            Main.removeUser(user);
            for (Object o : listModel.toArray()) {
                String realName = ((String)o).split(" ")[0];
                if (realName.equals(user)) {
                    listModel.removeElement(o);
                    break;
                }
            }
        }
    }

    private void updateMessageCount(String user,  Integer count) {

        for (int i=0; i<listModel.size(); i++) {
            String o = (String)listModel.elementAt(i);
            String realName = o.split(" ")[0];
            if (realName.equals(user)) {
                if (count > 0)
                    listModel.setElementAt(realName + " (" +count.toString() + ")", i);
                else {
                    listModel.setElementAt(realName, i);
                }
                break;
            }
        }

    }

    private void sendMessageAction() {
        String text = inputTxt.getText();

        if (!text.trim().equalsIgnoreCase("")) {
            sendMessage(text, currentPartner);
        }
        inputTxt.setText("");
    }

    private void newMessage(String from, String message) {

        ChatMessage mess = new ChatMessage(message, from);
        Main.addMessage(from, mess);

        if (from.equals(currentPartner)) {
            if (Main.messagesCountFrom.get(currentPartner) != null && Main.messagesCountFrom.get(currentPartner) != 0) {
                Main.messagesCountFrom.put(currentPartner, 0);
                updateMessageCount(from, 0);
            }

            printMessage(mess);
            return;
        }

        Integer tmp;
        if ((tmp = Main.messagesCountFrom.get(from)) == null) {
            Main.messagesCountFrom.put(from, 1);
            tmp = 1;
        } else {
            Main.messagesCountFrom.put(from, ++tmp);
        }

        updateMessageCount(from, tmp);


    }

    private void broadcast(String text) {
        try {
            Main.client.broadcast(text, obj -> {
                String content = text;
                if (!Main.isSuccess(obj)) {
                    content += " (error)";
                }

                for (String user : Main.users) {
                    ChatMessage mess = new ChatMessage(content, Main.username);
                    Main.addMessage(user, mess);
                    if (user.equals(currentPartner)) {
                        printMessage(mess);
                    }
                }
            });
        } catch (IOException ignored) {}
    }

    private void printMessage(ChatMessage message) {
        chatTxa.append(message.sender + ": " + message.content + "\n");
        JScrollBar vertical = scrollBar.getVerticalScrollBar();
        vertical.setValue( vertical.getMaximum() );
    }

    private void sendMessage(String text, String to) {
        if (to == null) return;
        try {
            Main.client.sendMessage(to, text, obj -> {
                String content = text;
                if (!Main.isSuccess(obj)) {
                    content += " (error)";
                }
                ChatMessage message =  new ChatMessage(content, Main.username);
                Main.addMessage(to, message);
                if (to.equals(currentPartner)) {
                    printMessage(message);
                }
            });
        } catch (IOException ignored) { }
    }
}
