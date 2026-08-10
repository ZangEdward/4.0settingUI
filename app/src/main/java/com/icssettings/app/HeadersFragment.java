package com.icssettings.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * The top-level dashboard list (WIRELESS &amp; NETWORKS / DEVICE / PERSONAL /
 * SYSTEM). Wi-Fi and Bluetooth rows carry a real inline {@link android.widget.Switch}
 * wired to {@link SettingsHelper}; every other row opens its sub-panel. This is
 * a plain list built on {@link PrefAdapter}, so it never touches the deprecated
 * header machinery that crashed on Android 16.
 */
public class HeadersFragment extends android.app.Fragment implements PrefAdapter.Callback {

    private ListView mList;
    private PrefAdapter mAdapter;
    private List<PrefItem> mItems;
    private PrefItem mWifiItem;
    private PrefItem mBtItem;
    private final Handler mRefreshDelayer = new Handler();

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)
                    || BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                refreshSwitches();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
        Activity a = getActivity();
        mList = new ListView(a);
        mList.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mList.setCacheColorHint(0);
        buildItems(a);
        mAdapter = new PrefAdapter(inflater, mItems);
        mAdapter.setCallback(this);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                PrefItem item = mItems.get(pos);
                if (item.type == PrefItem.TYPE_CATEGORY) return;
                if (item.panelKey != null) {
                    ((OnPanelSelectedListener) getActivity()).onPanelSelected(item.panelKey);
                } else if (item.action != null) {
                    item.action.run();
                } else if (item.intent != null) {
                    startActivity(item.intent);
                }
            }
        });
        return mList;
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity a = getActivity();
        if (a != null) {
            IntentFilter f = new IntentFilter();
            f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            a.registerReceiver(mStateReceiver, f);
        }
        refreshSwitches();
    }

    @Override
    public void onPause() {
        Activity a = getActivity();
        if (a != null) {
            try { a.unregisterReceiver(mStateReceiver); } catch (Throwable ignored) {}
        }
        mRefreshDelayer.removeCallbacksAndMessages(null);
        super.onPause();
    }

    private PrefItem panelRow(int iconRes, String title, String key) {
        PrefItem it = PrefItem.row(iconRes, title, null);
        it.panelKey = key;
        return it;
    }

    private void buildItems(Activity a) {
        mItems = new ArrayList<>();

        mItems.add(PrefItem.category(a.getString(R.string.header_category_wireless_networks)));

        boolean wifiOn = SettingsHelper.isWifiEnabled(a);
        mWifiItem = PrefItem.switchRow(R.drawable.ic_settings_wireless,
                a.getString(R.string.wifi_settings_title),
                statusText(a, wifiOn, SettingsHelper.getConnectedSsid(a)),
                PrefItem.SWITCH_WIFI, wifiOn);
        mWifiItem.panelKey = "wifi";
        mItems.add(mWifiItem);

        boolean btOn = SettingsHelper.isBtEnabled(a);
        mBtItem = PrefItem.switchRow(R.drawable.ic_settings_bluetooth2,
                a.getString(R.string.bluetooth_settings_title),
                statusText(a, btOn, SettingsHelper.getBtName(a)),
                PrefItem.SWITCH_BT, btOn);
        mBtItem.panelKey = "bluetooth";
        mItems.add(mBtItem);

        mItems.add(panelRow(R.drawable.ic_settings_data_usage, a.getString(R.string.data_usage_summary_title), "data_usage"));
        mItems.add(panelRow(0, a.getString(R.string.radio_controls_title), "more"));

        mItems.add(PrefItem.category(a.getString(R.string.header_category_device)));
        mItems.add(panelRow(R.drawable.ic_settings_sound, a.getString(R.string.sound_settings), "sound"));
        mItems.add(panelRow(R.drawable.ic_settings_display, a.getString(R.string.display_settings), "display"));
        mItems.add(panelRow(R.drawable.ic_settings_storage, a.getString(R.string.storage_settings), "storage"));
        mItems.add(panelRow(R.drawable.ic_settings_battery, a.getString(R.string.power_usage_summary_title), "battery"));
        mItems.add(panelRow(R.drawable.ic_settings_applications, a.getString(R.string.applications_settings), "apps"));

        mItems.add(PrefItem.category(a.getString(R.string.header_category_personal)));
        mItems.add(panelRow(R.drawable.ic_settings_sync, a.getString(R.string.sync_settings), "accounts"));
        mItems.add(panelRow(R.drawable.ic_settings_location, a.getString(R.string.location_settings_title), "location"));
        mItems.add(panelRow(R.drawable.ic_settings_security, a.getString(R.string.security_settings_title), "security"));
        mItems.add(panelRow(R.drawable.ic_settings_language, a.getString(R.string.language_settings), "language_input"));
        mItems.add(panelRow(R.drawable.ic_settings_backup, a.getString(R.string.privacy_settings), "backup"));

        mItems.add(PrefItem.category(a.getString(R.string.header_category_system)));
        mItems.add(panelRow(R.drawable.ic_settings_dock, a.getString(R.string.dock_settings), "dock"));
        mItems.add(panelRow(R.drawable.ic_settings_date_time, a.getString(R.string.date_and_time_settings_title), "datetime"));
        mItems.add(panelRow(R.drawable.ic_settings_accessibility, a.getString(R.string.accessibility_settings), "accessibility"));
        mItems.add(panelRow(R.drawable.ic_settings_development, a.getString(R.string.development_settings_title), "developer"));
        // About phone is a self-contained, crash-proof fake screen that hosts the
        // ICS easter egg (the real panel=about path needs system reads that can
        // fail on modern Android, e.g. 16). Launch AboutActivity directly.
        PrefItem about = PrefItem.row(R.drawable.ic_settings_about,
                a.getString(R.string.about_settings), null);
        about.intent = new Intent(a, AboutActivity.class);
        mItems.add(about);
    }

    private static String statusText(Activity a, boolean on, String detail) {
        if (on) {
            String prefix = a.getString(R.string.state_on);
            return detail != null ? prefix + " · " + detail : prefix;
        }
        return a.getString(R.string.state_off);
    }

    /** Re-read Wi-Fi/Bluetooth state and refresh the two toggle rows. */
    public void refreshSwitches() {
        if (mItems == null || mAdapter == null) return;
        Activity a = getActivity();
        if (a == null) return;
        boolean wifiOn = SettingsHelper.isWifiEnabled(a);
        mWifiItem.checked = wifiOn;
        mWifiItem.summary = statusText(a, wifiOn, SettingsHelper.getConnectedSsid(a));
        boolean btOn = SettingsHelper.isBtEnabled(a);
        mBtItem.checked = btOn;
        mBtItem.summary = statusText(a, btOn, SettingsHelper.getBtName(a));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRowClick(PrefItem item) {
        // Row body clicks are handled by the ListView listener above.
    }

    @Override
    public void onSwitchToggle(PrefItem item, boolean checked) {
        Activity a = getActivity();
        if (a == null) return;
        boolean accepted;
        if (item.switchKind == PrefItem.SWITCH_WIFI) {
            accepted = SettingsHelper.setWifiEnabled(a, checked);
        } else if (item.switchKind == PrefItem.SWITCH_BT) {
            accepted = SettingsHelper.setBtEnabled(a, checked);
        } else {
            accepted = true;
        }
        if (!accepted) {
            if (item.switchKind == PrefItem.SWITCH_WIFI) {
                SettingsHelper.openSystemSettings(a, android.provider.Settings.ACTION_WIFI_SETTINGS);
            } else if (item.switchKind == PrefItem.SWITCH_BT) {
                SettingsHelper.openSystemSettings(a, android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            }
        }
        // The actual state arrives by broadcast; schedule one safety refresh in
        // case the broadcast is slow or suppressed on this platform.
        mRefreshDelayer.removeCallbacksAndMessages(null);
        mRefreshDelayer.postDelayed(new Runnable() {
            @Override
            public void run() { refreshSwitches(); }
        }, 1200);
    }
}
