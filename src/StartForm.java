import oracle.jrockit.jfr.JFR;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.UnknownHostException;

public class StartForm extends JFrame {
    private JTextField usernameTxf;
    private JPanel rootPanel;
    private JLabel usernameLbl;
    private JLabel passwordLbl;
    private JPasswordField passwordField1;
    private JPanel passwordLyt;
    private JPanel usernameLyt;
    private JButton loginBtn;
    private JButton signupBtn;
    private JTextField ipTxf;
    private JTextField portTxf;
    private JPanel ipLyt;
    private JPanel portLyt;

    public StartForm() {
        super("Chat App");
        setContentPane(rootPanel);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginAction();
            }
        });

        signupBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (setupClient() == true) {
                    goToSignup();
                }
            }
        });

        passwordField1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == 10) {
                    loginAction();
                }
            }
        });
        usernameTxf.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == 10) {
                    loginAction();
                }
            }
        });
    }

    private boolean setupClient() {
        try {
            if (Main.isStarted) return true;
            Main.setupClient(ipTxf.getText(), Integer.parseInt(portTxf.getText()));
            return true;
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to host", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to host", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to host", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private void loginAction() {
        String username = usernameTxf.getText();
        String password = passwordField1.getText();

        try {
            if (setupClient() == false) return;
            Main.client.signIn(username, password, (obj) -> {
                if (Main.isSuccess(obj)) {
                    Main.username = username;
                    goToChat();
                } else {
                    JOptionPane.showMessageDialog(null, Main.getContent(obj), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (IOException ignored) { }
    }

    private void goToSignup() {
        SignupForm signupForm = new SignupForm();
        NavigationController.getInstance().pushFrame(signupForm);
    }

    private void goToChat() {
        try {
            ChatForm chatForm = new ChatForm();
            NavigationController.getInstance().pushFrame(chatForm);
        } catch (IOException ex) {
            return;
        }
    }
}
