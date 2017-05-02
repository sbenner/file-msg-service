import server.MessageServer;

import java.io.IOException;


public class StartServer {

    public static void main(String[] args) throws IOException {
        MessageServer server = new MessageServer(10523);
        (new Thread(server)).start();
    }

}
