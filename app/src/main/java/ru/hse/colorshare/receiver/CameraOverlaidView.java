package ru.hse.colorshare.receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CameraOverlaidView extends TextureView {

    private FrameOverlayView overlayView;

    public CameraOverlaidView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOverlayView(FrameOverlayView overlayView) {
        this.overlayView = overlayView;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (overlayView != null) {
            Log.d("CameraOverlaidView", "w=" + w + " h=" + h + " oldw=" + oldw + " oldh=" + oldh);
            overlayView.setLayoutParams(new FrameLayout.LayoutParams(w, h));
        }
        requestLayout();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                overlayView.setFrameSizeOnTouch(event.getX(), event.getY());
                break;
            default:
                break;
        }
        return true;
    }
}
