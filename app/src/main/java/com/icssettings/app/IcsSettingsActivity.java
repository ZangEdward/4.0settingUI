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

    private Header mInitialHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings_title);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
        // Capture the first header that has a real fragment, so the
        // multi-pane auto-select (onGetInitialHeader) never targets a
        // title-only category divider (which would crash with NPE).
        for (Header h : target) {
            if (h.fragment != null) {
                mInitialHeader = h;
                break;
            }
        }
    }

    /**
     * On large-screen (multi-pane) devices ICS auto-opens the header returned
     * here. The dashboard's first header is a title-only category divider with
     * a null fragment, which would make Fragment.instantiate(null) throw.
     * Return the first header that actually has a fragment instead.
     */
    @Override
    public Header onGetInitialHeader() {
        return mInitialHeader != null ? mInitialHeader : super.onGetInitialHeader();
    }

    /**
     * Belt-and-suspenders guard: if the framework somehow tries to switch to a
     * title-only category header (null fragment), ignore it. Real headers still
     * call through to the framework.
     */
    @Override
    public void switchToHeader(Header header) {
        if (header == null || header.fragment == null || header.fragment.length() == 0) {
            return;
        }
        super.switchToHeader(header);
    }
}
