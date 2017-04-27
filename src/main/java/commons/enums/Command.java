package commons.enums;

/**
 * Created with IntelliJ IDEA.
 * User: sbenner
 * Date: 12/4/16
 * Time: 3:55 AM
 */
public enum Command {
    FILE(1), TEXT(2), ACTION(3);
    private final int type;

    Command(int type) {
        this.type = type;
    }

    public static Command getValue(int value) {
        for (Command e : Command.values()) {
            if (e.type == value) {
                return e;
            }
        }
        return null;// not found
    }

}
