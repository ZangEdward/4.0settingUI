package com.icssettings.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Crash-proof bridge to real Android settings. Every method is version-guarded
 * and wrapped so a missing permission or a disabled API can never throw — on
 * Android 16 several legacy toggles (Wi-Fi/Bluetooth enable, brightness write)
 * are restricted for normal apps, in which case we report the current state and
 * let the UI fall back to launching the real system Settings panel.
 */
public final class SettingsHelper {

    private SettingsHelper() {}

    // ---------------------------------------------------------------- Wi-Fi
    public static WifiManager getWifiManager(Context c) {
        return (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static boolean isWifiEnabled(Context c) {
        try {
            WifiManager w = getWifiManager(c);
            return w != null && w.isWifiEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Attempt to toggle Wi-Fi. Returns the resulting enabled state. */
    public static boolean setWifiEnabled(Context c, boolean on) {
        try {
            WifiManager w = getWifiManager(c);
            if (w == null) return false;
            w.setWifiEnabled(on);
            return w.isWifiEnabled();
        } catch (Throwable t) {
            return isWifiEnabled(c);
        }
    }

    public static String getConnectedSsid(Context c) {
        try {
            WifiManager w = getWifiManager(c);
            if (w == null || !w.isWifiEnabled()) return null;
            WifiInfo info = w.getConnectionInfo();
            if (info == null) return null;
            String ssid = info.getSSID();
            if (ssid == null || ssid.equals("0x") || ssid.equals("<unknown ssid>")) return null;
            return stripQuotes(ssid);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Visible scan results (needs location permission on API 29+). */
    public static List<ScanResult> getWifiNetworks(Context c) {
        try {
            WifiManager w = getWifiManager(c);
            if (w == null) return new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Without location, SSIDs are redacted; still return the (empty) list.
                return new ArrayList<>(w.getScanResults());
            }
            return new ArrayList<>(w.getScanResults());
        } catch (Throwable t) {
            return new ArrayList<>();
        }
    }

    public static List<String> getConfiguredSsids(Context c) {
        List<String> out = new ArrayList<>();
        try {
            WifiManager w = getWifiManager(c);
            if (w == null) return out;
            for (WifiConfiguration cfg : w.getConfiguredNetworks()) {
                if (cfg != null && cfg.SSID != null) out.add(stripQuotes(cfg.SSID));
            }
        } catch (Throwable t) {
            // ignored
        }
        return out;
    }

    // ------------------------------------------------------------ Bluetooth
    public static BluetoothAdapter getBtAdapter(Context c) {
        try {
            return BluetoothAdapter.getDefaultAdapter();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isBtEnabled(Context c) {
        try {
            BluetoothAdapter a = getBtAdapter(c);
            return a != null && a.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean setBtEnabled(Context c, boolean on) {
        try {
            BluetoothAdapter a = getBtAdapter(c);
            if (a == null) return false;
            if (on) {
                // enable() is restricted on API 31+ for non-system apps.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) a.enable();
                else a.enable();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) a.disable();
                else a.disable();
            }
            return a.isEnabled();
        } catch (Throwable t) {
            return isBtEnabled(c);
        }
    }

    public static String getBtName(Context c) {
        try {
            BluetoothAdapter a = getBtAdapter(c);
            return a != null ? a.getName() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static List<String> getPairedDevices(Context c) {
        List<String> out = new ArrayList<>();
        try {
            BluetoothAdapter a = getBtAdapter(c);
            if (a == null) return out;
            Set<BluetoothDevice> bonded = a.getBondedDevices();
            if (bonded != null) {
                for (BluetoothDevice d : bonded) {
                    out.add(d.getName() != null ? d.getName() : d.getAddress());
                }
            }
        } catch (Throwable t) {
            // ignored
        }
        return out;
    }

    // ------------------------------------------------------------- Display
    public static boolean canWriteSystemSettings(Context c) {
        try {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Settings.System.canWrite(c);
        } catch (Throwable t) {
            return false;
        }
    }

    public static int getBrightness(Context c) {
        try {
            return Settings.System.getInt(c.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Throwable t) {
            return 128;
        }
    }

    public static void setBrightness(Context c, int value) {
        try {
            if (!canWriteSystemSettings(c)) return;
            Settings.System.putInt(c.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, clamp(value, 0, 255));
        } catch (Throwable t) {
            // ignored
        }
    }

    public static boolean isAutoBrightness(Context c) {
        try {
            int mode = Settings.System.getInt(c.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            return mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setAutoBrightness(Context c, boolean on) {
        try {
            if (!canWriteSystemSettings(c)) return;
            Settings.System.putInt(c.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    on ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                       : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        } catch (Throwable t) {
            // ignored
        }
    }

    public static boolean isAutoRotate(Context c) {
        try {
            return Settings.System.getInt(c.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setAutoRotate(Context c, boolean on) {
        try {
            if (!canWriteSystemSettings(c)) return;
            Settings.System.putInt(c.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, on ? 1 : 0);
        } catch (Throwable t) {
            // ignored
        }
    }

    public static int getScreenOffTimeout(Context c) {
        try {
            return Settings.System.getInt(c.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Throwable t) {
            return 60000;
        }
    }

    public static void setScreenOffTimeout(Context c, int ms) {
        try {
            if (!canWriteSystemSettings(c)) return;
            Settings.System.putInt(c.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, ms);
        } catch (Throwable t) {
            // ignored
        }
    }

    // --------------------------------------------------------------- Audio
    public static int getStreamVolume(Context c, int stream) {
        try {
            android.media.AudioManager am = (android.media.AudioManager)
                    c.getSystemService(Context.AUDIO_SERVICE);
            return am.getStreamVolume(stream);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getStreamMaxVolume(Context c, int stream) {
        try {
            android.media.AudioManager am = (android.media.AudioManager)
                    c.getSystemService(Context.AUDIO_SERVICE);
            return am.getStreamMaxVolume(stream);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void setStreamVolume(Context c, int stream, int val) {
        try {
            android.media.AudioManager am = (android.media.AudioManager)
                    c.getSystemService(Context.AUDIO_SERVICE);
            am.setStreamVolume(stream, clamp(val, 0, am.getStreamMaxVolume(stream)), 0);
        } catch (Throwable t) {
            // ignored
        }
    }

    // -------------------------------------------------------------- Battery
    public static int getBatteryLevel(Context c) {
        try {
            Intent i = c.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return -1;
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level < 0 || scale <= 0) return -1;
            return (level * 100) / scale;
        } catch (Throwable t) {
            return -1;
        }
    }

    public static boolean isCharging(Context c) {
        try {
            Intent i = c.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return false;
            int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        } catch (Throwable t) {
            return false;
        }
    }

    // -------------------------------------------------------------- Storage
    public static long[] getStorage(Context c) {
        // returns {totalBytes, availableBytes} for internal data partition
        long total = 0, avail = 0;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                android.os.StatFs sf = new android.os.StatFs(
                        Environment.getDataDirectory().getPath());
                long block = sf.getBlockSizeLong();
                total = sf.getBlockCountLong() * block;
                avail = sf.getAvailableBlocksLong() * block;
            } else {
                android.os.StatFs sf = new android.os.StatFs(
                        Environment.getDataDirectory().getPath());
                int block = sf.getBlockSize();
                total = (long) sf.getBlockCount() * block;
                avail = (long) sf.getAvailableBlocks() * block;
            }
        } catch (Throwable t) {
            // ignored
        }
        return new long[]{total, avail};
    }

    public static long[] getExternalStorage(Context c) {
        long total = 0, avail = 0;
        try {
            String path = Environment.getExternalStorageDirectory().getPath();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                android.os.StatFs sf = new android.os.StatFs(path);
                long block = sf.getBlockSizeLong();
                total = sf.getBlockCountLong() * block;
                avail = sf.getAvailableBlocksLong() * block;
            } else {
                android.os.StatFs sf = new android.os.StatFs(path);
                int block = sf.getBlockSize();
                total = (long) sf.getBlockCount() * block;
                avail = (long) sf.getAvailableBlocks() * block;
            }
        } catch (Throwable t) {
            // ignored
        }
        return new long[]{total, avail};
    }

    // ---------------------------------------------------------------- Misc
    public static String getKernelVersion() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/version"))) {
            return br.readLine();
        } catch (IOException t) {
            return "Unknown";
        }
    }

    public static String getIpAddress() {
        try {
            List<NetworkInterface> interfaces =
                    Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                for (java.net.InetAddress addr :
                        Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException t) {
            // ignored
        }
        return null;
    }

    public static String getImei(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && c.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            TelephonyManager tm = (TelephonyManager)
                    c.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) return null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return tm.getImei();
            }
            return tm.getDeviceId();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isNetworkConnected(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Throwable t) {
            return false;
        }
    }

    // ----------------------------------------------------------- Launchers
    public static void openSystemSettings(Activity a, String action) {
        try {
            a.startActivity(new Intent(action));
        } catch (Throwable t) {
            try {
                a.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable ignored) {
                // give up silently
            }
        }
    }

    public static void requestWriteSettings(Activity a) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !Settings.System.canWrite(a)) {
                a.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        .setData(android.net.Uri.parse("package:" + a.getPackageName())));
            }
        } catch (Throwable t) {
            // ignored
        }
    }

    // ------------------------------------------------------------ Helpers
    private static String stripQuotes(String s) {
        if (s == null) return null;
        if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        return s;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
