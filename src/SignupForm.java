import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class SignupForm extends JFrame {
    private JTextField usernameTxf;
    private JButton signupBtn;
    private JPanel signupLyt;
    private JLabel passwordLbl;
    private JPasswordField passwordTxf;
    private JLabel usernameLbl;
    private JPanel rootPanel;
    private JButton cancelBtn;

    public SignupForm() {
        super("Chat App");
        setContentPane(rootPanel);

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        signupBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameTxf.getText();
                String password = passwordTxf.getText();
                try {
                    Main.client.signUp(username, password, obj -> {
                        if (!Main.isSuccess(obj)) {
                            JOptionPane.showMessageDialog(null, Main.getContent(obj), "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null, "Registration success!", "Success", JOptionPane.INFORMATION_MESSAGE);
                            NavigationController.getInstance().popFrame();
                        }
                    });
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Error connecting to Server", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: Add extra event for cancel.
                NavigationController.getInstance().popFrame();
            }
        });
    }
}
