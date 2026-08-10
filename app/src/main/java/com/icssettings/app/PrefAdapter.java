package com.icssettings.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

/**
 * Renders a {@link PrefItem} list using local Holo-styled row layouts
 * (res/layout/pref_row.xml, pref_category.xml) so the result is
 * indistinguishable from real ICS Settings on any Android version and never
 * depends on hidden framework preference layouts. Toggle rows get an inline
 * {@link Switch} dropped into the widget frame.
 */
public class PrefAdapter extends BaseAdapter {

    public interface Callback {
        /** Row body clicked (not the inline switch). */
        void onRowClick(PrefItem item);
        /** Inline switch toggled. */
        void onSwitchToggle(PrefItem item, boolean checked);
    }

    private final LayoutInflater mInf;
    private final List<PrefItem> mItems;
    private Callback mCb;

    public PrefAdapter(LayoutInflater inf, List<PrefItem> items) {
        mInf = inf;
        mItems = items;
    }

    public void setCallback(Callback cb) {
        mCb = cb;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public PrefItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        PrefItem item = mItems.get(position);
        if (item.type == PrefItem.TYPE_CATEGORY) return false;
        return item.enabled;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final PrefItem item = mItems.get(position);

        if (item.type == PrefItem.TYPE_CATEGORY) {
            View v = mInf.inflate(R.layout.pref_category, parent, false);
            TextView t = (TextView) v.findViewById(R.id.title);
            if (t != null) t.setText(item.title);
            return v;
        }

        View v = convertView;
        if (v == null || !(v.getTag() instanceof Integer) || (Integer) v.getTag() != 1) {
            v = mInf.inflate(R.layout.pref_row, parent, false);
            v.setTag(1);
        }

        TextView title = (TextView) v.findViewById(R.id.title);
        TextView summary = (TextView) v.findViewById(R.id.summary);
        ImageView icon = (ImageView) v.findViewById(R.id.icon);
        FrameLayout widget = (FrameLayout) v.findViewById(R.id.widget_frame);

        if (title != null) title.setText(item.title);

        if (summary != null) {
            if (item.summary != null && item.summary.length() > 0) {
                summary.setVisibility(View.VISIBLE);
                summary.setText(item.summary);
            } else {
                summary.setVisibility(View.GONE);
            }
        }

        if (icon != null) {
            if (item.iconRes != 0) {
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(item.iconRes);
            } else {
                icon.setVisibility(View.GONE);
            }
        }

        if (widget != null) {
            widget.removeAllViews();
            if (item.type == PrefItem.TYPE_SWITCH) {
                Switch sw = new Switch(mInf.getContext());
                sw.setChecked(item.checked);
                sw.setFocusable(false);
                sw.setFocusableInTouchMode(false);
                // The switch consumes its own touches, so ListView's onItemClick
                // will not fire for it; this handler drives the toggle.
                sw.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View vv) {
                        if (mCb != null) {
                            mCb.onSwitchToggle(item, ((Switch) vv).isChecked());
                        }
                    }
                });
                widget.addView(sw);
                widget.setVisibility(View.VISIBLE);
            } else {
                widget.setVisibility(View.GONE);
            }
        }

        if (v != null) v.setEnabled(item.enabled);
        return v;
    }
}
