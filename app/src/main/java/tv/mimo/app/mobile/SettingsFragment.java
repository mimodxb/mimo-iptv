package tv.mimo.app.mobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.*;
import tv.mimo.app.R;

public class SettingsFragment extends MobileBaseFragment {

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_settings;
    }

    @Override
    public void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, new SettingsPreferenceFragment())
            .commit();
    }

    public static class SettingsPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.mobile_preferences, rootKey);

            Preference aboutPref = findPreference("pref_about");
            if (aboutPref != null) {
                aboutPref.setOnPreferenceClickListener(preference -> {
                    Fragment aboutFragment = new AboutFragment();
                    requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, aboutFragment)
                        .addToBackStack(null)
                        .commit();
                    return true;
                });
            }

            Preference diagnosticsPref = findPreference("pref_diagnostics");
            if (diagnosticsPref != null) {
                diagnosticsPref.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv?check=1"));
                    startActivity(intent);
                    return true;
                });
            }

            Preference sourcesPref = findPreference("pref_manage_sources");
            if (sourcesPref != null) {
                sourcesPref.setOnPreferenceClickListener(preference -> {
                    return true;
                });
            }
        }
    }
}