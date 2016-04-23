package cz.davidkuna.remotecontrolserver.activity;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import cz.davidkuna.remotecontrolserver.R;
import cz.davidkuna.remotecontrolserver.video.CameraStream;

public class Preferences extends Activity {

    public static final String PREF_USE_STUN = "useStun";
    public static final String PREF_STUN_SERVER = "stunServer";
    public static final String PREF_STUN_PORT = "stunPort";
    public static final String PREF_RELAY_SERVER = "relayServer";

    private final String DEF_STUN_SERVER = "stun.sipgate.net";
    private final String DEF_RELAY_SERVER = "http://remotecontrol.8u.cz/wp-login.php";

    private SharedPreferences prefs;
    private EditText stunServer = null;
    private EditText stunPort = null;
    private EditText relayServer = null;
    private CheckBox useStun = null;
    private Spinner videoQuality = null;
    private ArrayAdapter<String> videoQualityArrayAdapter;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        stunServer = (EditText) findViewById(R.id.stunServer);
        stunPort = (EditText) findViewById(R.id.stunPort);
        relayServer = (EditText) findViewById(R.id.relayServer);
        useStun = (CheckBox) findViewById(R.id.useStun);
        videoQuality = (Spinner) findViewById(R.id.videoQuality);
        videoQualityArrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.video_quality_list));
        videoQuality.setAdapter(videoQualityArrayAdapter);
        Button save = (Button) findViewById(R.id.bSave);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
            }
        });


        loadPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
    }

    private void savePreferences() {
        prefs.edit().putBoolean(PREF_USE_STUN, useStun.isChecked()).commit();
        prefs.edit().putString(PREF_STUN_SERVER, stunServer.getText().toString()).commit();
        prefs.edit().putInt(PREF_STUN_PORT, Integer.valueOf(stunPort.getText().toString())).commit();
        prefs.edit().putString(PREF_RELAY_SERVER, relayServer.getText().toString()).commit();
        prefs.edit().putString(CameraStream.PREF_JPEG_QUALITY, videoQuality.getSelectedItem().toString()).commit();
        prefs.edit().apply();
        Toast toast = Toast.makeText(this, "Settings has been saved", Toast.LENGTH_SHORT);
        toast.show();
    }

    private void loadPreferences() {
        useStun.setChecked(prefs.getBoolean(PREF_USE_STUN, false));
        stunServer.setText(prefs.getString(PREF_STUN_SERVER, DEF_STUN_SERVER));
        relayServer.setText(prefs.getString(PREF_RELAY_SERVER, DEF_RELAY_SERVER));
        stunPort.setText(Integer.toString(prefs.getInt(PREF_STUN_PORT, 10000)));
        int spinnerPosition = videoQualityArrayAdapter.getPosition(prefs.getString(String.valueOf(CameraStream.PREF_JPEG_QUALITY), "10"));
        videoQuality.setSelection(spinnerPosition);
    }
}
