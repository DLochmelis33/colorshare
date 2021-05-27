package ru.hse.colorshare.receiver;

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

import ru.hse.colorshare.R;
import ru.hse.colorshare.receiver.util.RelativePoint;

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
        volatile double[] xHints = new double[4];
        volatile double[] yHints = new double[4];

        public void resizeToView(int w, int h) {
            Log.d("frameOverlay", "resizing to w=" + w + " h=" + h);
            double effectiveW = w - 2 * margin;
            double effectiveH = h - 2 * margin;
            double resultW = effectiveW;
            double resultH = effectiveH;
            double marginX = margin;
            double marginY = margin;
            if (effectiveH / effectiveW > ImageProcessor.DEFAULT_RATIO) {
                // h is less than max
                resultH = effectiveW * ImageProcessor.DEFAULT_RATIO;
                marginY = (h - resultH) / 2;
            } else {
                // h is more than max
                resultW = effectiveH / ImageProcessor.DEFAULT_RATIO;
                marginX = (w - resultW) / 2;
            }

            setupMatrices();

            if (underlyingView == null) {
                return;
            }
            double viewWidth = underlyingView.getWidth();
            double viewHeight = underlyingView.getHeight();
            ul.postTranslate((float) marginX, (float) marginY);
            xHints[0] = marginX / viewWidth;
            yHints[0] = marginY / viewHeight;
            ur.postTranslate((float) (marginX + resultW), (float) marginY);
            xHints[1] = (marginX + resultW) / viewWidth;
            yHints[1] = marginY / viewHeight;
            dr.postTranslate((float) (marginX + resultW), (float) (marginY + resultH));
            yHints[2] = (marginX + resultW) / viewHeight;
            yHints[2] = (marginY + resultH) / viewHeight;
            dl.postTranslate((float) marginX, (float) (marginY + resultH));
            yHints[3] = marginX / viewHeight;
            yHints[3] = (marginY + resultH) / viewHeight;

            FrameOverlayView.this.invalidate();
        }

        public void resizeOnTouch(float x, float y) {
            int centerX = underlyingView.getWidth() / 2;
            int centerY = underlyingView.getHeight() / 2;
            float dx = Math.abs(centerX - x);
            float dy = Math.abs(centerY - y);

            setupMatrices();
            double viewWidth = underlyingView.getWidth();
            double viewHeight = underlyingView.getHeight();
            ul.postTranslate((centerX - dx), (centerY - dy));
            xHints[0] = (centerX - dx) / viewWidth;
            yHints[0] = (centerY - dy) / viewHeight;
            ur.postTranslate((centerX + dx), (centerY - dy));
            xHints[1] = (centerX + dx) / viewWidth;
            yHints[1] = (centerY - dy) / viewHeight;
            dr.postTranslate((centerX + dx), (centerY + dy));
            xHints[2] = (centerX + dx) / viewWidth;
            yHints[2] = (centerY + dy) / viewHeight;
            dl.postTranslate((centerX - dx), (centerY + dy));
            xHints[3] = (centerX - dx) / viewWidth;
            yHints[3] = (centerY + dy) / viewHeight;

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

        if (extras != null) {
            canvas.drawBitmap(extras, extrasMtx, paint);
        }
    }

    private Bitmap extras;
    private Matrix extrasMtx;

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        frame.resizeToView(params.width, params.height);
        requestLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        frame.resizeToView(w, h);
    }

    public void setFrameSizeOnTouch(float x, float y) {
        frame.resizeOnTouch(x, y);
    }

    public RelativePoint[] getCornersHints() {
        RelativePoint[] result = new RelativePoint[4];
        for (int i = 0; i < 4; i++) {
            result[i] = new RelativePoint(frame.xHints[i], frame.yHints[i]);
        }
        return result;
    }

    public void setExtras(Bitmap bitmap) {
        extras = bitmap;
        float scaleWidth = (float) underlyingView.getWidth() / bitmap.getWidth();
        float scaleHeight = (float) underlyingView.getHeight() / bitmap.getHeight();
        extrasMtx = new Matrix();
        extrasMtx.postScale(scaleWidth, scaleHeight);
//        Log.d("xtr", "undW=" + underlyingView.getWidth() + " undH=" + underlyingView.getHeight() +
//                " bitW=" + bitmap.getWidth() + " bitH=" + bitmap.getHeight());
        invalidate();
    }
}

