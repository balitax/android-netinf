package android.netinf.node;

import java.util.Map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.netinf.R;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    public static boolean getPreferenceAsBoolean(String key) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Node.getContext());
        return prefs.getBoolean(key, false);

    }

    public static int getPreferenceAsInt(String key) {
        return Integer.valueOf(getPreference(key));
    }

    public static long getPreferenceAsLong(String key) {
        return Long.valueOf(getPreference(key));
    }

    public static String getPreference(String key) {
        String value = PreferenceManager.getDefaultSharedPreferences(Node.getContext()).getString(key, null);
        if (value == null) {
            throw new IllegalArgumentException("Preferences does not contain the key '" + key + "'");
        }
        return value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load preferences
            addPreferencesFromResource(R.xml.preferences);

            // Set default summaries
            Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(getActivity()).getAll();
            for (String key : prefs.keySet()) {
                updateDependencies(key);
                updateSummary(key);
            }

        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updateDependencies(key);
            updateSummary(key);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        private void updateSummary(String key) {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Map<String, ?> entries = prefs.getAll();

            if (entries.get(key) instanceof String) {
                findPreference(key).setSummary(entries.get(key).toString());
            }
        }

        private void updateDependencies(String key) {

            // Bluetooth Routing
            if (key.equals("pref_key_bluetooth_routing")) {
                boolean enabled = getPreferenceScreen().getSharedPreferences().getString("pref_key_bluetooth_routing", "").equalsIgnoreCase("Static");
                findPreference("pref_key_bluetooth_static_devices").setEnabled(enabled);
            }

            // Http Routing
            if (key.equals("pref_key_http_routing")) {
                boolean enabled = getPreferenceScreen().getSharedPreferences().getString("pref_key_http_routing", "").equalsIgnoreCase("Static");
                findPreference("pref_key_http_static_peers").setEnabled(enabled);
            }

        }
    }

}
