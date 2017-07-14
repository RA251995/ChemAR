package ra.electrifierz.ChemAR;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ScanQR extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CAMERA = 123;
    SurfaceView cameraView;
    TextView barcodeInfo;
    CameraSource cameraSource;
    BarcodeDetector barcodeDetector;
    FloatingActionButton fabQR;
    String url = null;

    @Override
    public void onBackPressed() {
        cameraSource.stop();
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        cameraView = (SurfaceView) findViewById(R.id.camera_view1);
        barcodeInfo = (TextView) findViewById(R.id.code_info1);
        fabQR = (FloatingActionButton) findViewById(R.id.fabQR);

        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();

        SharedPreferences settings = getSharedPreferences("PREF", Context.MODE_PRIVATE);
        int cam_width = settings.getInt("WIDTH", 800);
        int cam_height = settings.getInt("HEIGHT", 480);
        cameraSource = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(cam_width, cam_height).build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ContextCompat.checkSelfPermission(ScanQR.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(ScanQR.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                    }
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    barcodeInfo.post(new Runnable() {    // Use the post method of the TextView
                        public void run() {
                            String qrcodeContent = barcodes.valueAt(0).displayValue;
                            parseJson(qrcodeContent);
                        }
                    });
                }
            }
        });

        fabQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraSource.stop();
                if(url != null) {
                    Intent i = new Intent(ScanQR.this, NDKmolActivity.class);
                    i.putExtra("URI", Uri.parse(url).toString());
                    startActivity(i);
                    finish();
                }
            }
        });
    }

    private void parseJson(String jsonStr) {
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            String name = jsonObj.getString("name");
            url = jsonObj.getString("uri");
            barcodeInfo.setText(name);
            fabQR.setVisibility(View.VISIBLE);
        } catch (final JSONException e) {
            url = null;
            barcodeInfo.setText("Invalid QR Code");
            fabQR.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: {
                Log.i("Camera", "G : " + grantResults[0]);
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    return;
                } else {
                    // Permission Denied
                    Toast.makeText(ScanQR.this, "Cannot continue without camera permission", Toast.LENGTH_LONG).show();
                    this.finishAffinity();
                }
            }
        }
    }
}
