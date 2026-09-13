package tv.mimo.app.mobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.*;

public class SettingsFragment extends MobileBaseFragment {

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_settings;
    }

    @Override
    protected void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, new SettingsPreferenceFragment())
            .commit();
    }

    public static class SettingsPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.mobile_preferences, rootKey);

            findPreference("pref_about")?.setOnPreferenceClickListener(preference -> {
                Fragment aboutFragment = new AboutFragment();
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, aboutFragment)
                    .addToBackStack(null)
                    .commit();
                return true;
            });

            findPreference("pref_diagnostics")?.setOnPreferenceClickListener(preference -> {
                // Open diagnostics URL
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv?check=1"));
                startActivity(intent);
                return true;
            });

            findPreference("pref_manage_sources")?.setOnPreferenceClickListener(preference -> {
                // TODO: Navigate to source management - placeholder for now
                return true;
            });
        }
    }
}