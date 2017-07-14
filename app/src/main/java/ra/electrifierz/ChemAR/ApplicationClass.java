package ra.electrifierz.ChemAR;

import android.app.Application;

import org.artoolkit.ar.base.assets.AssetHelper;

public class ApplicationClass extends Application {

    private static Application sInstance;

    // Anywhere in the application where an instance is required, this method
    // can be used to retrieve it.
    public static Application getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        ((ApplicationClass) sInstance).initializeInstance();
    }

    // Here we do one-off initialisation which should apply to all activities
    // in the application.
    protected void initializeInstance() {

        // Unpack assets to cache directory so native library can read them.
        // N.B.: If contents of assets folder changes, be sure to increment the
        // versionCode integer in the AndroidManifest.xml file.
        AssetHelper assetHelper = new AssetHelper(getAssets());
        assetHelper.cacheAssetFolder(getInstance(), "Data");
    }

    private int optimalCameraWidth, optimalCameraHeight;

    public int getOptimalCameraWidth() {
        return optimalCameraWidth;
    }

    public int getOptimalCameraHeight() {
        return optimalCameraHeight;
    }

    public void setOptimalCameraWidth(int optimalCameraWidth) {
        this.optimalCameraWidth = optimalCameraWidth;
    }

    public void setOptimalCameraHeight(int optimalCameraHeight) {
        this.optimalCameraHeight = optimalCameraHeight;
    }
}

