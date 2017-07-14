/*  NDKmol - Molecular Viewer on Android NDK

     (C) Copyright 2011 - 2012, biochem_fan

     This file is part of NDKmol.

     NDKmol is free software: you can redistribute it and/or modify
     it under the terms of the GNU Lesser General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.

     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU Lesser General Public License for more details.

     You should have received a copy of the GNU Lesser General Public License
     along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package ra.electrifierz.ChemAR;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.artoolkit.ar.base.ARActivity;
import org.artoolkit.ar.base.rendering.ARRenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NDKmolActivity extends ARActivity implements NavigationView.OnNavigationItemSelectedListener {
	public static final boolean GLES1 = true;//false;
	private String currentFilename;

	static {
		System.loadLibrary("ChemAR");
	}

	public NdkView view;
    public FrameLayout fl;

    NavigationView navigationView;
    DrawerLayout drawer;

	void initializeResource() {
		String targetPath = getDataDirectory() + "/2POR.pdb";
		Log.d("ChemAR", "Initializing sample data " + targetPath);
		File target = new File(targetPath);
		if (target.exists()) return;

		try {
			FileWriter out= new FileWriter(target);
			out.write(readResource(R.raw.initial));
			out.close();
		} catch (Exception e) {
			Log.d("initializeResource", "failed: " + e.toString());
		}
	}

	String readResource(int resourceId) {
		String ret = ""; 

		Resources res = this.getResources();
		InputStream st = null;
		try {
			st = res.openRawResource(resourceId);
			byte[] buffer = new byte[st.available()];
			while((st.read(buffer)) != -1) {}
			st.close();
			ret = new String(buffer);
		} catch (Exception e) {
			Log.d("ResourceOpen", e.toString());
		} finally{
		}
		return ret;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

        MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                .content("Loading...");
        MaterialDialog dialog = builder.build();
        dialog.show();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ndkmol);

		view = new NdkView();
        fl = (FrameLayout)findViewById(R.id.mainLayout);

		initializeResource();

        if (getIntent().getStringExtra("URI") != null) {
            Log.d("URI", getIntent().getStringExtra("URI"));
            readURI(getIntent().getStringExtra("URI"));
        } else {
            Toast.makeText(NDKmolActivity.this, "Opening 2POR", Toast.LENGTH_SHORT).show();
            openFile(getDataDirectory() + "/2POR.pdb");
        }

        drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
		navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

        initNav();
        drawer.openDrawer(Gravity.LEFT);

        SeekBar sbZoom = (SeekBar) findViewById(R.id.sbZoom);
        sbZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(view != null)
                    view.seekZoom = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        dialog.dismiss();
	}

    private void initNav() {
        if (view != null) {
            navigationView.getMenu().findItem(R.id.Sidechain).setChecked(view.showSidechain);
            navigationView.getMenu().findItem(R.id.Cell).setChecked(view.showUnitcell);
            navigationView.getMenu().findItem(R.id.Solvent).setChecked(view.showSolvents);
            navigationView.getMenu().findItem(R.id.doNotSmoothen).setChecked(view.doNotSmoothen);
            navigationView.getMenu().findItem(R.id.symopHetatms).setChecked(view.symopHetatms);

            switch (view.proteinMode) {
                case 0:
                    navigationView.getMenu().findItem(R.id.ThickRibbon).setChecked(true);
                    break;
                case 1:
                    navigationView.getMenu().findItem(R.id.Ribbon).setChecked(true);
                    break;
                case 2:
                    navigationView.getMenu().findItem(R.id.CA_trace).setChecked(true);
                    break;
                case 3:
                    navigationView.getMenu().findItem(R.id.Strand).setChecked(true);
                    break;
                case 4:
                    navigationView.getMenu().findItem(R.id.Tube).setChecked(true);
                    break;
                case 5:
                    navigationView.getMenu().findItem(R.id.Bonds).setChecked(true);
                    break;
                case 6:
                    navigationView.getMenu().findItem(R.id.MainchainNone).setChecked(true);
                    break;
            }

            switch (view.nucleicAcidMode) {
                case 0:
                    navigationView.getMenu().findItem(R.id.baseLine).setChecked(true);
                    break;
                case 1:
                    navigationView.getMenu().findItem(R.id.basePolygon).setChecked(true);
                    break;
                case 2:
                    navigationView.getMenu().findItem(R.id.baseNone).setChecked(true);
                    break;
            }

            switch (view.hetatmMode) {
                case 0:
                    navigationView.getMenu().findItem(R.id.Sphere).setChecked(true);
                    break;
                case 1:
                    navigationView.getMenu().findItem(R.id.Stick).setChecked(true);
                    break;
                case 2:
                    navigationView.getMenu().findItem(R.id.Line).setChecked(true);
                    break;
                case 3:
                    navigationView.getMenu().findItem(R.id.HetatmNone).setChecked(true);
                    break;
            }

            switch (view.symmetryMode) {
                case 0:
                    navigationView.getMenu().findItem(R.id.Monomer).setChecked(true);
                    break;
                case 1:
                    navigationView.getMenu().findItem(R.id.Biomt).setChecked(true);
                    break;
                case 2:
                    navigationView.getMenu().findItem(R.id.Packing).setChecked(true);
            }

            switch (view.colorMode) {
                case 0:
                    navigationView.getMenu().findItem(R.id.Chainbow).setChecked(true);
                    break;
                case 1:
                    navigationView.getMenu().findItem(R.id.Chain).setChecked(true);
                    break;
                case 2:
                    navigationView.getMenu().findItem(R.id.Structure).setChecked(true);
                    break;
                case 3:
                    navigationView.getMenu().findItem(R.id.Polarity).setChecked(true);
                    break;
                case 4:
                    navigationView.getMenu().findItem(R.id.BFactor).setChecked(true);
                    break;
            }
        }
    }

    @Override
	protected ARRenderer supplyRenderer() {
		return view;
	}

	@Override
	protected FrameLayout supplyFrameLayout() {
		return fl;
	}

	public void showHeader() {
		if (currentFilename == null) return;

		FileInputStream input;
		try {
			input = new FileInputStream(currentFilename);
			InputStreamReader reader = new InputStreamReader(input);
			int headerLength = 50000;
			char buffer[] = new char[headerLength];
			reader.read(buffer, 0, headerLength);
			String header = new String(buffer);
			reader.close();
			alert("First 50KB of the file: \n\n" + header);
		} catch (Exception e) {
		}
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

	public void alert(String msg) {
        new MaterialDialog.Builder(this)
                .content(msg)
                .positiveText("OK")
                .show();
	}

	public void readURI(String uri) {
		String scheme = uri.substring(0, 7);
		if (scheme.equals("file://")) {
			openFile(uri.substring(7));
		} else if (scheme.equals("http://")) {
			String fileName = uri.substring(uri.lastIndexOf('/') + 1);
			if (uri.contains("record_type=3d")) {
				int start = "http://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/".length();
                int index = uri.indexOf("/record/SDF/?record_type=3d");
				if (index < 0) return;

				fileName = uri.substring(start, index) + ".sdf";
			}
            new Downloader(this, uri, getDataDirectory() + "/" + fileName);
		} else if (scheme.equals("https:/")) {
            String fileName = uri.substring(uri.lastIndexOf('/') + 1);
            if (uri.contains("record_type=3d")) {
                int start = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/".length();
                int index = uri.indexOf("/record/SDF/?record_type=3d");
                if (index < 0) return;

                fileName = uri.substring(start, index) + ".sdf";
            }
            new Downloader(this, uri, getDataDirectory() + "/" + fileName);
        }
	}

	void openFile(String filePath) {
		if (filePath.toUpperCase().endsWith("PDB")) {
			view.loadPDB(filePath);
			currentFilename = filePath;
		} else if (filePath.toUpperCase().endsWith("SDF") || filePath.toUpperCase().endsWith("MOL")) {
			view.loadSDF(filePath);
			currentFilename = filePath;
		} else if (filePath.toUpperCase().endsWith("CCP4") || filePath.toUpperCase().endsWith("CCP4.GZ")) {
			view.loadCCP4(filePath);
		} else {
			alert(getString(R.string.unknownFileType));
		}
        if(super.getGLView()!=null) {
            super.getGLView().requestRender();
        }else{
            Log.e("ACTIVITY", "Null GLView");
        }
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
        boolean ret = true;
        switch (item.getItemId()) {
            case R.id.ThickRibbon:
                view.proteinMode = 0;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Ribbon:
                view.proteinMode = 1;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.CA_trace:
                view.proteinMode = 2;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Strand:
                view.proteinMode = 3;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Tube:
                view.proteinMode = 4;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Bonds:
                view.proteinMode = 5;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.MainchainNone:
                view.proteinMode = 6;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.baseLine:
                view.nucleicAcidMode = 0;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.basePolygon:
                view.nucleicAcidMode = 1;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.baseNone:
                view.nucleicAcidMode = 2;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.Monomer:
                view.symmetryMode = 0;
                item.setChecked(true);
                view.prepareScene();
                view.resetCamera();
                super.getGLView().requestRender();
                break;
            case R.id.Biomt:
                view.symmetryMode = 1;
                item.setChecked(true);
                view.prepareScene();
                view.resetCamera();
                super.getGLView().requestRender();
                break;
            case R.id.Packing:
                view.symmetryMode = 2;
                item.setChecked(true);
                view.prepareScene();
                view.resetCamera();
                super.getGLView().requestRender();
                break;

            case R.id.Chainbow:
                view.colorMode = 0;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Chain:
                view.colorMode = 1;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Structure:
                view.colorMode = 2;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Polarity:
                view.colorMode = 3;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.BFactor:
                view.colorMode = 4;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.Sphere:
                view.hetatmMode = 0;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Stick:
                view.hetatmMode = 1;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.Line:
                view.hetatmMode = 2;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;
            case R.id.HetatmNone:
                view.hetatmMode = 3;
                item.setChecked(true);
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.Sidechain:
                item.setChecked(!item.isChecked());
                view.showSidechain = !view.showSidechain;
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.Cell:
                item.setChecked(!item.isChecked());
                view.showUnitcell = !view.showUnitcell;
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.Solvent:
                item.setChecked(!item.isChecked());
                view.showSolvents = !view.showSolvents;
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.doNotSmoothen:
                item.setChecked(!item.isChecked());
                view.doNotSmoothen = !view.doNotSmoothen;
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.symopHetatms:
                item.setChecked(!item.isChecked());
                view.symopHetatms = !view.symopHetatms;
                view.prepareScene();
                super.getGLView().requestRender();
                break;

            case R.id.showHeader:
                showHeader();
        }
        return ret;
	}

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}