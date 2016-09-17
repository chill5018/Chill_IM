package chill_im;

import java.io.Serializable;

/**
 * Created by chill on 9/17/16.
 */
public class ChatMessage implements Serializable {

    protected static final long serialVersionUID = 1112122200L;

    // The different types of message sent by the Client
    // LIST to receive the list of the users connected
    // DATA an ordinary message
    // QUIT to disconnect from the Server
    static final int LIST = 0, DATA = 1, QUIT = 2, JOIN = 3;
    private int type;
    private String message;

    // constructor
    ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    // getters
    int getType() {
        return type;
    }
    String getMessage() {
        return message;
    }
}
