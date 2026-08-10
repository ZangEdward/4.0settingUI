package com.icssettings.app;

import android.app.Activity;
import android.os.Bundle;

/**
 * Phone (single-pane) host for a settings sub-panel. The two-pane layout on
 * tablets uses {@link PanelFragment} directly inside {@link MainActivity}'s
 * detail container instead.
 */
public class PanelActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panel_host);
        String key = getIntent().getStringExtra("panel");
        if (key == null) key = "about";
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.panel_host, PanelFragment.newInstance(key))
                    .commit();
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
