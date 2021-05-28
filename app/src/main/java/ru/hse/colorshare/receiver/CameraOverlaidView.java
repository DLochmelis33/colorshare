package ru.hse.colorshare.receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CameraOverlaidView extends TextureView {
    private static final String TAG = "CameraOverlaidView";
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

    public void resizeToRatio(Size size) {
        int w = getWidth();
        int h = getHeight();
        Log.d(TAG, "resizing: w was " + w + ", h was " + h);
        double scaleByWidth = (double) w / size.getWidth();
        double scaleByHeight = (double) h / size.getHeight();
        if (scaleByWidth * size.getHeight() <= h) {
            setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scaleByWidth), (int) (size.getHeight() * scaleByWidth)));
        } else if (scaleByHeight * size.getWidth() <= w) {
            setLayoutParams(new FrameLayout.LayoutParams((int) (size.getWidth() * scaleByHeight), (int) (size.getHeight() * scaleByHeight)));
        } else {
            throw new AssertionError("unscalable size?");
        }
        invalidate();
//        Log.w(TAG, "resizing: w new " + getWidth() + ", h new " + getHeight());
    }
}
