package wachi.drawingview;

import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by Usagi on 2017/8/23.
 */

public class OnGestureListener implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener{
    private final String TAG = OnGestureListener.class.getSimpleName();

    private RectF currentViewport = new RectF();
    private RectF canvasRect = new RectF();

    private float scaleFactor = 1.0f;
    private float minZoom = 1f;
    private float maxZoom = 5f;
    private int canvasWidth = 0;
    private int canvasHeight = 0;

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {
        if(motionEvent1.getPointerCount() == 2){
            float viewportOffsetX = distanceX * currentViewport.width() / canvasRect.width();
            float viewportOffsetY = distanceY * currentViewport.height() / canvasRect.height();
            setViewportBottomLeft(currentViewport.left + viewportOffsetX,
                currentViewport.bottom + viewportOffsetY);
        }

        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    private void setViewportBottomLeft(float x, float y) {
        float viewWidth = currentViewport.width();
        float viewHeight = currentViewport.height();

//        x = Math.max(0, Math.min(x, 0));
//        y = Math.max(0 + viewHeight, Math.min(y, viewHeight));
//        currentViewport.set(x, y - viewHeight, x + viewWidth, y);

        float left = Math.max(0, Math.min(x, canvasRect.width() - viewWidth));
        float bottom = Math.max(0 + viewHeight, Math.min(y, canvasRect.height()));
        float top = bottom - viewHeight;
        float right = left + viewWidth;

//        Log.v(TAG, "left/ bottom/top/right: " + left + "/" + bottom + "/" + top + "/" + right);
        currentViewport.set(left, top, right, bottom);
    }

    // OnScaleGestureListener
    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
//        Log.v(TAG, "onScale");
        scaleFactor *= scaleGestureDetector.getScaleFactor();
        scaleFactor = Math.max(minZoom, Math.min(scaleFactor, maxZoom));

        canvasRect.right = canvasWidth * scaleFactor;
        canvasRect.bottom = canvasHeight * scaleFactor;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

    }

    public RectF getCurrentViewport() {
        return currentViewport;
    }

    public RectF getCanvasRect() {
        return canvasRect;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public void setCanvasBounds(int canvasWidth, int canvasHeight){
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        canvasRect.right = canvasWidth;
        canvasRect.bottom = canvasHeight;
    }

    public void setViewBounds(int viewWidth, int viewHeight) {
        currentViewport.right = viewWidth;
        currentViewport.bottom = viewHeight;
    }
}
