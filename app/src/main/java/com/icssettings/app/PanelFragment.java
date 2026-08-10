package com.icssettings.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-driven renderer for every Settings sub-panel. Replaces the old
 * {@code PreferenceFragment}; nothing here depends on the deprecated preference
 * framework, so it runs unchanged on Android 4.0 through Android 16.
 *
 * <p>High-value panels (Wi-Fi, Bluetooth, Display, Sound, Battery, Storage,
 * About) read and write real system state via {@link SettingsHelper}. The rest
 * are faithful, "virtual" replicas (as requested) that mostly delegate to the
 * real system Settings panels through intents.
 */
public class PanelFragment extends Fragment implements PrefAdapter.Callback {

    private static final String ARG_KEY = "panel";

    private String mKey;
    private ListView mList;
    private PrefAdapter mAdapter;
    private List<PrefItem> mItems = new ArrayList<>();

    // Easter-egg tap counters (ICS behaviour: 7 taps on Android version / build).
    private int mVersionTaps = 0;
    private int mBuildTaps = 0;

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if ("wifi".equals(mKey)
                    && (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)
                    || WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action))) {
                rebuild();
            } else if ("bluetooth".equals(mKey)
                    && BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                rebuild();
            }
        }
    };

    public static PanelFragment newInstance(String key) {
        PanelFragment f = new PanelFragment();
        Bundle b = new Bundle();
        b.putString(ARG_KEY, key);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        if (getArguments() != null) mKey = getArguments().getString(ARG_KEY, "about");
        else mKey = "about";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
        final Context ctx = getActivity();
        if (getActivity() != null) getActivity().setTitle(titleFor(mKey));
        mList = new ListView(ctx);
        mList.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mList.setCacheColorHint(0);
        buildPanel();
        mAdapter = new PrefAdapter(inflater, mItems);
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
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getActivity(), R.string.toast_no_activity, Toast.LENGTH_SHORT).show();
                    }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if ("wifi".equals(mKey)
                        && a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                }
                if ("about".equals(mKey)
                        && a.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 101);
                }
            }
            IntentFilter f = new IntentFilter();
            if ("wifi".equals(mKey)) {
                f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                f.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            } else if ("bluetooth".equals(mKey)) {
                f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            }
            if (f.countActions() > 0) {
                a.registerReceiver(mStateReceiver, f);
            }
        }
        rebuild();
    }

    @Override
    public void onPause() {
        Activity a = getActivity();
        if (a != null) {
            try { a.unregisterReceiver(mStateReceiver); } catch (Throwable ignored) {}
        }
        super.onPause();
    }

    private void rebuild() {
        mItems.clear();
        buildPanel();
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    // ----------------------------------------------------------- builders
    private void buildPanel() {
        Context ctx = getActivity();
        if (ctx == null) return;
        switch (mKey) {
            case "wifi":        buildWifi(ctx); break;
            case "bluetooth":   buildBluetooth(ctx); break;
            case "display":     buildDisplay(ctx); break;
            case "sound":       buildSound(ctx); break;
            case "battery":     buildBattery(ctx); break;
            case "storage":     buildStorage(ctx); break;
            case "about":       buildAbout(ctx); break;
            case "data_usage":  buildDataUsage(ctx); break;
            case "more":        buildMore(ctx); break;
            case "apps":        buildApps(ctx); break;
            case "accounts":    buildAccounts(ctx); break;
            case "location":    buildLocation(ctx); break;
            case "security":    buildSecurity(ctx); break;
            case "language_input": buildLanguage(ctx); break;
            case "backup":      buildBackup(ctx); break;
            case "dock":        buildDock(ctx); break;
            case "datetime":    buildDateTime(ctx); break;
            case "accessibility": buildAccessibility(ctx); break;
            case "developer":   buildDeveloper(ctx); break;
            default:            buildAbout(ctx); break;
        }
    }

    private PrefItem withIntent(PrefItem it, String action) {
        it.intent = new Intent(action);
        return it;
    }

    private PrefItem withAction(PrefItem it, Runnable r) {
        it.action = r;
        return it;
    }

    private void buildWifi(Context ctx) {
        boolean on = SettingsHelper.isWifiEnabled(ctx);
        PrefItem sw = PrefItem.switchRow(0, ctx.getString(R.string.wifi_settings_title),
                ctx.getString(on ? R.string.state_on : R.string.state_off),
                PrefItem.SWITCH_NONE, on);
        sw.key = "wifi_on";
        mItems.add(sw);

        mItems.add(PrefItem.category(ctx.getString(R.string.p_wifi_networks)));
        String connected = SettingsHelper.getConnectedSsid(ctx);
        List<ScanResult> nets = SettingsHelper.getWifiNetworks(ctx);
        if (!on) {
            mItems.add(PrefItem.row(ctx.getString(R.string.wifi_is_off),
                    ctx.getString(R.string.wifi_turn_on_hint)));
        } else if (nets.isEmpty()) {
            mItems.add(PrefItem.row(ctx.getString(R.string.wifi_scanning),
                    ctx.getString(R.string.wifi_no_networks_yet)));
        } else {
            for (ScanResult r : nets) {
                String ssid = r.SSID;
                if (ssid == null || ssid.isEmpty()) ssid = ctx.getString(R.string.hidden_network);
                boolean secured = (r.capabilities != null)
                        && (r.capabilities.contains("WPA") || r.capabilities.contains("WEP")
                        || r.capabilities.contains("EAP") || r.capabilities.contains("WPS"));
                int iconRes = signalIconRes(r.level, secured);
                String lvl = signalText(ctx, r.level);
                String sec = ctx.getString(secured ? R.string.state_secured : R.string.state_open);
                StringBuilder sum = new StringBuilder();
                if (ssid.equals(connected)) {
                    sum.append(ctx.getString(R.string.state_connected)).append(" · ");
                }
                sum.append(sec).append(" · ").append(lvl);
                mItems.add(PrefItem.row(iconRes, ssid, sum.toString()));
            }
        }
        mItems.add(PrefItem.row(ctx.getString(R.string.wifi_add_network), null));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.wifi_saved_networks), null), Settings.ACTION_WIFI_SETTINGS));
        mItems.add(PrefItem.row(ctx.getString(R.string.wifi_advanced), null));
    }

    private void buildBluetooth(Context ctx) {
        boolean on = SettingsHelper.isBtEnabled(ctx);
        PrefItem sw = PrefItem.switchRow(0, ctx.getString(R.string.bluetooth_settings_title),
                ctx.getString(on ? R.string.state_on : R.string.state_off),
                PrefItem.SWITCH_NONE, on);
        sw.key = "bt_on";
        mItems.add(sw);

        mItems.add(PrefItem.category(ctx.getString(R.string.p_bt_device)));
        String name = SettingsHelper.getBtName(ctx);
        mItems.add(PrefItem.row(ctx.getString(R.string.p_bt_device_name),
                name != null ? name : ctx.getString(R.string.state_unknown)));

        mItems.add(PrefItem.category(ctx.getString(R.string.p_bt_paired)));
        List<String> paired = SettingsHelper.getPairedDevices(ctx);
        String pairedLabel = ctx.getString(R.string.state_paired);
        if (paired.isEmpty()) {
            mItems.add(PrefItem.row(ctx.getString(R.string.bt_none_paired), null));
        } else {
            for (String d : paired) mItems.add(PrefItem.row(d, pairedLabel));
        }

        mItems.add(PrefItem.category(ctx.getString(R.string.p_bt_available)));
        mItems.add(PrefItem.row(ctx.getString(R.string.bt_search_devices), null));
        mItems.add(PrefItem.row(ctx.getString(R.string.bt_rename_device), null));
        mItems.add(PrefItem.row(ctx.getString(R.string.bt_visibility_timeout), null));
    }

    private void buildDisplay(Context ctx) {
        boolean ab = SettingsHelper.isAutoBrightness(ctx);
        PrefItem auto = PrefItem.switchRow(0, ctx.getString(R.string.p_auto_bright), null,
                PrefItem.SWITCH_NONE, ab);
        auto.key = "auto_bright";
        mItems.add(auto);

        int bri = SettingsHelper.getBrightness(ctx);
        PrefItem brightness = PrefItem.row(ctx.getString(R.string.p_brightness),
                ab ? ctx.getString(R.string.state_automatic) : bri + " / 255");
        brightness.action = new Runnable() {
            @Override
            public void run() {
                showBrightnessDialog();
            }
        };
        mItems.add(brightness);

        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_wallpaper), null),
                Intent.ACTION_SET_WALLPAPER));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_sleep),
                sleepText(ctx, SettingsHelper.getScreenOffTimeout(ctx))), Settings.ACTION_DISPLAY_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_font_size),
                ctx.getString(R.string.font_size_normal)), Settings.ACTION_DISPLAY_SETTINGS));

        boolean rot = SettingsHelper.isAutoRotate(ctx);
        PrefItem rotate = PrefItem.switchRow(0, ctx.getString(R.string.p_auto_rotate), null,
                PrefItem.SWITCH_NONE, rot);
        rotate.key = "auto_rotate";
        mItems.add(rotate);
    }

    private void buildSound(Context ctx) {
        mItems.add(PrefItem.category(ctx.getString(R.string.p_volumes)));
        addVolumeRow(ctx, ctx.getString(R.string.p_ring_vol), AudioManager.STREAM_RING);
        addVolumeRow(ctx, ctx.getString(R.string.p_media_vol), AudioManager.STREAM_MUSIC);
        addVolumeRow(ctx, ctx.getString(R.string.p_alarm_vol), AudioManager.STREAM_ALARM);
        addVolumeRow(ctx, ctx.getString(R.string.p_incall_vol), AudioManager.STREAM_VOICE_CALL);

        mItems.add(PrefItem.category(ctx.getString(R.string.p_feedback)));
        PrefItem vb = PrefItem.switchRow(0, ctx.getString(R.string.p_vibrate), null,
                PrefItem.SWITCH_NONE, true);
        vb.key = "vibrate";
        mItems.add(vb);
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_default_ringtone),
                ctx.getString(R.string.v_ringtone)), Settings.ACTION_SOUND_SETTINGS));
    }

    private void addVolumeRow(Context ctx, String label, final int stream) {
        int vol = SettingsHelper.getStreamVolume(ctx, stream);
        int max = SettingsHelper.getStreamMaxVolume(ctx, stream);
        PrefItem it = PrefItem.row(label, vol + " / " + max);
        final Context c = ctx;
        it.action = new Runnable() {
            @Override
            public void run() {
                showVolumeDialog(c, label, stream);
            }
        };
        mItems.add(it);
    }

    private void buildBattery(Context ctx) {
        int lvl = SettingsHelper.getBatteryLevel(ctx);
        boolean charging = SettingsHelper.isCharging(ctx);
        mItems.add(PrefItem.category(ctx.getString(R.string.p_battery)));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_level), lvl + "%"));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_status),
                charging ? ctx.getString(R.string.p_charging) : ctx.getString(R.string.state_discharging)));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_screen), ctx.getString(R.string.v_screen_pct)));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_android_system), ctx.getString(R.string.v_sys_pct)));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.battery_use), null),
                Settings.ACTION_BATTERY_SAVER_SETTINGS));
    }

    private void buildStorage(Context ctx) {
        long[] internal = SettingsHelper.getStorage(ctx);
        long[] external = SettingsHelper.getExternalStorage(ctx);
        mItems.add(PrefItem.category(ctx.getString(R.string.p_internal_storage)));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_total), human(internal[0])));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_available), human(internal[1])));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_apps), human(internal[0] - internal[1])));
        mItems.add(PrefItem.category(ctx.getString(R.string.p_sd_card)));
        if (external[0] > 0) {
            mItems.add(PrefItem.row(ctx.getString(R.string.p_total), human(external[0])));
            mItems.add(PrefItem.row(ctx.getString(R.string.p_available), human(external[1])));
        } else {
            mItems.add(PrefItem.row(ctx.getString(R.string.p_sd_none), null));
        }
    }

    private void buildAbout(Context ctx) {
        mItems.add(PrefItem.category(ctx.getString(R.string.p_phone)));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_model), Build.MODEL));

        PrefItem ver = PrefItem.row(ctx.getString(R.string.p_android_version),
                Build.VERSION.RELEASE + " " + ctx.getString(R.string.about_version_suffix));
        ver.action = new Runnable() {
            @Override
            public void run() {
                onVersionTap();
            }
        };
        mItems.add(ver);

        mItems.add(PrefItem.row(ctx.getString(R.string.p_baseband), safeRadio(ctx)));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_kernel),
                SettingsHelper.getKernelVersion() + " " + ctx.getString(R.string.about_kernel_suffix)));
        PrefItem build = PrefItem.row(ctx.getString(R.string.p_build), Build.DISPLAY);
        build.action = new Runnable() {
            @Override
            public void run() {
                onBuildTap();
            }
        };
        mItems.add(build);

        mItems.add(PrefItem.category(ctx.getString(R.string.p_status_cat)));
        int lvl = SettingsHelper.getBatteryLevel(ctx);
        mItems.add(PrefItem.row(ctx.getString(R.string.p_battery_level), lvl + "%"));
        String imei = SettingsHelper.getImei(ctx);
        mItems.add(PrefItem.row(ctx.getString(R.string.p_imei),
                imei != null ? imei : ctx.getString(R.string.about_imei_unavailable)));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_ip),
                SettingsHelper.getIpAddress() != null ? SettingsHelper.getIpAddress()
                        : ctx.getString(R.string.about_ip_none)));

        mItems.add(PrefItem.category(ctx.getString(R.string.p_legal)));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_licenses), null));
        mItems.add(PrefItem.row(ctx.getString(R.string.p_demo_ui), ctx.getString(R.string.v_demo_value)));
    }

    // ---- virtual panels (delegate to real system Settings where sensible) ----
    private void buildDataUsage(Context ctx) {
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_mobile_data),
                ctx.getString(R.string.state_on)), Settings.ACTION_DATA_ROAMING_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_mobile_usage),
                ctx.getString(R.string.v_mobile_use)), Settings.ACTION_DATA_USAGE_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_wifi_usage),
                ctx.getString(R.string.v_wifi_use)), Settings.ACTION_DATA_USAGE_SETTINGS));
    }

    private void buildMore(Context ctx) {
        boolean airplane = Settings.Global.getInt(ctx.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        PrefItem ap = PrefItem.switchRow(0, ctx.getString(R.string.p_airplane), null,
                PrefItem.SWITCH_NONE, airplane);
        ap.key = "airplane";
        mItems.add(ap);
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_vpn), ctx.getString(R.string.p_vpn_none)),
                Settings.ACTION_VPN_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_mobile_networks), null),
                Settings.ACTION_DATA_ROAMING_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.radio_controls_title), null),
                Settings.ACTION_WIRELESS_SETTINGS));
    }

    private void buildApps(Context ctx) {
        mItems.add(PrefItem.category(ctx.getString(R.string.p_downloaded)));
        mItems.add(PrefItem.row(ctx.getString(R.string.v_wechat), "45 MB"));
        mItems.add(PrefItem.row(ctx.getString(R.string.v_alipay), "38 MB"));
        mItems.add(PrefItem.row(ctx.getString(R.string.v_thisapp), "12 MB"));
        mItems.add(PrefItem.category(ctx.getString(R.string.p_running)));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.v_android_sys), "82 MB"),
                Settings.ACTION_APPLICATION_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.v_launcher), "24 MB"),
                Settings.ACTION_APPLICATION_SETTINGS));
    }

    private void buildAccounts(Context ctx) {
        mItems.add(PrefItem.category(ctx.getString(R.string.p_accounts_cat)));
        mItems.add(withIntent(PrefItem.row("Google", ctx.getString(R.string.state_synced)),
                Settings.ACTION_SYNC_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_add_account), null),
                Settings.ACTION_ADD_ACCOUNT));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_bg_data),
                ctx.getString(R.string.state_on)), Settings.ACTION_SYNC_SETTINGS));
    }

    private void buildLocation(Context ctx) {
        PrefItem acc = PrefItem.switchRow(0, ctx.getString(R.string.p_access_location), null,
                PrefItem.SWITCH_NONE, true);
        acc.key = "location";
        mItems.add(acc);
        mItems.add(PrefItem.category(ctx.getString(R.string.p_location_mode)));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_high_acc), null),
                Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_battery_saving), null),
                Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_device_only), null),
                Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void buildSecurity(Context ctx) {
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_screen_lock), ctx.getString(R.string.p_swipe)),
                Settings.ACTION_SECURITY_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_owner_info), ctx.getString(R.string.p_none_set)),
                Settings.ACTION_SECURITY_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_device_admins), ctx.getString(R.string.p_none)),
                Settings.ACTION_SECURITY_SETTINGS));
        PrefItem unk = PrefItem.switchRow(0, ctx.getString(R.string.p_unknown_sources), null,
                PrefItem.SWITCH_NONE, false);
        unk.key = "unknown_sources";
        mItems.add(unk);
    }

    private void buildLanguage(Context ctx) {
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_current_keyboard),
                ctx.getString(R.string.p_android_keyboard)), Settings.ACTION_INPUT_METHOD_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.language_settings),
                ctx.getString(R.string.v_lang_zh)), Settings.ACTION_LOCALE_SETTINGS));
    }

    private void buildBackup(Context ctx) {
        PrefItem bk = PrefItem.switchRow(0, ctx.getString(R.string.p_backup_data), null,
                PrefItem.SWITCH_NONE, true);
        bk.key = "backup_data";
        mItems.add(bk);
        PrefItem ar = PrefItem.switchRow(0, ctx.getString(R.string.p_auto_restore), null,
                PrefItem.SWITCH_NONE, true);
        ar.key = "auto_restore";
        mItems.add(ar);
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_factory_reset),
                ctx.getString(R.string.p_erase_all)), Settings.ACTION_PRIVACY_SETTINGS));
    }

    private void buildDock(Context ctx) {
        PrefItem a = PrefItem.switchRow(0, ctx.getString(R.string.p_auto_dock), null,
                PrefItem.SWITCH_NONE, false);
        a.key = "auto_dock";
        mItems.add(a);
        PrefItem d = PrefItem.switchRow(0, ctx.getString(R.string.p_audio_dock), null,
                PrefItem.SWITCH_NONE, false);
        d.key = "audio_dock";
        mItems.add(d);
    }

    private void buildDateTime(Context ctx) {
        PrefItem at = PrefItem.switchRow(0, ctx.getString(R.string.p_auto_datetime), null,
                PrefItem.SWITCH_NONE, true);
        at.key = "auto_datetime";
        mItems.add(at);
        PrefItem az = PrefItem.switchRow(0, ctx.getString(R.string.p_auto_timezone), null,
                PrefItem.SWITCH_NONE, true);
        az.key = "auto_timezone";
        mItems.add(az);
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_set_date), ctx.getString(R.string.v_date)),
                Settings.ACTION_DATE_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_set_time), ctx.getString(R.string.v_time)),
                Settings.ACTION_DATE_SETTINGS));
        PrefItem h24 = PrefItem.switchRow(0, ctx.getString(R.string.p_24h), null,
                PrefItem.SWITCH_NONE, true);
        h24.key = "use_24h";
        mItems.add(h24);
    }

    private void buildAccessibility(Context ctx) {
        PrefItem tb = PrefItem.switchRow(0, ctx.getString(R.string.p_talkback), null,
                PrefItem.SWITCH_NONE, false);
        tb.key = "talkback";
        mItems.add(tb);
        PrefItem mg = PrefItem.switchRow(0, ctx.getString(R.string.p_magnify), null,
                PrefItem.SWITCH_NONE, false);
        mg.key = "magnify";
        mItems.add(mg);
        PrefItem lt = PrefItem.switchRow(0, ctx.getString(R.string.p_large_text), null,
                PrefItem.SWITCH_NONE, false);
        lt.key = "large_text";
        mItems.add(lt);
        PrefItem hc = PrefItem.switchRow(0, ctx.getString(R.string.p_high_contrast), null,
                PrefItem.SWITCH_NONE, false);
        hc.key = "high_contrast";
        mItems.add(hc);
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_power_end), null),
                Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void buildDeveloper(Context ctx) {
        PrefItem usb = PrefItem.switchRow(0, ctx.getString(R.string.p_usb_debug), null,
                PrefItem.SWITCH_NONE, false);
        usb.key = "usb_debug";
        mItems.add(usb);
        PrefItem aw = PrefItem.switchRow(0, ctx.getString(R.string.p_stay_awake), null,
                PrefItem.SWITCH_NONE, false);
        aw.key = "stay_awake";
        mItems.add(aw);
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_window_anim),
                ctx.getString(R.string.anim_scale_1x)),
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        mItems.add(withIntent(PrefItem.row(ctx.getString(R.string.p_transition_anim),
                ctx.getString(R.string.anim_scale_1x)),
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
    }

    // ----------------------------------------------------- helpers / dialogs
    private void showVolumeDialog(final Context ctx, String label, final int stream) {
        int vol = SettingsHelper.getStreamVolume(ctx, stream);
        int max = Math.max(1, SettingsHelper.getStreamMaxVolume(ctx, stream));
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(label);
        SeekBar sb = new SeekBar(ctx);
        sb.setMax(max);
        sb.setProgress(vol);
        final TextView val = new TextView(ctx);
        val.setText(vol + " / " + max);
        val.setPadding(40, 20, 40, 20);
        LinearLayout lay = new LinearLayout(ctx);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(40, 20, 40, 20);
        lay.addView(val);
        lay.addView(sb);
        b.setView(lay);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                val.setText(p + " / " + s.getMax());
                if (fromUser) SettingsHelper.setStreamVolume(ctx, stream, p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        b.setPositiveButton(R.string.dialog_ok, null);
        b.show();
        rebuild();
    }

    private void showBrightnessDialog() {
        final Context ctx = getActivity();
        if (ctx == null) return;
        if (!SettingsHelper.canWriteSystemSettings(ctx)) {
            SettingsHelper.requestWriteSettings((Activity) ctx);
            Toast.makeText(ctx, R.string.toast_grant_write_settings, Toast.LENGTH_LONG).show();
            return;
        }
        int bri = SettingsHelper.getBrightness(ctx);
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(ctx.getString(R.string.p_brightness));
        SeekBar sb = new SeekBar(ctx);
        sb.setMax(255);
        sb.setProgress(bri);
        final TextView val = new TextView(ctx);
        val.setText(bri + " / 255");
        val.setPadding(40, 20, 40, 20);
        LinearLayout lay = new LinearLayout(ctx);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(40, 20, 40, 20);
        lay.addView(val);
        lay.addView(sb);
        b.setView(lay);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                val.setText(p + " / 255");
                if (fromUser) SettingsHelper.setBrightness(ctx, p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        b.setPositiveButton(R.string.dialog_ok, null);
        b.show();
        rebuild();
    }

    // ----------------------------------------------------------- easter egg
    private void onVersionTap() {
        mVersionTaps++;
        if (mVersionTaps >= 7) {
            mVersionTaps = 0;
            try {
                startActivity(new Intent(getActivity(), EasterEggActivity.class));
            } catch (Throwable t) {
                Toast.makeText(getActivity(), R.string.toast_easter_egg, Toast.LENGTH_SHORT).show();
            }
        } else if (mVersionTaps >= 3) {
            Toast.makeText(getActivity(),
                    getString(R.string.about_tap_remaining,
                            Build.VERSION.RELEASE, 7 - mVersionTaps),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onBuildTap() {
        mBuildTaps++;
        if (mBuildTaps >= 7) {
            mBuildTaps = 0;
            Toast.makeText(getActivity(), R.string.dev_options_enabled, Toast.LENGTH_SHORT).show();
        }
    }

    // ----------------------------------------------------- switch handling
    @Override
    public void onSwitchToggle(PrefItem item, boolean checked) {
        Context ctx = getActivity();
        if (ctx == null) return;
        String k = item.key;
        if (k == null) { item.checked = checked; return; }
        switch (k) {
            case "wifi_on": {
                boolean ok = SettingsHelper.setWifiEnabled(ctx, checked);
                if (!ok) {
                    SettingsHelper.openSystemSettings((Activity) ctx,
                            android.provider.Settings.ACTION_WIFI_SETTINGS);
                }
                break;
            }
            case "bt_on": {
                boolean ok = SettingsHelper.setBtEnabled(ctx, checked);
                if (!ok) {
                    SettingsHelper.openSystemSettings((Activity) ctx,
                            android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                }
                break;
            }
            case "auto_bright":
                SettingsHelper.setAutoBrightness(ctx, checked);
                break;
            case "auto_rotate":
                SettingsHelper.setAutoRotate(ctx, checked);
                break;
            case "vibrate": {
                AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) am.setRingerMode(checked ? AudioManager.RINGER_MODE_VIBRATE
                        : AudioManager.RINGER_MODE_NORMAL);
                break;
            }
            case "airplane": {
                try {
                    Settings.Global.putInt(ctx.getContentResolver(),
                            Settings.Global.AIRPLANE_MODE_ON, checked ? 1 : 0);
                } catch (Throwable ignored) {}
                break;
            }
            default:
                item.checked = checked;
                break;
        }
        rebuild();
    }

    @Override
    public void onRowClick(PrefItem item) {
        // Handled by the ListView onItemClick listener.
    }

    // ----------------------------------------------------------- utilities
    private String titleFor(String key) {
        Context ctx = getActivity();
        if (ctx == null) return getString(R.string.settings_title);
        switch (key) {
            case "wifi": return ctx.getString(R.string.wifi_settings_title);
            case "bluetooth": return ctx.getString(R.string.bluetooth_settings_title);
            case "display": return ctx.getString(R.string.display_settings);
            case "sound": return ctx.getString(R.string.sound_settings);
            case "storage": return ctx.getString(R.string.storage_settings);
            case "battery": return ctx.getString(R.string.power_usage_summary_title);
            case "about": return ctx.getString(R.string.about_settings);
            case "data_usage": return ctx.getString(R.string.data_usage_summary_title);
            case "more": return ctx.getString(R.string.radio_controls_title);
            case "apps": return ctx.getString(R.string.applications_settings);
            case "accounts": return ctx.getString(R.string.sync_settings);
            case "location": return ctx.getString(R.string.location_settings_title);
            case "security": return ctx.getString(R.string.security_settings_title);
            case "language_input": return ctx.getString(R.string.language_settings);
            case "backup": return ctx.getString(R.string.privacy_settings);
            case "dock": return ctx.getString(R.string.dock_settings);
            case "datetime": return ctx.getString(R.string.date_and_time_settings_title);
            case "accessibility": return ctx.getString(R.string.accessibility_settings);
            case "developer": return ctx.getString(R.string.development_settings_title);
            default: return ctx.getString(R.string.settings_title);
        }
    }

    private static int signalIndex(int level) {
        if (level >= -50) return 4;
        if (level >= -60) return 3;
        if (level >= -70) return 2;
        return 1;
    }

    private static int signalIconRes(int level, boolean secured) {
        int idx = signalIndex(level);
        if (secured) {
            switch (idx) {
                case 1: return R.drawable.ic_wifi_lock_signal_1;
                case 2: return R.drawable.ic_wifi_lock_signal_2;
                case 3: return R.drawable.ic_wifi_lock_signal_3;
                default: return R.drawable.ic_wifi_lock_signal_4;
            }
        } else {
            switch (idx) {
                case 1: return R.drawable.ic_wifi_signal_1;
                case 2: return R.drawable.ic_wifi_signal_2;
                case 3: return R.drawable.ic_wifi_signal_3;
                default: return R.drawable.ic_wifi_signal_4;
            }
        }
    }

    private static String signalText(Context ctx, int level) {
        if (level >= -50) return ctx.getString(R.string.sig_excellent);
        if (level >= -60) return ctx.getString(R.string.sig_good);
        if (level >= -70) return ctx.getString(R.string.sig_fair);
        return ctx.getString(R.string.sig_poor);
    }

    private static String sleepText(Context ctx, int ms) {
        String[] values = ctx.getResources().getStringArray(R.array.sleep_values);
        String[] entries = ctx.getResources().getStringArray(R.array.sleep_entries);
        String target = String.valueOf(ms);
        for (int i = 0; i < values.length; i++) {
            if (target.equals(values[i])) return entries[i];
        }
        return ctx.getString(R.string.sleep_seconds_fallback, ms / 1000);
    }

    private static String human(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] u = {"B", "KB", "MB", "GB", "TB"};
        int i = (int) (Math.log(bytes) / Math.log(1024));
        if (i < 0) i = 0;
        if (i > u.length - 1) i = u.length - 1;
        return String.format("%.1f %s", bytes / Math.pow(1024, i), u[i]);
    }

    private static String safeRadio(Context ctx) {
        try {
            return Build.getRadioVersion();
        } catch (Throwable t) {
            return ctx.getString(R.string.state_unknown);
        }
    }
}
