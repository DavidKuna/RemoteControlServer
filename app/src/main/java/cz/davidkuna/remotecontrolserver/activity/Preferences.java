package cz.davidkuna.remotecontrolserver.activity;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import cz.davidkuna.remotecontrolserver.R;

public class Preferences extends PreferenceActivity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            loadHeadersFromResource(R.xml.preference_headers, target);
        }
    }
    
    @SuppressLint("NewApi")
    public static class SensorsPreferencesFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            PreferenceManager.setDefaultValues(getActivity(),
                    R.xml.sensors_preferences, false);

            addPreferencesFromResource(R.xml.sensors_preferences);
            if(!isSensorAvailable(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            	CheckBoxPreference checkbox = (CheckBoxPreference)getPreferenceManager().findPreference("accelerometr_enabled");
            	checkbox.setEnabled(false);
            }
            if(!isSensorAvailable(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
            	CheckBoxPreference checkbox = (CheckBoxPreference)getPreferenceManager().findPreference("gyroscope_enabled");
            	checkbox.setEnabled(false);
            }
            if(!isSensorAvailable(PackageManager.FEATURE_SENSOR_COMPASS)) {
            	CheckBoxPreference checkbox = (CheckBoxPreference)getPreferenceManager().findPreference("compass_enabled");
            	checkbox.setEnabled(false);
            }
        }
        
        public boolean isSensorAvailable(String sensor) {
        	PackageManager PM = getActivity().getPackageManager();
        	return PM.hasSystemFeature(sensor);
        }
    }

}
