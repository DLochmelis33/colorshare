package ru.hse.colorshare.transmitter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class LocatorMarkGraphic {
    public final static int SIDE_SIZE_IN_UNITS = 6;
    public final static int SIZE_IN_UNITS = SIDE_SIZE_IN_UNITS * SIDE_SIZE_IN_UNITS;

    public static void draw(Canvas canvas, Location location, int unitSize) {
        // left top bottom of location mark is (0;0)
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        canvas.drawRect(new Rect(0, 0, 6 * unitSize, 6 * unitSize), paint);
        canvas.save();

        Rect outerBlackRect = new Rect(0, 0, 5 * unitSize, 5 * unitSize);
        Rect innerWhiteRect = new Rect(unitSize, unitSize, 4 * unitSize, 4 * unitSize);
        Rect innerBlackRect = new Rect(2 * unitSize, 2 * unitSize, 3 * unitSize, 3 * unitSize);
        switch (location) {
            case LEFT_TOP:
                break;
            case RIGHT_TOP:
                canvas.translate(unitSize, 0);
                break;
            case LEFT_BOTTOM:
                canvas.translate(0, unitSize);
                break;
            case RIGHT_BOTTOM:
                canvas.translate(unitSize, unitSize);
                break;
        }
        paint.setColor(Color.BLACK);
        canvas.drawRect(outerBlackRect, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRect(innerWhiteRect, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRect(innerBlackRect, paint);
        canvas.restore();
    }

    public enum Location {
        LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM
    }
}
