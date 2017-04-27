package commons.types;

import commons.enums.Action;

/**
 * Created by sergey on 12/3/16.
 */
public class ActionMessage extends Message {

    private Action action;


    public ActionMessage() {
        setType((byte)3);
    }


    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}
