package com.newsrob.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class CheckBoxPreference extends android.preference.CheckBoxPreference {

    public CheckBoxPreference(Context context) {
        super(context);
    }

    public CheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

    }

}
