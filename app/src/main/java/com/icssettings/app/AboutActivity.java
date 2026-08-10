package com.icssettings.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake "About phone" (关于手机) screen for the ICS Settings replica.
 *
 * <p>This is a SELF-CONTAINED, crash-proof screen: it deliberately performs NO
 * system reads (no {@code SettingsHelper}, no {@code /proc} access, no
 * permission requests) and shows ICS-style informational values. Its only real
 * purpose is to host the Android 4.0 easter-egg trigger — tap "Android version"
 * seven times to open the Ice Cream Sandwich screen. That egg is exclusive to
 * ICS and not shipped in any other Android version, so re-implementing it here
 * is the correct way to bring it back.
 *
 * <p>Because it avoids every API that is restricted or behaviour-changed on
 * modern Android (e.g. 16), this screen works identically from Android 4.0
 * through Android 16 — clicking it can never crash.
 */
public class AboutActivity extends Activity implements PrefAdapter.Callback {

    private ListView mList;
    private PrefAdapter mAdapter;
    private final List<PrefItem> mItems = new ArrayList<>();

    private int mVersionTaps = 0;
    private int mBuildTaps = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ListView list = new ListView(this);
        list.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        list.setCacheColorHint(0);
        setContentView(list);
        mList = list;

        if (getActionBar() != null) {
            getActionBar().setTitle(getString(R.string.about_settings));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        buildItems();
        mAdapter = new PrefAdapter(getLayoutInflater(), mItems);
        mAdapter.setCallback(this);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                PrefItem item = mItems.get(pos);
                if (item.type == PrefItem.TYPE_CATEGORY) return;
                if (item.action != null) {
                    item.action.run();
                } else if (item.intent != null) {
                    try {
                        startActivity(item.intent);
                    } catch (Throwable t) {
                        Toast.makeText(AboutActivity.this, R.string.toast_no_activity, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void buildItems() {
        mItems.clear();
        mItems.add(PrefItem.category(getString(R.string.p_phone)));
        mItems.add(PrefItem.row(getString(R.string.p_status_cat), getString(R.string.state_no_service)));
        mItems.add(PrefItem.row(getString(R.string.p_legal), null));
        mItems.add(PrefItem.row(getString(R.string.p_model), safeModel()));

        // Android version — the ICS easter-egg trigger (tap 7 times).
        PrefItem ver = PrefItem.row(getString(R.string.p_android_version), "4.0.4");
        ver.action = new Runnable() {
            @Override
            public void run() {
                onVersionTap();
            }
        };
        mItems.add(ver);

        mItems.add(PrefItem.row(getString(R.string.p_baseband), getString(R.string.state_unknown)));
        mItems.add(PrefItem.row(getString(R.string.p_kernel), "3.0.8-gb55e9a8 " + getString(R.string.about_kernel_suffix)));

        // Build number — tap 7 times to "enable developer options" (virtual joke).
        PrefItem build = PrefItem.row(getString(R.string.p_build), "IMM76L");
        build.action = new Runnable() {
            @Override
            public void run() {
                onBuildTap();
            }
        };
        mItems.add(build);
    }

    /** Safe on every Android version; never throws. */
    private static String safeModel() {
        try {
            String m = Build.MODEL;
            if (m == null || m.isEmpty()) m = "Android SDK";
            return m + "  ·  ICS replica";
        } catch (Throwable t) {
            return "Android SDK  ·  ICS replica";
        }
    }

    // ----------------------------------------------------- easter egg trigger
    private void onVersionTap() {
        mVersionTaps++;
        if (mVersionTaps >= 7) {
            mVersionTaps = 0;
            try {
                startActivity(new Intent(this, EasterEggActivity.class));
            } catch (Throwable t) {
                Toast.makeText(this, R.string.toast_easter_egg, Toast.LENGTH_SHORT).show();
            }
        } else if (mVersionTaps >= 3) {
            Toast.makeText(this,
                    getString(R.string.about_tap_remaining, "4.0.4", 7 - mVersionTaps),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onBuildTap() {
        mBuildTaps++;
        if (mBuildTaps >= 7) {
            mBuildTaps = 0;
            Toast.makeText(this, R.string.dev_options_enabled, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRowClick(PrefItem item) {
        // Row clicks are handled by the ListView listener above.
    }

    @Override
    public void onSwitchToggle(PrefItem item, boolean checked) {
        // No switches on this screen.
    }
}
