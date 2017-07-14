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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Searcher extends AppCompatActivity {

	private ListView listView = null;
    private SearchView sv;
	private String pdbSearchString = "<orgPdbQuery><queryType>org.pdb.query.simple.MoleculeNameQuery</queryType><macromoleculeName>#KEYWORD#</macromoleculeName></orgPdbQuery>";
	private String pdbRestURI = "http://www.rcsb.org/pdb/rest/search/";
	private String pdbDetailSearchURI = "http://www.rcsb.org/pdb/rest/customReport?pdbids=#PDBID#&customReportColumns=structureId,structureTitle,experimentalTechnique,depositionDate,releaseDate,ndbId,resolution,structureAuthor&format=xml";
	private List<Map<String, String>> dataList;
	private int MAXRESULT = 100;
	private Searcher self;
	private Proxy proxy;

    private String pubchemSearchURI = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pccompound&retmax=#MAXRESULT#&term=#KEYWORD#";
    private String pdbchemDetailURI = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pccompound&id=#IDs#";
    private String pubchemDownloadURI = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/#ID#/record/SDF/?record_type=3d";
    private int MAXSYNONYMS = 5;

    private FloatingActionButton fabSearch;
    private int SOURCE = 0;

    private ArrayList<String> queryPDBforIDs(String keyword) {
		ArrayList<String> ids = new ArrayList<String>();

		if (keyword.matches("^[a-zA-Z0-9]{4}$")) { // seems to be PDBID. 
			ids.add(keyword); // non-existing PDB ID is simply ignored so try adding it.
		}

		try {
			URL url = new URL(pdbRestURI);
			HttpURLConnection conn;
			if (proxy != null) {
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}
			conn.setDoOutput(true);

			OutputStream os = conn.getOutputStream();
			String queryStr = pdbSearchString.replaceFirst("#KEYWORD#", keyword);
			PrintStream ps = new PrintStream(os);
			ps.print(queryStr);
			ps.close();

			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
			int cnt = 0;
			while ((line = reader.readLine()) != null) {
				ids.add(line);
				if (cnt++ > MAXRESULT) {
					Toast.makeText(this, getString(R.string.tooManyHits), Toast.LENGTH_LONG).show();
					break; 
				}
			}
			reader.close();
		} catch (Exception e) {
			Log.d("queryPDB", e.toString());
		}
		return ids;
	}

	private List<Map<String,String>> queryPDBforDetails(ArrayList<String> ids) {
		List<Map<String,String>> ret = new ArrayList<Map<String,String>>();
		String joined = "";
		StringBuffer sb = new StringBuffer();

		int lim = ids.size();
		if (lim > MAXRESULT) lim = MAXRESULT;
		for (int i = 0; i < lim; i++) joined += ids.get(i) + ","; // final ',' doesn't harm

		try {
			URL url = new URL(pdbDetailSearchURI.replaceFirst("#PDBID#", joined));
			HttpURLConnection conn;
			if (proxy != null) {
				conn = (HttpURLConnection) url.openConnection(proxy);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}

			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;

			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			reader.close();
		} catch (Exception e) {
			Log.d("queryPDB", e.toString());
		}

		String[] entries = sb.toString().split("</record>");
		String[] fields = {"structureId", "structureTitle", "resolution", "structureAuthor", "releaseDate"};
		for (int i = 0, ilim = entries.length; i < ilim; i++) {
			String entry = entries[i];
			HashMap<String, String> records = new HashMap<String, String>();
			for (int j = 0, jlim = fields.length; j < jlim; j++) {
				String startTag = "<dimStructure." + fields[j] + ">";
				String endTag = "</dimStructure." + fields[j] + ">";
				int lindex = entry.indexOf(startTag);
				int rindex = entry.indexOf(endTag);
				if (lindex < 0 || rindex < 0) continue;
				lindex += startTag.length();
				String data = entry.substring(lindex, rindex);
				records.put(fields[j], data);
			}
			if (records.containsKey("structureId")) ret.add(records);
		}

		return ret;
	}

    private ArrayList<String> queryPubchemforIDs(String keyword) {
        ArrayList<String> ids = new ArrayList<String>();
        StringBuffer sb = new StringBuffer();

        try {
            keyword = URLEncoder.encode(keyword, "UTF-8");
            URL url = new URL(pubchemSearchURI.replaceFirst("#KEYWORD#", keyword).replaceFirst("#MAXRESULT#", Integer.toString(MAXRESULT)));

            HttpURLConnection conn;
            if (proxy != null) {
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
        } catch (Exception e) {
            Log.d("queryPubChem", e.toString());
        }

        int pos = 0, lindex, rindex;
        while ((lindex = sb.indexOf("<Id>", pos)) > 0) {
            lindex += 4;
            rindex = sb.indexOf("</Id>", lindex);
            if (rindex < 0) break; // Something is wrong!
            ids.add(sb.substring(lindex, rindex));
            pos = rindex + 1;
        }

        Log.d("Pubchem", ids.toString());
        return ids;
    }

    private List<Map<String,String>> queryPubchemforDetails(ArrayList<String> ids) {
        List<Map<String,String>> ret = new ArrayList<Map<String,String>>();
        String joined = "";
        StringBuffer sb = new StringBuffer();

        int lim = ids.size();
        if (lim > MAXRESULT) lim = MAXRESULT;
        for (int i = 0; i < lim; i++) joined += ids.get(i) + ","; // final ',' doesn't harm

        try {
            URL url = new URL(pdbchemDetailURI.replaceFirst("#IDs#", joined));
            HttpURLConnection conn;
            if (proxy != null) {
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
        } catch (Exception e) {
            Log.d("queryPubChem", e.toString());
        }

        String[] entries = sb.toString().split("</DocSum>");
        for (int i = 0, ilim = entries.length; i < ilim; i++) {
            String entry = entries[i];
            HashMap<String, String> records = new HashMap<String, String>();

            int idL = entry.indexOf("<Id>");
            int idR = entry.indexOf("</Id>");
            if (idL < 0 || idR < 0) continue;
            idL += 4;
            String id = entry.substring(idL, idR);
            String synonyms = "";

            int pos = entry.indexOf("<Item Name=\"SynonymList\" Type=\"List\">");
            int lindex, rindex, cnt = 0;
            while ((lindex = entry.indexOf("<Item Name=\"string\" Type=\"String\">", pos)) > 0) {
                lindex += 34;
                rindex = entry.indexOf("</Item>", lindex);
                synonyms += entry.substring(lindex, rindex) + " ";
                pos = rindex + 1;
                int peek = entry.indexOf("</Item>", rindex + 1);
                if (peek - rindex < 30) break;
                if (cnt++ > MAXSYNONYMS) break;
            }
            records.put("structureId", id);
            records.put("structureTitle", synonyms);
            ret.add(records);
        }
        return ret;
    }


    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		self = this;
		setContentView(R.layout.searcher);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
		if (prefs.getBoolean(self.getString(R.string.useProxy), false)) {
			String proxyAddress = prefs.getString(self.getString(R.string.proxyHost), "");
			int proxyPort = Integer.parseInt(prefs.getString(self.getString(R.string.proxyPort), "8080"));

			SocketAddress addr = new InetSocketAddress(proxyAddress, proxyPort);
			proxy = new Proxy(Proxy.Type.HTTP, addr);
		} else {
			proxy = null;
		}

		listView = (ListView)findViewById(R.id.searchResults);

        sv = (SearchView) findViewById(R.id.sv);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String[] tmp = {sv.getQuery().toString()};
                new SearchTask().execute(tmp);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        fabSearch = (FloatingActionButton)findViewById(R.id.fabSearch);
        fabSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(SOURCE == 0){
                    SOURCE = 1;
                    sv.setQueryHint("Search PubChem");
                    Toast.makeText(Searcher.this, "Search Source: PubChem", Toast.LENGTH_SHORT).show();
                }else{
                    SOURCE = 0;
                    sv.setQueryHint("Search PDB");
                    Toast.makeText(Searcher.this, "Search Source: PDB", Toast.LENGTH_SHORT).show();
                }
            }
        });

		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
                Map<String, String> item = (Map<String, String>) listView.getItemAtPosition(position);
                String model_id = item.get("structureId");
                String title = item.get("structureTitle");
                String strDetails = null;
                Uri uri = null;
                if (SOURCE == 0){
                    strDetails = "ID: " + model_id
                            + "\nTitle: " + title;
                    String resolution = item.get("resolution");
                    if (resolution == null || resolution.equals("null")) { // NMR structures
                        resolution = "N/A";
                    }
                    strDetails += "\nResolution: " + resolution
                            + "\nAuthors: " + item.get("structureAuthor")
                            + "\nRelease Date: " + item.get("releaseDate");

                    uri = Uri.parse("http://files.rcsb.org/download/" + model_id.toUpperCase() + ".pdb");
                }else{
                    strDetails = "ID: " + model_id
                            + "\nTitle: " + title;
                    uri = Uri.parse(pubchemDownloadURI.replaceFirst("#ID#", model_id));
                }

                final Uri finalUri = uri;
                new MaterialDialog.Builder(Searcher.this)
                        .title("Structure details")
                        .content(strDetails)
                        .positiveText(getString(R.string.download))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                Intent i = new Intent(Searcher.this, NDKmolActivity.class);
                                i.putExtra("URI", finalUri.toString());
                                startActivity(i);
                                finish();
                            }
                        })
                        .negativeText(getString(R.string.cancel))
                        .show();

			}
		});
	}

	public class SearchTask extends AsyncTask<String, Integer, Boolean> {
		MaterialDialog progress;

		@Override
		protected void onPreExecute() {
            progress = new MaterialDialog.Builder(Searcher.this)
                    .content("Please Wait")
                    .progress(true, 0)
                    .show();
		}

		@Override
		protected Boolean doInBackground(String... searchFor) {
            if(SOURCE == 0) {
                ArrayList<String> ids = queryPDBforIDs(searchFor[0]);
                dataList = queryPDBforDetails(ids);
            }else{
                ArrayList<String> ids = queryPubchemforIDs(searchFor[0]);
                dataList = queryPubchemforDetails(ids);
            }
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			SimpleAdapter adapter = new SimpleAdapter(
					self,
					dataList,
					android.R.layout.simple_list_item_2,
					new String[] { "structureId", "structureTitle"},
					new int[] { android.R.id.text1, android.R.id.text2 }
			);
			listView.setAdapter(adapter);
			progress.dismiss();
		}
	}
}