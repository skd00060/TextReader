package teampr.textreader;

import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;

/**
 * Created by CHO HYEONIN on 2017-05-31.
 */

public class TessOCR {
    private TessBaseAPI mTess;

    public TessOCR() {
        // TODO Auto-generated constructor stub



        mTess = new TessBaseAPI();
        // AssetManager assetManager=
        String datapath = Environment.getExternalStorageDirectory() + "/DemoOCR/";
        String language = "kor";
        // AssetManager assetManager = getAssets();
        File dir = new File(datapath + "/tessdata");
        if (!dir.exists())
            dir.mkdirs();
        mTess.init(datapath, language);
    }

    public String getOCRResult(Bitmap bitmap) {

        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();

        return result;
    }

    public void onDestroy() {
        if (mTess != null)
            mTess.end();
    }

}
