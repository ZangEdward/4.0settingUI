package com.icssettings.app;

import android.content.Intent;

/**
 * A single row in a Settings list. Replaces the legacy {@code Preference} XML
 * model: every dashboard item and every sub-panel row is described by one of
 * these, so the whole UI is rendered by a single {@link PrefAdapter} that
 * reuses the framework's own {@code android.R.layout.preference} row. That
 * keeps the ICS Holo look pixel-identical on every Android version and removes
 * the deprecated {@code PreferenceActivity} auto-instantiation path that crashed
 * on Android 16.
 */
public class PrefItem {

    public static final int TYPE_CATEGORY = 0; // non-clickable group header
    public static final int TYPE_ROW = 1;      // normal clickable preference row
    public static final int TYPE_SWITCH = 2;   // row with an inline Switch widget

    public static final int SWITCH_NONE = 0;
    public static final int SWITCH_WIFI = 1;
    public static final int SWITCH_BT = 2;

    public int type = TYPE_ROW;
    public int iconRes = 0;            // 0 = no icon
    public CharSequence title;
    public CharSequence summary;       // null/empty hides the summary line
    public String panelKey;            // if set, a row click opens this sub-panel
    public boolean checked = false;    // switch state
    public int switchKind = SWITCH_NONE;
    public boolean enabled = true;
    public Runnable action;            // custom row-click handler
    public Intent intent;              // row click launches this
    public String key;                 // logical key for switch handlers in panels

    public static PrefItem category(CharSequence title) {
        PrefItem i = new PrefItem();
        i.type = TYPE_CATEGORY;
        i.title = title;
        return i;
    }

    public static PrefItem row(int iconRes, CharSequence title, CharSequence summary) {
        PrefItem i = new PrefItem();
        i.type = TYPE_ROW;
        i.iconRes = iconRes;
        i.title = title;
        i.summary = summary;
        return i;
    }

    public static PrefItem row(CharSequence title, CharSequence summary) {
        return row(0, title, summary);
    }

    public static PrefItem switchRow(int iconRes, CharSequence title,
                                     CharSequence summary, int kind, boolean checked) {
        PrefItem i = new PrefItem();
        i.type = TYPE_SWITCH;
        i.iconRes = iconRes;
        i.title = title;
        i.summary = summary;
        i.switchKind = kind;
        i.checked = checked;
        return i;
    }
}
