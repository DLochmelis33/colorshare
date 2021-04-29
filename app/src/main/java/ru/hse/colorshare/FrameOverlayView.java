package ru.hse.colorshare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FrameOverlayView extends View {

    private final Bitmap overlayCornerImage;
    private final Paint paint;
    private static final float margin = 25;
    private final OverlayFrame frame;

    private CameraOverlaidView underlyingView;

    public void setUnderlyingView(CameraOverlaidView underlyingView) {
        this.underlyingView = underlyingView;
    }

    public FrameOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        overlayCornerImage = BitmapFactory.decodeResource(getResources(), R.drawable.overlay_corner);
        frame = new OverlayFrame();
    }

    private class OverlayFrame extends Drawable {

        private Matrix ul, ur, dr, dl;

        public void resizeToView(int w, int h) {
            Log.d("frameOverlay", "resizing to w=" + w + " h=" + h);
            double effectiveW = w - 2 * margin;
            double effectiveH = h - 2 * margin;
            double resultW = effectiveW;
            double resultH = effectiveH;
            double marginX = margin;
            double marginY = margin;
            if (effectiveH / effectiveW > ImageProcessor.RATIO) {
                // h is less than max
                resultH = effectiveW * ImageProcessor.RATIO;
                marginY = (h - resultH) / 2;
            } else {
                // h is more than max
                resultW = effectiveH / ImageProcessor.RATIO;
                marginX = (w - resultW) / 2;
            }

            setupMatrices();
            ul.postTranslate((float) marginX, (float) marginY);
            ur.postTranslate((float) (marginX + resultW), (float) marginY);
            dr.postTranslate((float) (marginX + resultW), (float) (marginY + resultH));
            dl.postTranslate((float) marginX, (float) (marginY + resultH));

            FrameOverlayView.this.invalidate();
        }

        public void resizeOnTouch(float x, float y) {
            int centerX = underlyingView.getWidth() / 2;
            int centerY = underlyingView.getHeight() / 2;
            float dx = Math.abs(centerX - x);
            float dy = Math.abs(centerY - y);

            setupMatrices();
            ul.postTranslate((centerX - dx), (centerY - dy));
            ur.postTranslate((centerX + dx), (centerY - dy));
            dr.postTranslate((centerX + dx), (centerY + dy));
            dl.postTranslate((centerX - dx), (centerY + dy));

            FrameOverlayView.this.invalidate();
        }

        private void setupMatrices() {
            ul = new Matrix();
            ul.postRotate(0);
            ur = new Matrix();
            ur.postRotate(90);
            dr = new Matrix();
            dr.postRotate(180);
            dl = new Matrix();
            dl.postRotate(270);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawBitmap(overlayCornerImage, ul, paint);
            canvas.drawBitmap(overlayCornerImage, ur, paint);
            canvas.drawBitmap(overlayCornerImage, dr, paint);
            canvas.drawBitmap(overlayCornerImage, dl, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        frame.draw(canvas);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
//        invalidate();
        frame.resizeToView(params.width, params.height);
        super.setLayoutParams(params);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        frame.resizeToView(w, h);
    }

    public void setFrameSizeOnTouch(float x, float y) {
        frame.resizeOnTouch(x, y);
    }
}

