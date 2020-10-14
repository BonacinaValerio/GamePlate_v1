package com.bonacogo.gameplate.outlineprovider;

import android.graphics.Outline;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;

public class CustomOutlineProvider extends ViewOutlineProvider {
    private int yShift;
    private float cornerRadius;
    private Rect rect;
    private float alpha;

    public CustomOutlineProvider(int yShift, float cornerRadius) {
        this.yShift = yShift;
        this.cornerRadius = cornerRadius;
        this.rect = new Rect();
        this.alpha = -1;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        view.getBackground().copyBounds(rect);
        rect.offset(0, yShift);
        if (alpha == -1)
            this.alpha = outline.getAlpha();
        else
            outline.setAlpha(alpha);
        outline.setRoundRect(rect, cornerRadius);
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

}
