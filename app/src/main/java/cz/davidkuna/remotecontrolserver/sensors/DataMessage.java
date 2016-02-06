package cz.davidkuna.remotecontrolserver.sensors;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;

/**
 * Created by David Kuna on 4.2.16.
 */
public class DataMessage extends JSONStringer {

    public static final String TYPE_ACCELEROMETER = "acc";
    public static final String TYPE_GYROSCOPE = "gyr";
    public static final String TYPE_COMPASS = "com";
    public static final String TYPE_GPS = "gps";

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

        JSONArray jsArray = new JSONArray();
        try {
            for (String[] item : data) {
                JSONObject jsObject = new JSONObject();
                jsObject.put("name", item[0]);
                jsObject.put("value", item[1]);
                jsArray.put(jsObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsArray.toString();
    }
}
