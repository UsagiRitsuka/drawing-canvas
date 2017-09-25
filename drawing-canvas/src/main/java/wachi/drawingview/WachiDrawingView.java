package wachi.drawingview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import wachi.drawingview.floodfill.ScalingUtilities;
import wachi.drawingview.manager.ThreadManager;

/**
 * Created by Usagi on 2017/8/23.
 */

public class WachiDrawingView extends View {
    final private String TAG = WachiDrawingView.class.getSimpleName();
    public enum PAINT_STYLE{
        SOLID,
        ALPHA,
        MESS
    }

    private GestureDetector gestureDetector;
    private OnGestureListener onGestureListener;
    private ScaleGestureDetector scaleGestureDetector;

    private Point lastPosition;
    private Paint paint;
    private PorterDuffXfermode clearMode;
    private PathTraker curPath;
    private List<PathTraker> pathTrakerList;
    private List<PathTraker> redoPathList;
    final private int limit = 50;

    // buffer
    private Bitmap bufferBitmap;
    private Canvas bufferCanvas;

    private Canvas newStartCanvas;
    private Bitmap newStartBitmap;

    private boolean actionUp = false;
    private boolean isDrawing = false;
    private boolean isEraser = false;

    private int originalWidth = 0;
    private int originalHeight = 0;
    private Canvas loadedCanvas;
    private Bitmap loadedBitmap;

    public WachiDrawingView(Context context){
        super(context);
        init();
    }

