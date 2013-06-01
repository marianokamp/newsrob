package com.newsrob.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class ListPreference extends android.preference.ListPreference {
    private static int VALUE_ID = 999;

    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView tv = (TextView) view.findViewById(VALUE_ID);
        try {
            if (tv != null)
                tv.setText("«" + getEntry().toString().replace('\n', ' ') + "»");
        } catch (NullPointerException npe) {
            //
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View v = super.onCreateView(parent);

        ViewGroup vg = (ViewGroup) v.findViewById(android.R.id.summary).getParent();
        TextView tv = new TextView(vg.getContext());
        tv.setId(VALUE_ID);
        android.widget.RelativeLayout.LayoutParams rlp = new android.widget.RelativeLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.BELOW, android.R.id.summary);
        tv.setLayoutParams(rlp);
        vg.addView(tv);
        return v;
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        notifyChanged();

    }

}
