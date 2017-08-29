package wachi.drawingview.floodfill;

/**
 * Created by darri on 12/6/2015.
 */

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class Utilities
{
    private static final String TAG    = "Utilities";
    private static final Matrix MATRIX = new Matrix();

    public static double distance (Point p1, Point p2)
    {
        int xDiff = p1.x - p2.x;
        int yDiff = p1.y - p2.y;
        int xSqr  = xDiff * xDiff;
        int ySqr  = yDiff * yDiff;

        double dist = SquareRoot.fastSqrt (xSqr + ySqr);
        return dist;
    }

    public static double distance (int x1, int y1, int x2, int y2)
    {
        int xDiff = x1 - x2;
        int yDiff = y1 - y2;
        int xSqr  = xDiff * xDiff;
        int ySqr  = yDiff * yDiff;

        double dist = SquareRoot.fastSqrt (xSqr + ySqr);
        return dist;
    }

    public static double speed (Point p1, Point p2, long millis)
    {
        double dist = distance (p1, p2);
        double speed = dist / millis;
        return speed;
    }

    public static double speed (Point p1, Point p2, long startTime, long stopTime)
    {
        double dist = distance (p1, p2);
        double speed = dist / (stopTime - startTime);
        return speed;
    }

    public static double speed (int x1, int y1, int x2, int y2, long startTime, long stopTime)
    {
        double dist = distance (x1, y1, x2, y2);
        double speed = dist / (stopTime - startTime);
        return speed;
    }

    public static double speed (double distance, long millis)
    {
        double speed = distance / millis;
        return speed;
    }

    public static double speed (double distance, long startTime, long stopTime)
    {
        double speed = distance / (stopTime - startTime);
        return speed;
    }


    public static double fastSpeed (int x1, int y1, int x2, int y2, long startTime, long stopTime)
    {
        int xDiff = x1 - x2;
        int yDiff = y1 - y2;

        if (xDiff < 0) {
            xDiff = 0 - xDiff;
        }
        if (yDiff < 0) {
            yDiff = 0 - yDiff;
        }
        double speed = ((double) (xDiff + yDiff)) / (stopTime - startTime);
        return speed;
    }

    public static void dumpEvent (MotionEvent event)
    {
        String names[]    = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" , "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
        StringBuilder sb         = new StringBuilder();
        int           action     = event.getAction ();
        int           actionCode = action & MotionEvent.ACTION_MASK;

        sb.append ("event ACTION_" ).append (names[actionCode]);

        if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                ||  actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append (" (pid " ).append (action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append (")" );
        }
        sb.append ("[" );

        for (int i = 0; i < event.getPointerCount (); i++) {
            sb.append ("#" ).append (i);
            sb.append (" (pid " ).append (event.getPointerId (i));
            sb.append (")=" ).append ((int) event.getX (i));
            sb.append ("," ).append ((int) event.getY (i));

            if (i + 1 < event.getPointerCount ()) {
                sb.append (";" );
            }
        }
        sb.append ("]" );
        Log.d (TAG, sb.toString ());
    }

    public static Bitmap rescale (Bitmap bitmap, int width, int height, Configuration config)
    {
        float scale;

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            scale = ((float)width) / height;
        }
        else {
            scale = ((float)height) / width;
        }
        Matrix matrix = new Matrix();
        matrix.postScale (scale, scale);

        Bitmap resizedBitmap = Bitmap.createBitmap (bitmap, 0, 0, bitmap.getWidth (), bitmap.getHeight (), matrix, true);

        return resizedBitmap;
    }

    public static Bitmap createScaledBitmap (Bitmap unscaledBitmap, int dstWidth, int dstHeight, ScalingUtilities.ScalingLogic scalingLogic)
    {
        Rect srcRect = calculateSrcRect (unscaledBitmap.getWidth (), unscaledBitmap.getHeight (), dstWidth, dstHeight, scalingLogic);
        Rect dstRect = calculateDstRect (unscaledBitmap.getWidth (), unscaledBitmap.getHeight (), dstWidth, dstHeight, scalingLogic);

        Bitmap scaledBitmap = Bitmap.createBitmap (dstRect.width (), dstRect.height (), Config.ARGB_8888);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap (unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }


    public static Rect calculateSrcRect (int srcWidth, int srcHeight, int dstWidth, int dstHeight, ScalingUtilities.ScalingLogic scalingLogic)
    {
        if (scalingLogic == ScalingUtilities.ScalingLogic.CROP) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                final int srcRectWidth = (int) (srcHeight * dstAspect);
                final int srcRectLeft  = (srcWidth - srcRectWidth) / 2;
                return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
            }
            else {
                final int srcRectHeight = (int) (srcWidth / dstAspect);
                final int scrRectTop    = (int) (srcHeight - srcRectHeight) / 2;
                return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
            }
        }
        else {
            return new Rect(0, 0, srcWidth, srcHeight);
        }
    }


    public static Rect calculateDstRect (int srcWidth, int srcHeight, int dstWidth, int dstHeight, ScalingUtilities.ScalingLogic scalingLogic)
    {
        if (scalingLogic == ScalingUtilities.ScalingLogic.FIT) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int) (dstWidth / srcAspect));
            }
            else {
                return new Rect(0, 0, (int) (dstHeight * srcAspect), dstHeight);
            }
        }
        else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }

    public static byte[] getBitmapBytes (Bitmap bitmap)
    {
        int size = bitmap.getWidth () * bitmap.getHeight ();
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        Bitmap mutableBitmap = bitmap.copy (Config.ARGB_8888, true);
        mutableBitmap.compress (Bitmap.CompressFormat.PNG, 100, out);
        byte[] bytes = out.toByteArray ();
        return bytes;
    }

    public static String getImageAsString (Bitmap targetBitmap)
    {
        //Bitmap myImage = BitmapFactory.decodeFile ("/path_to/image.jpg");
        if (targetBitmap == null) {
            return "";
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        targetBitmap.compress (Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] buf = baos.toByteArray ();
        String encodedImage = new String(Base64.encode (buf, Base64.DEFAULT));
        Log.i (TAG, "Returning " + buf.length + " bytes for users image.");
        return encodedImage;
    }

    public static Bitmap getImageFromString (String imageString)
    {
        byte[] buf   = Base64.decode (imageString, Base64.DEFAULT);
        Bitmap image = BitmapFactory.decodeByteArray (buf, 0, buf.length);

        Bitmap mutableBitmap = image.copy (Config.ARGB_8888, true);

        return mutableBitmap;
    }

    public static Bitmap copyBitmapFromImage (ImageView ivSrc)
    {
        Bitmap bmSrc1 = ((BitmapDrawable)ivSrc.getDrawable ()).getBitmap ();
        Bitmap bmSrc2 = bmSrc1.copy (bmSrc1.getConfig (), true);
        return bmSrc2;
    }

    public static Bitmap copyBitmap (Bitmap src)
    {
        Bitmap bmSrc2 = src.copy (src.getConfig (), true);
        return bmSrc2;
    }

    public static Bitmap regenerateBitmapFromBytes (byte[] bytes)
    {
        Bitmap tempBitmap    = BitmapFactory.decodeByteArray (bytes, 0, bytes.length);
        Bitmap mutableBitmap = tempBitmap.copy (Config.ARGB_8888, true);
        return mutableBitmap;
    }

    public static boolean pixelInColor (int px, int tClr, int rTol, int gTol, int bTol)
    {
        int red    = (px >>> 16)   & 0xff;
        int green  = (px >>> 8)    & 0xff;
        int blue   =  px           & 0xff;
        int tRed   = (tClr >>> 16) & 0xff;
        int tGreen = (tClr >>> 8)  & 0xff;
        int tBlue  =  tClr         & 0xff;

        return (red   >= (tRed   - rTol) && red   <= (tRed   + rTol) &&
                green >= (tGreen - gTol) && green <= (tGreen + gTol) &&
                blue  >= (tBlue  - bTol) && blue  <= (tBlue  + bTol));
    }

    /**
     * Used to remove the press position you get when there is a long press for a flood or similar action
     * @param x
     * @param y
     */
    public static void erasePressPoint (Bitmap bitmap, int x, int y, int replaceColor)
    {

        int fromX = x - 3;
        int fromY = y - 3;
        int toX   = x + 3;
        int toY   = y + 3;

        if (x < 1) {
            x = 1;
        }
        if (x > bitmap.getWidth () - 1) {
            x = bitmap.getWidth () - 1;
        }
        if (y > bitmap.getHeight () - 1) {
            y = bitmap.getHeight () - 1;
        }
        for (int x1 = fromX; x1 <= toX; x1++) {
            for (int y1 = fromY; y1 <= toY; y1++) {
                bitmap.setPixel (x1, y1, replaceColor);
                Log.d (TAG, "Replacing " + x1 + ", " + y1 + " with color " + replaceColor);
            }
        }
    }

    public static Bitmap createThumbnail (Bitmap src)
    {
        Log.d (TAG, "Creating thumbnail. Original width: " + src.getWidth () + " height: " + src.getHeight ());

        MATRIX.setScale (0.24f, 0.24f);

        Bitmap mutableBitmap = src.copy (Config.ARGB_8888, true);
        Bitmap resizedBitmap = Bitmap.createBitmap (mutableBitmap, 0, 0, mutableBitmap.getWidth (), mutableBitmap.getHeight (), MATRIX, true);

        return resizedBitmap;
    }

    public static String getColorAsString (int color)
    {
        int red    = (color >>> 16)   & 0xff;
        int green  = (color >>> 8)    & 0xff;
        int blue   =  color           & 0xff;

        String sColor = "Red: " + Integer.toString (red) +  " Green: " + Integer.toString (green) + " Blue: " + Integer.toString (blue);

        return sColor;
    }

    public static int getIntFromByteArray (byte[] array)
    {
        String temp = new String(array);
        int result = Integer.parseInt (temp);
        return result;
    }

    public static float getFloatFromByteArray (byte[] array)
    {
        String temp = new String(array);
        float result = Float.parseFloat (temp);
        return result;
    }

    public static String getStringFromByteArray (byte[] array)
    {
        return new String(array);
    }

    public static byte[] getByteArrayFromBitmap (Bitmap src, Bitmap.CompressFormat format, int quality)
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        src.compress (format, quality, os);
        byte[] array = os.toByteArray ();
        return array;
    }

    public static void saveImage (byte[] blob, String fileName)
    {
        FileOutputStream fos;

        try {
            fos = new FileOutputStream(new File("/sdcard/" + fileName +".jpg"));
            fos.write (blob);
            fos.close ();
        }
        catch (FileNotFoundException e) {
            Log.d (TAG, "FileNotFoundException saving image.");
            e.printStackTrace ();
        }
        catch (IOException e) {
            Log.d (TAG, "IOException saving image.");
            e.printStackTrace ();
        }
    }

    /**
     * Saves the bitmap to the SD Card
     *
     * @param bitmap
     */
    public static void saveImage (Bitmap bitmap)
    {
        String fileName = getNewFileName("IMG_");

        byte[] blob = getByteArrayFromBitmap (bitmap, Bitmap.CompressFormat.JPEG, 100);

        saveImage (blob, fileName);
    }

    /**
     * Returns the intersection point of two lines.
     *
     * @param   x1Line1   First line
     * @param   x2Line2   Second line
     * @return  The Point object where the two lines intersect. This method
     * returns null if the two lines do not intersect.
     * @throws  <tt>MultipleIntersectionException</tt> when the two lines
     * have more than one intersection point.
     */
    public static Point getIntersection (
            int x1Line1, int y1Line1,
            int x2Line1, int y2Line1,
            int x1Line2, int y1Line2,
            int x2Line2, int y2Line2,
            int maxX, int maxY,
            int minX, int minY)
    {
        double dyline1, dxline1;
        double dyline2, dxline2, e, f;

        /*  Check to see if the segments have any endpoints in common.
            If they do, then return the endpoints as the intersection point
        */
        if ((x1Line1 == x1Line2) && (y1Line1 == y1Line2)) {
            return (new Point((int) x1Line1, (int) y1Line1));
        }
        if ((x1Line1 == x2Line2) && (y1Line1 == y2Line2)) {
            return (new Point((int) x1Line1, (int) y1Line1));
        }
        if ((x2Line1 == x1Line2) && (y2Line1 == y1Line2)) {
            return (new Point((int) x2Line1, (int) y2Line1));
        }
        if ((x2Line1 == x2Line2) && (y2Line1 == y2Line2)) {
            return (new Point((int) x2Line1, (int) y2Line1));
        }
        dyline1 = - (y2Line1 - y1Line1);
        dxline1 =    x2Line1 - x1Line1;
        dyline2 = - (y2Line2 - y1Line2);
        dxline2 =    x2Line2 - x1Line2;

        e = - (dyline1 * x1Line1) - (dxline1 * y1Line1);
        f = - (dyline2 * x1Line2) - (dxline2 * y1Line2);

        /* compute the intersection point using
          ax+by+e = 0 and cx+dy+f = 0

          If there is more than 1 intersection point between two lines,
        */
        int x, y;
        if ((dyline1 * dxline2 - dyline2 * dxline1) == 0 ) {
            x = y = 0;
        }
        else {
            x = (int) (- (e * dxline2 - dxline1 * f)/ (dyline1 * dxline2 - dyline2 * dxline1));
            y = (int) (- (dyline1 * f - dyline2 * e)/ (dyline1 * dxline2 - dyline2 * dxline1));
            x = x > maxX? maxX: x < minX? minX: x;
            y = y > maxY? maxY: y < minY? minY: y;
        }
        return (new Point(x, y));
    }

    public static String getNewFileName(String primer)
    {
        Calendar cal = Calendar.getInstance();

        StringBuilder builder = new StringBuilder(primer);

        builder.append(cal.get(Calendar.YEAR));
        builder.append('_');
        builder.append(cal.get(Calendar.MONTH));
        builder.append('_');
        builder.append(cal.get(Calendar.DAY_OF_MONTH));
        builder.append('_');
        builder.append(cal.get(Calendar.HOUR));
        builder.append('_');
        builder.append(cal.get(Calendar.MINUTE));
        builder.append('_');
        builder.append(cal.get(Calendar.SECOND));

        return builder.toString();
    }

    public static byte[] getByteArray(boolean input)
    {
        byte[] bytes = new byte[1];

        if(input){
            bytes[0] = 1;
        }

        return bytes;
    }

    public static boolean getBooleanFromByteArray(byte[] input)
    {
        if(input[0] > 0){
            return true;
        }

        return false;
    }

}
