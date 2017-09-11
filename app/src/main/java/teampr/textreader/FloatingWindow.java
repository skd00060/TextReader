package teampr.textreader;

/**
 * Created by CHO HYEONIN on 2017-05-28.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Locale;

import static android.content.ContentValues.TAG;

public class FloatingWindow extends Service {
    TextToSpeech tts;

    int[] xylocation = new int[2];
    int flotingwidth=0;
    int flotingheight=0;
    DeliverMp dmp = new DeliverMp();
    private TessOCR mTessOCR;
    public static final String lang ="kor";
    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/DemoOCR/";
    WindowManager wm;
    LinearLayout ll;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private ImageReader mImageReader;
    private boolean shouldCapture = true;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;
            Bitmap floatingbitmap=null;
            ll.getLocationOnScreen(xylocation);
            flotingwidth=ll.getWidth();
            flotingheight=ll.getHeight();
            try {
                //가장 최신 이미지를 가져 옵니다. image 객체로
                image = mImageReader.acquireLatestImage();
                if (image != null && shouldCapture) {
                    Log.e(TAG, "oh yes");
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    floatingbitmap = Bitmap.createBitmap(bitmap,xylocation[0],xylocation[1],flotingwidth,flotingheight);
                    final String result = mTessOCR.getOCRResult(floatingbitmap).toLowerCase();

                    if (result != null && !result.equals("")) {
                        String s = result.trim();
                        Log.e("TESS", "working TESS");
                    }
                    String utteranceId=this.hashCode() + "";
                    tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                    stopProjection();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                if (bitmap != null) {
                    bitmap.recycle();
                }
                if (image != null) {
                    image.close();
                    Log.e(TAG, "oh yes2");
                }

            }
        }
    }
    private class MediaProjectionStopCallback extends MediaProjection.Callback{
        @Override
        public void onStop(){
            Log.e("Screencapture", "stopping projection");
            if(mVirtualDisplay !=null) mVirtualDisplay.release();
            if(mImageReader !=null) mImageReader.setOnImageAvailableListener(null,null);
            dmp.returnmp().unregisterCallback(MediaProjectionStopCallback.this);
            ll.setVisibility(View.VISIBLE);
            Log.e("Screencapture", "work?");


        }
    }

    private void createVirtualDisplay(){
        Log.e(TAG, "start CreateVirtualDisplay()");
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        Log.e(TAG, "before dmp.return");
        mVirtualDisplay=dmp.returnmp().createVirtualDisplay("screencap",mWidth,mHeight,mDensity,VIRTUAL_DISPLAY_FLAGS,mImageReader.getSurface(),null,null);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(),null);
        Log.e(TAG,"end CVD");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    private void stopProjection(){
        Log.e(TAG, "stop called");
        if(dmp.returnmp() != null){
            shouldCapture=false;
            dmp.returnmp().stop();
            if(mVirtualDisplay != null) mVirtualDisplay.release();
            if(mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
        }
        Log.e(TAG, "stop ended");
    }
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v("Main", "ERROR: Creation of directory " + path + " on sdcard failed");
                    break;
                } else {
                    Log.v("Main", "Created directory " + path + " on sdcard");
                }
            }

        }
        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();

                InputStream in = assetManager.open(lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                // Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                // Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }


        }
        mTessOCR =new TessOCR();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Log.e(TAG, "before call createVirtualDisplay");

        ll = new LinearLayout(this);
        ll.setBackgroundColor(Color.RED);
        LinearLayout.LayoutParams layoutParameteres = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400);
        ll.setBackgroundColor(Color.argb(66,255,0,0));
        ll.setLayoutParams(layoutParameteres);

        final WindowManager.LayoutParams parameters = new WindowManager.LayoutParams(
                500, 200,WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        parameters.gravity = Gravity.LEFT | Gravity.TOP;
        parameters.x = 0;
        parameters.y = 0;
        flotingwidth=parameters.width;
        flotingheight=parameters.height;

        final Button capture = new Button(this);
        capture.setText("Capture");
        final Button     stop = new Button(this);
        stop.setText("Stop");
        final Button moveyourass = new Button(this);
        moveyourass.setText("move");
        ViewGroup.LayoutParams btnParameters = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        stop.setLayoutParams(btnParameters);
        ll.addView(moveyourass);
        ll.addView(capture);
        ll.addView(stop);
        wm.addView(ll, parameters);

        moveyourass.setOnTouchListener(new View.OnTouchListener() {
            WindowManager.LayoutParams updatedParameters = parameters;
            double x;
            double y;
            double pressedX;
            double pressedY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        x = updatedParameters.x;
                        y = updatedParameters.y;

                        pressedX = event.getRawX();
                        pressedY = event.getRawY();

                        break;

                    case MotionEvent.ACTION_MOVE:
                        updatedParameters.x = (int) (x + (event.getRawX() - pressedX));
                        updatedParameters.y = (int) (y + (event.getRawY() - pressedY));

                        wm.updateViewLayout(ll, updatedParameters);

                    default:
                        break;
                }

                return false;
            }
        });
        ll.setOnTouchListener(new View.OnTouchListener() {
            WindowManager.LayoutParams updatedParameters = parameters;
            int orgX, orgY;
            int offsetX, offsetY;

            int orgWidth, orgHeight;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        orgX = (int) event.getRawX();
                        orgY = (int) event.getRawY();
                        orgWidth=v.getMeasuredWidth();
                        orgHeight=v.getMeasuredHeight();

                        break;
                    case MotionEvent.ACTION_MOVE:
                        offsetX = (int)event.getRawX() - orgX;
                        offsetY = (int)event.getRawY() - orgY;
                        updatedParameters.width=orgWidth+offsetX;
                        updatedParameters.height=orgHeight+offsetY;
                        flotingheight=flotingheight+updatedParameters.height;
                        flotingwidth=flotingwidth+updatedParameters.width;
                        //resize PopWindow
                        wm.updateViewLayout(ll,updatedParameters);
                        break;

                }
                return false;
            }
        });
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ll.setVisibility(View.INVISIBLE);
                dmp.getScreenshotPermission();
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                mDisplay = window.getDefaultDisplay();
                shouldCapture=true;
                createVirtualDisplay();
                dmp.returnmp().registerCallback(new MediaProjectionStopCallback(), null);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wm.removeView(ll);
                stopSelf();
                System.exit(0);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(tts!=null){
            tts.stop();
            tts.shutdown();
        }
        stopSelf();
    }

}