/*  NDKmol - Molecular Viewer on Android NDK

     (C) Copyright 2011 - 2012, biochem_fan

     This file is part of Androidmol.

     Androidmol is free software: you can redistribute it and/or modify
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

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.apache.http.HttpException;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader {
	String uri;
	String dest;
	final int SUCCESS = 0;
	final int ERROR = -1;
	final int NO3DSDF = -2;
	final int CANCELED = -3;


	NDKmolActivity parent; // FIXME: this is not very good solution....
	
	public Downloader(NDKmolActivity parent, String uri, String dest) {
		Log.d("Downloader", "From " + uri + " To " + dest);

		this.uri = uri;
		this.dest = dest;
		this.parent = parent;
		
		String[] tmp = {};
		new DownloadTask().execute(tmp);
	}
	
	public class DownloadTask extends AsyncTask<String, Integer, Integer> {
		boolean isKilled = false;
		MaterialDialog progress = null;
		
		@Override
		protected void onPreExecute() {
			progress = new MaterialDialog.Builder(parent)
					.title(parent.getString(R.string.downloading))
					.content(parent.getString(R.string.pleaseWait))
					.progress(true, 0)
                    .negativeText("CANCEL")
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            isKilled = true;
							dialog.cancel();
                        }
                    })
					.show();
		}

		@Override
		protected Integer doInBackground(String... dummy) {
            int ret = SUCCESS;

			try {			
				URL url = new URL(uri);
				HttpURLConnection httpConn;

                httpConn = (HttpURLConnection)url.openConnection();
				httpConn.setInstanceFollowRedirects(true);
				httpConn.setRequestMethod("GET");
				httpConn.connect();

                DataInputStream inp;
                FileOutputStream out;
                byte[] buffer = new byte[10000];

				if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    inp = new DataInputStream(httpConn.getInputStream());
                    out = new FileOutputStream(dest);
                    int read;

                    boolean first = true;
                    while ((read = inp.read(buffer)) > 0) {
                        if (isKilled) return CANCELED;
                        out.write(buffer, 0, read);
                    }

                    inp.close();
                    out.close();
                    httpConn.disconnect();
                } else {
                    inp = new DataInputStream(httpConn.getErrorStream());
                    inp.read(buffer);

                    String str = new String(buffer);
                    if (str.indexOf("PUGREST.NotFound") != -1) ret = NO3DSDF;
                    else throw new HttpException("File not found.");
                }

			} catch (Exception e) {
				e.printStackTrace();
				Log.d("Downloader", "failed " + e.getMessage());
				Log.d("Downloader", "failed " + e.toString());
				Log.d("Downloader", "failed " + e.getStackTrace()[0].getClassName() + e.getStackTrace()[0].getLineNumber());
				ret = ERROR;
			}
			return ret;
		}

		@Override
		protected void onPostExecute(Integer result) {
			progress.dismiss();
			if (result == SUCCESS) {
				parent.readURI("file://" + dest);
				return;
			} else if (result == NO3DSDF) {
				parent.alert(parent.getString(R.string.no3DSDF));
			} else if (result == ERROR) {
				parent.alert(parent.getString(R.string.downloadError));
			} else if (result == CANCELED) {
			}
			File output = new File(dest);

			output.delete();
		}
	}
}
