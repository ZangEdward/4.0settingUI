package com.icssettings.app;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import java.util.List;

/**
 * Top-level Settings activity.
 *
 * Mirrors the structure of the AOSP Settings app (android-4.0.4_r2.1):
 * a {@link PreferenceActivity} that loads its dashboard from
 * res/xml/settings_headers.xml and opens each item as a {@link PanelFragment}.
 * The look is provided entirely by the framework's Holo theme and the real
 * AOSP Settings drawables, so it renders identically on every Android version.
 */
public class IcsSettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings_title);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
    }
}
