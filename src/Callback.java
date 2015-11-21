import org.json.simple.JSONObject;

import java.io.IOException;

public interface Callback {
    void run(JSONObject response) throws IOException;
}
