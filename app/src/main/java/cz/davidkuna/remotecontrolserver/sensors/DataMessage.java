package cz.davidkuna.remotecontrolserver.sensors;

import org.json.JSONArray;
import org.json.JSONStringer;

import java.util.ArrayList;

/**
 * Created by David Kuna on 4.2.16.
 */
public class DataMessage extends JSONStringer {

    private ArrayList<String[]> data = new ArrayList<String[]>();

    public DataMessage() {

    }

    public DataMessage addData(String name, String value) {
        String[] item = {name, value};
        data.add(item);

        return this;
    }

    @Override
    public String toString() {
        JSONArray jsArray = new JSONArray(data);
        return jsArray.toString();
    }
}
