package server;

import commons.types.FileMessage;
import commons.types.Message;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: sbenner
 * Date: 12/9/16
 * Time: 8:47 PM
 */
public class FileMessageProcessor implements MessageProcessor{

    public Set<FileMessage> fileMessages=new HashSet<FileMessage>();

    public void processMessage(Message message) {

    }
}
