/*  NDKmol - Molecular Viewer for Android

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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileBrowser extends AppCompatActivity {

	private ListView listView = null;
	private List<Map<String, String>> dataList;
	private FileBrowser self;
	private String currentPath, selectedFile;
	private SimpleAdapter adapter;


	private List<Map<String,String>> getFileList(String path) {
		List<Map<String,String>> ret = new ArrayList<Map<String,String>>();

		File dir = new File(path);
		File[] files = dir.listFiles();
		Log.d("FileBrowser", dir.toURI().toString());

		for (int i = 0, lim = files.length; i < lim; i++) {
			String	name = "", title = ""; 
			try {
				HashMap<String, String> records = new HashMap<String, String>();
				File file = files[i];
				name = file.getName();
				String upperCased = name.toUpperCase();
				if (upperCased.endsWith("PDB")) {
					FileInputStream input = new FileInputStream(file);
					InputStreamReader reader = new InputStreamReader(input);
					int headerLength = 300;
					char buffer[] = new char[headerLength];
					reader.read(buffer, 0, headerLength);
					String header[] = new String(buffer).split("\n");
					for (int j = 0; j < header.length; j++) {
						if (header[j].startsWith("TITLE") && header[j].length() > 11) {
							title += header[j].substring(10).replace("  ", "");
						}
					}
					reader.close();
				} else if (upperCased.endsWith("SDF") || upperCased.endsWith("MOL")) {
					title = "SDF/MOL file";
				} else if (upperCased.endsWith("CCP4") || upperCased.endsWith("CCP4.GZ")) {
					title = "Electron density file (CCP4/MRC format)";
				} else {
					continue;
				}

				records.put("fileName", name);
				records.put("structureTitle", title);
				ret.add(records);
			} catch (Exception e) {

			}
		}
		return ret;
	}

	private void setFileList() {
		dataList = getFileList(currentPath);
		adapter = new SimpleAdapter(
				self,
				dataList,
				android.R.layout.simple_list_item_2,
				new String[] { "fileName", "structureTitle"},
				new int[] { android.R.id.text1, android.R.id.text2 }
		);
		listView.setAdapter(adapter);
	}
	
	private void deleteSelectedFile() {
		new MaterialDialog.Builder(this)
				.content("Are you sure to delete " + selectedFile + "?")
				.positiveText("YES")
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						try {
							File file = new File(currentPath + selectedFile);
							file.delete();
						} catch (Exception e) {
							new MaterialDialog.Builder(FileBrowser.this)
									.content("Failed to delete " + selectedFile)
									.positiveText("OK")
									.show();
						}
						setFileList();
					}
				})
				.negativeText("NO")
				.show();
	}
		
	private void openSelectedFile() {
		Intent i = new Intent(FileBrowser.this, NDKmolActivity.class);
		i.putExtra("URI", Uri.parse("file://" + currentPath + selectedFile).toString());
		startActivity(i);
		finish();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		self = this;
		setContentView(R.layout.filebrowser);
		String tmp = getIntent().getDataString();
		Log.d("FileBrowser", tmp);
		currentPath = tmp.substring(7) + "/"; // file:// TODO: Error handling

		listView = (ListView)findViewById(R.id.searchResults);
		dataList = null;

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Map<String, String> item = (Map<String, String>) listView.getItemAtPosition(i);
                selectedFile = item.get("fileName");
                new MaterialDialog.Builder(FileBrowser.this)
                        .title(selectedFile)
                        .items(R.array.open_delete_array)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                switch (which) {
                                    case 0:
                                        openSelectedFile();
                                        break;
                                    case 1:
                                        deleteSelectedFile();
                                        break;
                                }
                            }
                        }).show();
                return true;
            }
        });

		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Map<String, String> item = (Map<String, String>) listView.getItemAtPosition(position);
				selectedFile = item.get("fileName");
				openSelectedFile();
			}
		});

		setFileList();
	}
}