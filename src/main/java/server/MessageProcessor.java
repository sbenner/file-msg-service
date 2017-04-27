package server;

import commons.types.Message;

/**
 * Created with IntelliJ IDEA.
 * User: sbenner
 * Date: 12/9/16
 * Time: 8:46 PM
 */
public interface MessageProcessor {
    public void processMessage(Message message);

}
