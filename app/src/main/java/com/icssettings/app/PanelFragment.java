package com.icssettings.app;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Fragment shown for every dashboard item. It chooses its preference layout
 * from the "panel" extra passed by the header in res/xml/settings_headers.xml.
 *
 * Each panel uses the real AOSP Settings preference widgets (SwitchPreference,
 * CheckBoxPreference, ListPreference, Preference, PreferenceCategory) and the
 * real AOSP Settings drawables, so the look matches Android 4.0 (ICS).
 */
public class PanelFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String panel = getArguments() != null ? getArguments().getString("panel") : "about";
        int res = resolvePanel(panel);
        if (res != 0) {
            addPreferencesFromResource(res);
        }
    }

    private int resolvePanel(String panel) {
        if ("wifi".equals(panel))            return R.xml.panel_wifi;
        if ("bluetooth".equals(panel))       return R.xml.panel_bluetooth;
        if ("data_usage".equals(panel))      return R.xml.panel_data_usage;
        if ("more".equals(panel))            return R.xml.panel_more;
        if ("sound".equals(panel))           return R.xml.panel_sound;
        if ("display".equals(panel))         return R.xml.panel_display;
        if ("storage".equals(panel))         return R.xml.panel_storage;
        if ("battery".equals(panel))         return R.xml.panel_battery;
        if ("apps".equals(panel))            return R.xml.panel_apps;
        if ("accounts".equals(panel))        return R.xml.panel_accounts;
        if ("location".equals(panel))        return R.xml.panel_location;
        if ("security".equals(panel))        return R.xml.panel_security;
        if ("language_input".equals(panel))  return R.xml.panel_language_input;
        if ("backup".equals(panel))          return R.xml.panel_backup;
        if ("dock".equals(panel))            return R.xml.panel_dock;
        if ("datetime".equals(panel))        return R.xml.panel_datetime;
        if ("accessibility".equals(panel))   return R.xml.panel_accessibility;
        if ("developer".equals(panel))       return R.xml.panel_developer;
        return R.xml.panel_about;
    }
}
