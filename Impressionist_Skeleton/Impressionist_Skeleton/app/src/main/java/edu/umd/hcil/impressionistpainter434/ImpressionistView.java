package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorEvent;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Random;

/**
 * kate Tolstaya
 */
public class ImpressionistView extends View {
    private static final int DEFAULT_BRUSH_RADIUS = 25;
    private static final int LINE_WIDTH = 2;
    private static final Random _random = new Random();
    float lastTouchX = 0;
    float lastTouchY = 0;
    private ImageView _imageView;
    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();
    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;

    //float[] gravity = {0,0,0};
    //float[] linear_acceleration = {0,0,0};
    private float _minBrushRadius = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

//    public void updateSensorValues (SensorEvent event, Context context){
//
//        // In this example, alpha is calculated as t / (t + dT),
//        // where t is the low-pass filter's time-constant and
//        // dT is the event delivery rate.
//
//        int duration = Toast.LENGTH_SHORT;
//        String text = "Sensor value";
//
//        Toast toast = Toast.makeText(context, text, duration);
//        toast.show();
//
//        final float alpha = (float)0.8;
//
//        // Isolate the force of gravity with the low-pass filter.
//        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
//        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
//        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
//
//        // Remove the gravity contribution with the high-pass filter.
//        linear_acceleration[0] = event.values[0] - gravity[0];
//        linear_acceleration[1] = event.values[1] - gravity[1];
//        linear_acceleration[2] = event.values[2] - gravity[2];
//
//        if ( _brushType == BrushType.Gyro){
//
//            Rect rect = getBitmapPositionInsideImageView(_imageView);
//            Bitmap bitmap = ((BitmapDrawable)_imageView.getDrawable()).getBitmap();
//            double offset = 0;//rect.width()/2;
//            double scalingX = bitmap.getWidth()/rect.width();
//            double scalingY = bitmap.getHeight()/rect.height();
//
//            //Log.d(ImpressionistView.class.getSimpleName(), "drawing");
//            float touchX = gravity[0];
//            float touchY = gravity[1];
//
//            int touchX2 = (int)(touchX/scalingX);
//            int touchY2 = (int)(touchY/scalingY);
//
//            if (!(rect.contains(touchX2,touchY2) &&touchX2 > 0 && touchX2 < bitmap.getWidth() && touchY2 > 0 && touchY2 < bitmap.getHeight() ))
//                return;
//
//            int pixel = (bitmap).getPixel((int) touchX2, (int) touchY2);
//            _paint.setColor(pixel);
//
//            _offScreenCanvas.drawCircle(touchX, touchY, DEFAULT_BRUSH_RADIUS, _paint);
//            invalidate();
//
//        }
//
//    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting() {
        // makes the screen black
        _offScreenBitmap.eraseColor(Color.WHITE);
        _offScreenCanvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        invalidate();
    }

    public void savePainting(Context context){
        final Bitmap bitmap = _offScreenBitmap;
        final File folder;
        File file;

        folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        final Date now = new Date();
        file = new File(folder, (now.getTime() / 1000) + ".png");
        try {
            file.createNewFile();

            final FileOutputStream ostream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
            ostream.close();

            // Tell the media scanner about the new file so that it is immediately available to the user.
            MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null, null);
        } catch (IOException e) {
            // Insert error handling here :)
        }

        // show toast/brief pop-up  that shows file name/location
        CharSequence text = file.toString() + " saved!";
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        //long startTime = SystemClock.elapsedRealtime();

        //float curTouchX = motionEvent.getX();
        //float curTouchY = motionEvent.getY();

        Log.d("Impressionist View", "On touch");

        float brushRadius = DEFAULT_BRUSH_RADIUS;

        switch(motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:

                // For efficiency, motion events with ACTION_MOVE may batch together multiple movement samples within a single object.
                // The most current pointer coordinates are available using getX(int) and getY(int).
                // Earlier coordinates within the batch are accessed using getHistoricalX(int, int) and getHistoricalY(int, int).
                // See: http://developer.android.com/reference/android/view/MotionEvent.html
                int historySize = motionEvent.getHistorySize();

                if (_imageView != null && _imageView.getDrawable() != null) {
//                    Bitmap bitmap = ((BitmapDrawable) _imageView.getDrawable()).getBitmap();
//                    int pixel = bitmap.getPixel((int) curTouchX, (int) curTouchY);
//                    _paint.setColor(pixel);
                    Log.d(ImpressionistView.class.getSimpleName(), "calculating");


                    if (historySize > 0) {
                        lastTouchX = motionEvent.getHistoricalX(0);
                        lastTouchY = motionEvent.getHistoricalY(0);
                    }

                    Rect rect = getBitmapPositionInsideImageView(_imageView);
                    Bitmap bitmap = ((BitmapDrawable)_imageView.getDrawable()).getBitmap();
                    double offset = 0;//rect.width()/2;
                    double scalingX = bitmap.getWidth()/rect.width();
                    double scalingY = bitmap.getHeight()/rect.height();

                    for (int i = 0; i < historySize; i++) {
                        Log.d(ImpressionistView.class.getSimpleName(), "drawing");
                        float touchX = motionEvent.getHistoricalX(i);
                        float touchY = motionEvent.getHistoricalY(i);

                        int touchX2 = (int)(touchX/scalingX);
                        int touchY2 = (int)(touchY/scalingY);

                        if (!(rect.contains(touchX2,touchY2) &&touchX2 > 0 && touchX2 < bitmap.getWidth() && touchY2 > 0 && touchY2 < bitmap.getHeight() ))
                            continue;

                        int pixel = (bitmap).getPixel((int) touchX2, (int) touchY2);
                        _paint.setColor(pixel);

                        float dx = lastTouchX - touchX;
                        float dy = lastTouchY - touchY;
                        double magnitude = Math.max(0.001,Math.sqrt(Math.pow(dx,2)+Math.pow(dy,2)));


                        brushRadius = (float)(_minBrushRadius + 2*DEFAULT_BRUSH_RADIUS / (Math.max(1/100,Math.sqrt(Math.pow(dx,2)+Math.pow(dy,2)))));

                        lastTouchX = touchX;
                        lastTouchY = touchY;
                        //brushRadius = _minBrushRadius + pressure * DEFAULT_BRUSH_RADIUS / 2;

                        switch (_brushType) {
                            case Square:
                                _offScreenCanvas.drawRect(touchX - brushRadius, touchY - brushRadius, touchX + brushRadius, touchY + brushRadius, _paint);
                                break;
                            case Circle:
                                _offScreenCanvas.drawCircle(touchX, touchY, brushRadius, _paint);
                                break;
                            case Line:
                                _offScreenCanvas.drawLine((float) (touchX - DEFAULT_BRUSH_RADIUS * dy / magnitude), touchY - (float) (DEFAULT_BRUSH_RADIUS * dx / magnitude), touchX + (float) (DEFAULT_BRUSH_RADIUS * dy / magnitude), touchY + (float) (DEFAULT_BRUSH_RADIUS * dx / magnitude), _paint);
                                break;
                           // case Gyro:
                           //     _offScreenCanvas.drawLine((float) (touchX - DEFAULT_BRUSH_RADIUS * dy / magnitude), touchY - (float) (DEFAULT_BRUSH_RADIUS * dx / magnitude), touchX + (float) (DEFAULT_BRUSH_RADIUS * dy / magnitude), touchY + (float) (DEFAULT_BRUSH_RADIUS * dx / magnitude), _paint);
                           //     break;

                        }
                        //Log.d(ImpressionistView.class.getSimpleName(), "Point drawn");
                    }



                    //float pressure = motionEvent.getPressure (); // 0 to 1
                    //brushRadius = DEFAULT_BRUSH_RADIUS / 2 + pressure * DEFAULT_BRUSH_RADIUS / 2;

//                    switch (_brushType) {
//                        case Square:
//                            _offScreenCanvas.drawRect(curTouchX - brushRadius, curTouchY - brushRadius, curTouchX + brushRadius, curTouchY + brushRadius, _paint);
//                            break;
//                        case Circle:
//                            _offScreenCanvas.drawCircle(curTouchX, curTouchY, brushRadius, _paint);
//                            break;
//                        case Line:
//                            //_offScreenCanvas.drawLine(curTouchX, curTouchY - brushRadius, curTouchX, curTouchY + brushRadius, _paint);
//                            break;
//                        case ParticleEmitter:
//                            break;
//                    }

                    invalidate();
                    break;
                }
            case MotionEvent.ACTION_UP:
                break;
        }
        Log.d(ImpressionistView.class.getSimpleName(), "Done drawing");
        return true;
    }

    protected int getRandomColor(){
        int r = _random.nextInt(255);
        int g = _random.nextInt(255);
        int b = _random.nextInt(255);
        //int b = 50 + (int)(_random.nextFloat() * (255-50));
        return Color.argb(_alpha, r, g, b);
    }
}

