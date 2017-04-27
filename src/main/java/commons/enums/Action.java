package commons.enums;

/**
 * Created with IntelliJ IDEA.
 * User: sbenner
 * Date: 12/4/16
 * Time: 3:55 AM
 */
public enum Action {
    MOVE((byte) 1), HIT((byte) 2), DODGE((byte) 3);
    private final byte type;

    Action(byte type) {
        this.type = type;
    }

    public static Action getValue(int value) {
        for (Action e : Action.values()) {
            if (e.type == value) {
                return e;
            }
        }
        return null;// not found
    }

}