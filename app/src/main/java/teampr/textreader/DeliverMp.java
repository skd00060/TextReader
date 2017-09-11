package teampr.textreader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;

/**
 * Created by CHO HYEONIN on 2017-05-28.
 */

public class DeliverMp {
    private static MediaProjection delivermp;
    private static Intent screenshotPermission = null;
    private static MediaProjectionManager deliverManger;
    public DeliverMp(){
    }
    protected void initdeliverM(MediaProjectionManager inputM){
        deliverManger=inputM;
    }
    protected static void getScreenshotPermission(){
        try{
            if(screenshotPermission != null){
                if(delivermp != null){
                    delivermp.stop();
                    delivermp =null;
                }
                delivermp = deliverManger.getMediaProjection(Activity.RESULT_OK,(Intent)screenshotPermission.clone());
            }else {
                openScreenshotPermissionRequest();
            }
        }catch(final RuntimeException ignored){
            openScreenshotPermissionRequest();
        }
    }
    protected static void setScreenshotPermission(final Intent permissionIntent){
        screenshotPermission =permissionIntent;
    }
protected static void openScreenshotPermissionRequest(){
    Context context = null;
    final Intent intent = new Intent(context, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
}
    protected void initializemp(MediaProjection inputmp){
        delivermp = inputmp;
    }
    protected  MediaProjection returnmp(){
        return delivermp;
    }


}
