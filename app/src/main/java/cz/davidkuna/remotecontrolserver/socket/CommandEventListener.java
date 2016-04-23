package cz.davidkuna.remotecontrolserver.socket;

import cz.davidkuna.remotecontrolserver.helpers.Command;

/**
 * Created by David Kuna on 21.4.16.
 */
public interface CommandEventListener {
    void onCommandReceived(Command command);
}
