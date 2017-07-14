package ra.electrifierz.ChemAR;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

public class MenuActivity extends AppCompatActivity implements View.OnClickListener {

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_menu);

        LinearLayout llScan = (LinearLayout)findViewById(R.id.llScan);
        llScan.setOnClickListener(this);
        LinearLayout llOpen = (LinearLayout)findViewById(R.id.llOpen);
        llOpen.setOnClickListener(this);
        LinearLayout llSearch = (LinearLayout)findViewById(R.id.llSearch);
        llSearch.setOnClickListener(this);
        LinearLayout llHelp = (LinearLayout)findViewById(R.id.llHelp);
        llHelp.setOnClickListener(this);
        LinearLayout llAbout = (LinearLayout)findViewById(R.id.llAbout);
        llAbout.setOnClickListener(this);
    }

    String getDataDirectory() {
        String dataDir = this.getFilesDir().getAbsolutePath();
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // Although document says MEDIA_MOUNTED means a WRITABLE media exists,
            //   readonly SDcards still result in MEDIA_MOUNTED.
            File writeTest = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/.writeTest");
            if (writeTest.exists()) writeTest.delete();
            if (writeTest.mkdir()) {
                writeTest.delete();
                File sdfolder = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/PDB");
                sdfolder.mkdir();
                dataDir = sdfolder.getAbsolutePath();
            }
        }
        Log.d("ChemAR", "Data dir is " + dataDir);
        return dataDir;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.llScan:
                Intent intent0 = new Intent(MenuActivity.this, ScanQR.class);
                intent0.setData(Uri.parse("file://" + getDataDirectory()));
                startActivity(intent0);
                break;
            case R.id.llOpen:
                Intent intent1 = new Intent(MenuActivity.this, FileBrowser.class);
                intent1.setData(Uri.parse("file://" + getDataDirectory()));
                startActivity(intent1);
                break;
            case R.id.llSearch:
                Intent intent2 = new Intent(MenuActivity.this, Searcher.class);
                startActivity(intent2);
                break;
            case R.id.llHelp:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://electrifierz.000webhostapp.com/chemar/"));
                startActivity(browserIntent);
                break;
            case R.id.llAbout:
                new MaterialDialog.Builder(MenuActivity.this)
                        .title(getString(R.string.app_name))
                        .content(getString(R.string.about))
                        .positiveText("OK")
                        .show();
                break;
        }
    }
}
