package com.icssettings.app;

/**
 * Callback used by {@link HeadersFragment} to tell its host (either the
 * two-pane {@link MainActivity} or a single-pane host) which sub-panel should
 * be opened. Kept as a top-level interface so it can be referenced both by the
 * fragment and the activity without nesting issues.
 */
public interface OnPanelSelectedListener {
    void onPanelSelected(String panelKey);
}
