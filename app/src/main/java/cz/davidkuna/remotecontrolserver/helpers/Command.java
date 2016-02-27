package cz.davidkuna.remotecontrolserver.helpers;

/**
 * Created by David Kuna on 27.2.16.
 */
public class Command {

    public static final String GET_DATA = "getData";
    public static final String MOVE_UP = "w";
    public static final String MOVE_RIGHT = "d";
    public static final String MOVE_DOWN = "s";
    public static final String MOVE_LEFT = "a";
    public static final String NONE = "undefined";

    private String name = NONE;
    private String value = NONE;

    public Command() {}

    public Command(String command) {
        name = command;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
