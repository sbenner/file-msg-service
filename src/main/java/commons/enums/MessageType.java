package commons.enums;

/**
 * Created with IntelliJ IDEA.
 * User: sbenner
 * Date: 12/4/16
 * Time: 3:55 AM
 */
public enum MessageType {
    FILE(1), TEXT(2), ACTION(3);
    private final int type;

    MessageType(int type) {
        this.type = type;
    }

    public static MessageType getValue(int value) {
        for (MessageType e : MessageType.values()) {
            if (e.type == value) {
                return e;
            }
        }
        return null;// not found
    }

}
