import server.MessageServer;

import java.io.IOException;

/**
 * Created by sbenner on 28/04/2017.
 */
public class StartServer {

    public static void main(String[] args) throws IOException {
        MessageServer server = new MessageServer(10523);
        (new Thread(server)).start();
    }

}
