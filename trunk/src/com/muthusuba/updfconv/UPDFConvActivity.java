/*
Copyright (C) Muthu Subramanian K <muthusuba@gmail.com> 2011

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/
*/
package com.muthusuba.updfconv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.Xml;
import android.util.Xml.Encoding;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.ads.*;

public class UPDFConvActivity extends Activity {
	private static String deviceID;
	private static String filename = "";
	private static String uniquekey = "";
	private static String error = null;
	private static String status = null;
	private static String updateStatus = "";
	private static Button choosebutton;
	private static TextView statustext;
	private static SharedPreferences pref;
	private static boolean statustimer = false;
	private static AdView[] adviews  = new AdView[5]; 

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	if(!isOnline()) {
    		new AlertDialog.Builder(this)
    	    .setMessage("This app requires internet connection.\nPlease verify that you are connected.\nThank you.")
    	    .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {                
    	        @Override
    	        public void onClick(DialogInterface dialog, int which) {
    	    		finish();
    	        }
    	    })
    	    .show();
    	}
        setContentView(R.layout.main);
        LinearLayout layout = (LinearLayout) findViewById(R.id.admob);
        choosebutton = (Button)findViewById(R.id.choosebutton);
        choosebutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			} 
        });
        choosebutton.setVisibility(View.INVISIBLE);
        statustext = (TextView)findViewById(R.id.statustext);
        restoreData();
        
        for(int i=0;i<adviews.length;i++) {
        	adviews[i] = new AdView(this, AdSize.BANNER, __AD_REQ_KEY__);
        	layout.addView(adviews[i]);
        	AdRequest adRequest = new AdRequest();
        	//adRequest.setTesting(true);
        	adviews[i].loadAd(adRequest);
        }
        
        if(filename.length() != 0) {
        	statustext.setText(filename+"\nGetting status...");
        	choosebutton.setVisibility(View.INVISIBLE);
        }

        Intent intent = getIntent();
        if(intent!=null && intent.getType()!=null) {
        	if(intent.getType().contains("pdf"))
        		finish();
        }
        if(filename.length() == 0 && intent != null && intent.getData() != null) {
        	String name = "";
        	try {
        		name = getRealPathFromURI(intent.getData());
        	}
        	catch(Exception e) {
        		name = intent.getData().getPath();
        	}
        	File file = new File(name);
        	if(file.isDirectory())
        		finish();
        	filename = name;
        	uploadFile();
        }
        if(!statustimer) {
	        new Thread(new Runnable() {
	            public void run() {
	            	while(true) {
	            		doWork();
	            		try { 
	            			Thread.sleep(15000);
	            		} catch(Exception e) { return; }
	            	}
	              }
	            }).start();
	        statustimer = true;
        }
    }
    
    private boolean isOnline() {
    	ConnectivityManager cmgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	return cmgr.getActiveNetworkInfo() != null && cmgr.getActiveNetworkInfo().isConnected();
    }
    
    private void updateAds() {
    	/*for(int i=0;i<adviews.length;i++)  {
			AdRequest adRequest = new AdRequest();
			//adRequest.setTesting(true);
		    adviews[i].loadAd(adRequest);
    	}*/
    }
    
    private void doWork() {
    	// ---- Update Ads ----
    	adviews[0].post(new Runnable() {
			@Override
			public void run() {
	    		updateAds();
			}
    	});
    	// --------------------
        if(filename.length() == 0) {
        	// Show Picker
        	;
        }
        else if(uniquekey.length()!=0) {
        	getStatus();
        }	
    }
    
    private void uploadFile() {
    	String params="id="+URLEncoder.encode(deviceID);
    	String boundary = "xupdf342679942342242466890-88xxxxxxxxxxx";
    	String fullpath = filename;
    	int len;
    	byte[] buffer = new byte[1024];
    	File file = new File(fullpath);
    	filename = file.getName();
    	try {
    	URL url = new URL("http://muthusuba.co.cc/updf/upload.php");
    	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	StringBuffer requestBody = new StringBuffer();
    	StringBuffer requestTail = new StringBuffer();
    	requestBody.append("--");
    	requestBody.append(boundary);
        requestBody.append('\n');
        requestBody.append("Content-Disposition: form-data; name=\"MAX_FILE_SIZE\"");
        requestBody.append('\n');
        requestBody.append('\n');
        requestBody.append("5000000");
        requestBody.append('\n');
        requestBody.append("--");
        requestBody.append(boundary);
        requestBody.append('\n');
        requestBody.append("Content-Disposition: form-data; name=\"id\"");
        requestBody.append('\n');
        requestBody.append('\n');
        requestBody.append(deviceID);
        requestBody.append('\n');
        requestBody.append("--");
        requestBody.append(boundary);
        requestBody.append('\n');
        requestBody.append("Content-Disposition: form-data; name=\"userfile\"; filename=\"" + filename + "\"");
        requestBody.append('\n');
        requestBody.append("Content-Type: application/octet-stream");
        requestBody.append('\n');
        requestBody.append('\n');
        //----
        requestTail.append('\n');
        requestTail.append("--");
        requestTail.append(boundary);
        requestTail.append("--");
        requestTail.append('\n');
        
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.connect();
        DataOutputStream dataOS = new DataOutputStream(conn.getOutputStream());
        dataOS.writeBytes(requestBody.toString());
        FileInputStream is = new FileInputStream(file);
        while((len=is.read(buffer))>0) {
        	dataOS.write(buffer,0,len); //Bytes(new String(buffer, 0, len).toString());
        }
        dataOS.writeBytes(requestTail.toString());
        dataOS.flush();
        dataOS.close();
        int respcode = conn.getResponseCode();
        if(respcode == 200) {
			if(conn.getHeaderField("Content-type").contains("xml")) {	// Status update
				DataInputStream ir = new DataInputStream(conn.getInputStream());
				parseStatus(ir);
				if(error!=null) {
					updateStatus = "Processing Error\n"+error;
					resetData();
				} else {
					updateStatus=status;
		        	saveData();
				}
				ir.close();
				// Update Status
				statustext.post(new Runnable() {
					@Override
					public void run() {
						statustext.setText(updateStatus);
					}
				});
			}
        }
        conn.disconnect();
    	} catch(Exception e) { }
    }
    
    private void saveData() {
    	if(filename==null) filename ="";
    	if(uniquekey==null) uniquekey="";
    	if(filename.length() == 0 && uniquekey.length() != 0)
    		pref.edit().putString("uniquekey","").putString("filename", "").commit();
    	else
    		pref.edit().putString("uniquekey",uniquekey).putString("filename", filename).commit();
    }
    
    private void restoreData() {
    	pref = getPreferences(MODE_PRIVATE);
        uniquekey = pref.getString("uniquekey", "");
        filename = pref.getString("filename", "");
        deviceID = pref.getString("deviceid", "");
        if(deviceID.length() < 1) {
        	getNewDeviceID();
        	pref.edit().putString("deviceid", deviceID).commit();
        }
    }
    
    private void getNewDeviceID() {
    	deviceID = Secure.getString(getApplicationContext().getContentResolver(),
                Secure.ANDROID_ID);
        if(deviceID == null)
        	deviceID = "123456"+String.valueOf((new Random()).nextLong());
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	finish();
    }
    
    /*@Override
    public void onResume() {
    	super.onResume();
    	//restoreData();
    }*/
    
    private void getStatus() {
    	String outfile = null;
    	try {
    	String params="id="+URLEncoder.encode(deviceID)+"&fileid="+URLEncoder.encode(uniquekey);
		HttpURLConnection connection = (HttpURLConnection) new URL("http://muthusuba.co.cc/updf/getfile.php").openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(true); 
		connection.setRequestMethod("POST"); 
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length", String.valueOf(params.getBytes().length));
		connection.setUseCaches(false);
		connection.connect();
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(params);
		wr.flush();
		wr.close();
		if(connection.getResponseCode() == 200) {
			DataInputStream ir = new DataInputStream(connection.getInputStream());
			if(connection.getHeaderField("Content-type").contains("xml")) {	// Status update
				parseStatus(ir);
				if(error!=null) {
					updateStatus = "Processing Error\n"+error;
					resetData();
				} else {
					updateStatus = status;
				}
			}
			else {															// Download
				outfile = Environment.getExternalStorageDirectory()+"/"+filename+".pdf";
				OutputStream fo = new FileOutputStream(outfile);
				byte[] buff = new byte[1024];
				int len = 0;
				while((len=ir.read(buff))>0) {
					fo.write(buff,0,len);
				}
				fo.flush();
				fo.close();
				resetData();
				updateStatus = "File Downloaded to:\n"+outfile;
			}
			ir.close();
		}
		connection.disconnect();
		} catch(Exception e) {
			Log.e("Except:", e.toString());
			//Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG); 
		}
		/*if(outfile!=null)
			openFile(outfile);*/
		statustext.post(new Runnable() {
			@Override
			public void run() {
				statustext.setText(updateStatus);
			}
		});
    }
    
    private void resetData() {
		error=null;
		filename="";
		uniquekey="";
		//choosebutton.setVisibility(View.VISIBLE);
		//statustext.setText(R.string.status);
		pref.edit().putString("uniquekey","").putString("filename", "").commit();
    }
    
    private void parseStatus(DataInputStream ir) {
    	try {
    		Xml.parse(ir, Encoding.US_ASCII, new ContentHandler() {
			private int state = 0; // 1->fileid 2->status 3->error
			@Override
			public void characters(char[] ch, int start, int length)
					throws SAXException {
				switch(state) {
					case 0:
					default:
						break;
					case 1:
						uniquekey = String.valueOf(ch, start, length);
						break;
					case 2:
						status = String.valueOf(ch, start, length);
						if(status.contentEquals("0"))
							status = filename+"\nProcessing..."; 
						else 
							status = filename+"\nIn Queue:"+status;
						break;
					case 3:
						error = String.valueOf(ch, start, length);
						break;
				}
				state = 0;
			}

			@Override
			public void endDocument() throws SAXException {
			}

			@Override
			public void endElement(String uri, String localName,
					String qName) throws SAXException {
			}

			@Override
			public void endPrefixMapping(String prefix)
					throws SAXException {
			}

			@Override
			public void ignorableWhitespace(char[] ch, int start,
					int length) throws SAXException {
			}

			@Override
			public void processingInstruction(String target, String data)
					throws SAXException {
			}

			@Override
			public void setDocumentLocator(Locator locator) {
			}

			@Override
			public void skippedEntity(String name) throws SAXException {
			}

			@Override
			public void startDocument() throws SAXException {
			}

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes atts) throws SAXException {
				if(localName.contentEquals("fileid"))
					state = 1;
				else if(localName.contentEquals("status"))
					state = 2;
				else if(localName.contentEquals("error"))
					state = 3;
				else 
					state = 0;
			}

			@Override
			public void startPrefixMapping(String prefix, String uri)
					throws SAXException {
			}
				
		});
    	} catch(Exception e) { }
    }
    
    private void openFile(String filename) {
    	Intent of = new Intent(Intent.ACTION_VIEW);
        of.setDataAndType(Uri.fromFile(new File(filename)), "application/pdf");
        startActivity(of);
    }
    
    // And to convert the image URI to the direct file system path of the image file
    private String getRealPathFromURI(Uri contentUri) {
            String [] media={MediaStore.Images.Media.DATA};
            Cursor cursor = managedQuery(contentUri,
                            media, // Which columns to return
                            null,
                            null,
                            null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
    }
}