    public WachiDrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public WachiDrawingView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        initPaint();
        pathTrakerList = new ArrayList<>();
        redoPathList = new ArrayList<>();
        lastPosition = new Point(0, 0);
        onGestureListener = new OnGestureListener();
        gestureDetector = new GestureDetector(getContext(), onGestureListener);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), onGestureListener);
    }

    private void initPaint(){
        paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(10);
        paint.setColor(Color.BLACK);
        paint.setAlpha(100);
        clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    }

    public void setConfig(int width, int height){
        onGestureListener.setCanvasBounds(width, height);
        bufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bufferCanvas = new Canvas(bufferBitmap);

        newStartBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        newStartCanvas = new Canvas(newStartBitmap);

        originalWidth = width;
        originalHeight = height;
        Log.v(TAG, "bufferBitmap w/h: " +  bufferBitmap.getWidth() + "/" + bufferBitmap.getHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(originalWidth, originalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        adjustCanvas(canvas);
        adjustCanvas(loadedCanvas);

        canvas.drawBitmap(bufferBitmap, 0, 0, null);
        if(curPath != null && isDrawing) {
            if(!actionUp) {
                if(isDrawing && !isEraser) {
                    canvas.drawPath(curPath, paint);
                }
            }
        }

        if(loadedBitmap != null){
//            Log.v(TAG, "loadedBitmap != null");
            canvas.drawBitmap(loadedBitmap, 0, 0, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        float scaleFactor = onGestureListener.getScaleFactor();
        RectF viewRect = onGestureListener.getCurrentViewport();

        float touchX = (event.getX(0) + viewRect.left) / scaleFactor;
        float touchY = (event.getY(0) + viewRect.top) / scaleFactor;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                actionDown(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                actionMove(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                actionUp();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                actionPointDown();
                break;
        }

        invalidate();
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        onGestureListener.setViewBounds(w, h);
    }

    private void adjustCanvas(Canvas canvas){
        if(canvas != null) {
            float scaleFactor = onGestureListener.getScaleFactor();
            RectF viewRect = onGestureListener.getCurrentViewport();
            canvas.translate(-viewRect.left, -viewRect.top);
            canvas.scale(scaleFactor, scaleFactor);
        }
    }

    private void actionDown(float x, float y){
        if(isInCanvas(x, y)) {
            isDrawing = true;
            actionUp = false;

            lastPosition.set((int) x, (int) y);
            curPath = new PathTraker(lastPosition);
            curPath.setEraser(isEraser);
            curPath.setPaint(paint);
            curPath.moveTo(x, y);

            redoPathList.clear();
        }
    }

    /**
     * 畫筆模式: 儲存軌跡 => 原畫布畫上先前buffer bitmap => 原畫布畫上當前軌跡 => buffer畫布畫上當前軌跡
     * 橡皮擦模式: 儲存軌跡 => buffer畫布畫上當前軌跡 => 原畫布畫上buffer bitmap
     */
    private void actionMove(float x, float y){
        if(isInCanvas(x, y)) {
            if (isDrawing) {
                curPath.quadTo(lastPosition.x, lastPosition.y, (lastPosition.x + x) / 2, (lastPosition.y + y) / 2);
                lastPosition.set((int) x, (int) y);

                if(isEraser){
                    bufferCanvas.drawPath(curPath, paint);
                }
            }
        }
    }

    private void actionUp(){
        if(isDrawing) {
            if(!isEraser) {
                bufferCanvas.drawPath(curPath, paint);
            }

            pathTrakerList.add(curPath);
            if(pathTrakerList.size() > limit){
                PathTraker pathTraker = pathTrakerList.remove(0);
                newStartCanvas.drawPath(pathTraker, pathTraker.getPaint());
            }

            isDrawing = false;
        }

        actionUp = true;
    }

    private void actionPointDown(){
        isDrawing = false;
        curPath = null;
    }

    private boolean isInCanvas(float x, float y){
        RectF canvasRect = onGestureListener.getCanvasRect();
        return canvasRect.contains(x, y);
    }

    public void change2Eraser(){
        isEraser = true;
        paint.setXfermode(clearMode);
    }

    public void change2Brush(){
        isEraser = false;
        paint.setXfermode(null);
    }

    public void undo(){
        if(pathTrakerList.size() > 0) {
            redoPathList.add(pathTrakerList.remove(pathTrakerList.size() - 1));
            bufferBitmap.eraseColor(Color.TRANSPARENT);
            bufferCanvas.drawBitmap(newStartBitmap, 0, 0, null);

            for(PathTraker pathTraker: pathTrakerList){
                bufferCanvas.drawPath(pathTraker, pathTraker.getPaint());
            }

//            Log.v(TAG, "size pathTrakerList" + pathTrakerList.size());
            invalidate();
        }
    }

    public void redo(){
        if(redoPathList.size() > 0 && pathTrakerList.size() < limit){
            PathTraker pathTraker = redoPathList.remove(redoPathList.size() - 1);
            pathTrakerList.add(pathTraker);
            bufferCanvas.drawPath(pathTraker, pathTraker.getPaint());
        }

//        Log.v(TAG, "size pathTrakerList/redoPathList : " + pathTrakerList.size() + "/" + redoPathList.size());
        invalidate();
    }

    public void setColor(int color){
        if(!isEraser) {
            int alpha = paint.getAlpha();
            paint.setColor(color);
            paint.setAlpha(alpha);
        }
    }

    public void setStrokeWidth(int strokeWidth){
        paint.setStrokeWidth(strokeWidth);
    }

    public void setAlpha(int alpha){
        if(alpha >= 0 && alpha <= 255){
            paint.setAlpha(alpha);
        }
    }

    public void loadImg(final int resId){
        ThreadManager.getInstance().postToBackgroungThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "loadImg");
                loadedBitmap = ScalingUtilities.decodeResource(getResources(), resId,
                    originalWidth, originalHeight, ScalingUtilities.ScalingLogic.FIT)
                    .copy(Bitmap.Config.ARGB_8888, true);
                loadedBitmap = ScalingUtilities.createScaledBitmap(loadedBitmap, originalWidth, originalHeight,
                    ScalingUtilities.ScalingLogic.FIT);
                if(loadedCanvas == null){
                    loadedCanvas = new Canvas(loadedBitmap);
                } else {
                    loadedCanvas.setBitmap(loadedBitmap);
                }

                ThreadManager.getInstance().postToUIThread(new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                    }
                });
            }
        });
    }

    public void loadImg(Bitmap bitmap){
        bufferBitmap = ScalingUtilities.createScaledBitmap(bitmap, originalWidth, originalHeight,
                ScalingUtilities.ScalingLogic.FIT);

        ThreadManager.getInstance().postToUIThread(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    /**
     * just set bitmap which draw on last step
     * @param bitmap
     */
    public void setLoadedBitmap(Bitmap bitmap){
        loadedBitmap = ScalingUtilities.createScaledBitmap(bitmap, originalWidth, originalHeight,
                ScalingUtilities.ScalingLogic.FIT);
        if(loadedCanvas == null){
            loadedCanvas = new Canvas(loadedBitmap);
        } else {
            loadedCanvas.setBitmap(loadedBitmap);
        }
    }

    public Bitmap getViewBitmap(){
        return getViewBitmap(originalWidth, originalHeight);
    }

    public Bitmap getViewBitmap(int width, int height){
        // configuramos para que la view almacene la cache en una imagen
//        setDrawingCacheEnabled(true);
//        setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
//        buildDrawingCache();
//
//        if(getDrawingCache() == null) return null; // Verificamos antes de que no sea null
//
//        // utilizamos esa cache, para crear el bitmap que tendra la imagen de la view actual
//        Bitmap bitmap = Bitmap.createBitmap(getDrawingCache());
//        setDrawingCacheEnabled(false);
//        destroyDrawingCache();
//
//
//        bitmap = ScalingUtilities.createScaledBitmap(bitmap, width, height, ScalingUtilities.ScalingLogic.FIT);
//        return bitmap;


        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawBitmap(bufferBitmap, 0, 0, null);
        canvas.drawBitmap(loadedBitmap, 0, 0, null);

        return ScalingUtilities.createScaledBitmap(bitmap, width, height, ScalingUtilities.ScalingLogic.FIT);
    }

}
