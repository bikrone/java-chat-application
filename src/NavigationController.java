import javax.swing.*;
import java.util.ArrayList;

public class NavigationController {
    private static NavigationController ourInstance = new NavigationController();
    private ArrayList<JFrame> frameStack;

    public static NavigationController getInstance() {
        return ourInstance;
    }

    private NavigationController() {
        frameStack = new ArrayList<JFrame>();
    }

    public void pushFrame(JFrame frame) {
        if (frameStack.size() > 0) {
            JFrame topFrame = frameStack.get(frameStack.size() - 1);
            topFrame.setVisible(false);
        }
        frame.setVisible(true);
        frameStack.add(frame);
    }

    public void popFrame() {
        if (frameStack.size() >= 2) {
            JFrame popFrame = frameStack.get(frameStack.size() - 1);
            frameStack.remove(frameStack.size() - 1);

            JFrame topFrame = frameStack.get(frameStack.size() - 1);
            popFrame.dispose();
            topFrame.setVisible(true);
        }
    }
}
