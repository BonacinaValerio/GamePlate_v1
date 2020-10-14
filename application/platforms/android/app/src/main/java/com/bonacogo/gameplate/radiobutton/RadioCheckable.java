package com.bonacogo.gameplate.radiobutton;

import android.view.View;
import android.widget.Checkable;

public interface RadioCheckable extends Checkable {
    void addOnCheckChangeListener(OnCheckedChangeListener onCheckedChangeListener);
    void removeOnCheckChangeListener(OnCheckedChangeListener onCheckedChangeListener);

    interface OnCheckedChangeListener {
        void onCheckedChanged(View buttonView, boolean isChecked);
    }
}
