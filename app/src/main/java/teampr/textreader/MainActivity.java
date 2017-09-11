package teampr.textreader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE=100;
    private MediaProjectionManager mpm;
    private static MediaProjection mp;
    private VirtualDisplay vd;
    private static final String TAG = MainActivity.class.getName();
    DeliverMp dmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dmp=new DeliverMp();
        mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
       dmp.initdeliverM(mpm);
        startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_CODE);      //사진찍어도 되냐고 권한을 요청함
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {     //권한을 받아봄
        if(requestCode==REQUEST_CODE){
            if(Activity.RESULT_OK ==resultCode){
                dmp.setScreenshotPermission((Intent)data.clone());

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                }
            }
            else if(Activity.RESULT_CANCELED ==resultCode){
                dmp.setScreenshotPermission(null);
                Log.e(TAG,"no access");
            }
        }
        if(requestCode != REQUEST_CODE){
            Toast.makeText(this,
                    "unknow request code", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "unknow request code");
            return;
        }
        mp = mpm.getMediaProjection(resultCode, data);

        dmp.initializemp(mp);

        Log.e(TAG, "startservice");                                 //윈도우 창을 띄우기
        startService(new Intent(MainActivity.this,FloatingWindow.class));
        super.onActivityResult(requestCode, resultCode, data);
    }
}
