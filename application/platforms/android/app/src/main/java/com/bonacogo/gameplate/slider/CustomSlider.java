package com.bonacogo.gameplate.slider;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.bonacogo.gameplate.R;
import com.google.android.material.slider.Slider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomSlider extends Slider {
    // questa Slider sovvrascrive la Slider material con la modifica di non nascondere il cursore quando
    // l'utente alza il dito dall'oggetto

    public CustomSlider(@NonNull Context context) {
        this(context, null);
    }

    public CustomSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.sliderStyle);
    }

    public CustomSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP)
            return true;
        return super.onTouchEvent(event);
    }

    public void setFocus(boolean active, int index) {
        // calcolo il punto in cui si trova il valore della slider indicato da index
        float x = (this.getTrackWidth()/(this.getValueTo()-this.getValueFrom()))*(index-this.getValueFrom()) + this.getTrackSidePadding();
        if (active)
            onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, 0, 0));
        else {
            MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, 0, 0);
            super.onTouchEvent(event);
        }
    }

}
