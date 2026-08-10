package com.icssettings.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Top-level Settings activity. Replaces the old {@code PreferenceActivity} +
 * {@code preference-headers} approach. On wide screens it shows a two-pane
 * layout (dashboard list + detail); on phones it shows the dashboard and opens
 * each panel in {@link PanelActivity}. All panel navigation goes through
 * {@link #onPanelSelected}, so the crash-prone framework auto-instantiation of
 * preference fragments is never used.
 */
public class MainActivity extends Activity implements OnPanelSelectedListener {

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mTwoPane = findViewById(R.id.detail_container) != null;
        if (mTwoPane) {
            // Pre-select the first real panel (Wi-Fi) for parity with ICS.
            onPanelSelected("wifi");
        }
    }

    @Override
    public void onPanelSelected(String panelKey) {
        if (panelKey == null) return;
        if (mTwoPane) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, PanelFragment.newInstance(panelKey))
                    .commit();
        } else {
            Intent i = new Intent(this, PanelActivity.class);
            i.putExtra("panel", panelKey);
            startActivity(i);
        }
    }
}
