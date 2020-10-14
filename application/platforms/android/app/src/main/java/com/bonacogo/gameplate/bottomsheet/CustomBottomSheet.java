package com.bonacogo.gameplate.bottomsheet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class CustomBottomSheet<V extends View> extends BottomSheetBehavior<V> {
    // questa CustomBottomSheet è una normale BottomSheetBehavior con l'aggiunta della possibilità di
    // bloccare lo scroll, attraverso l'override di tutti i metodi che lo gestiscono e evitando di eseguire
    // il codice che lo permette se mLocked è true

    private boolean mLocked = false;

    public CustomBottomSheet() {}

    public CustomBottomSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        boolean handled = false;

        if (!mLocked) {
            handled = super.onInterceptTouchEvent(parent, child, event);
        }

        return handled;
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        boolean handled = false;

        if (!mLocked) {
            handled = super.onTouchEvent(parent, child, event);
        }

        return handled;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int nestedScrollAxes, int type) {
        boolean handled = false;

        if (!mLocked) {
            handled = super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes, type);
        }

        return handled;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed, int type) {
        if (!mLocked) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        }
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target, int type) {
        if (!mLocked) {
            super.onStopNestedScroll(coordinatorLayout, child, target, type);
        }
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY) {
        boolean handled = false;

        if (!mLocked) {
            handled = super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
        }

        return handled;

    }
}