package com.myStress.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.myStress.R;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ParentReference;

@SuppressLint("NewApi")
public class ConnectGoogleAccount extends Activity {
	// preferences
	private SharedPreferences settings;

	// GDrive folder
	private String GDrive_Folder;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            a Bundle of the saved state, according to Android lifecycle
	 *            model
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set content of View
		setContentView(R.layout.googleaccounts);

		// get default preferences
		settings = PreferenceManager.getDefaultSharedPreferences(this);

		GDrive_Folder = settings.getString("GDriveFolder", "myStress_" + settings.getString("UniqueID", "" +
        		((TelephonyManager)getApplicationContext()
        				.getSystemService(TELEPHONY_SERVICE))
        				.getDeviceId()
        				.hashCode()
        				));

		try {
			// create upload directory here for continuing with the
			// authentication
			createDirectory();

			// clear persistent flag
			Editor editor = settings.edit();
			editor.putString("myStress_local::accountname",
					"stressapp.fim@gmail.com");
			// finally commit to storing values!!
			editor.commit();

		} catch (Exception e) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.ConnectGoogle4), Toast.LENGTH_LONG)
					.show();
			finish();
		}
	}

	/**
	 * Called when the activity is resumed.
	 */
	@Override
	public synchronized void onResume() {
		super.onResume();
	}

	/**
	 * Called when the activity is paused.
	 */
	@Override
	public synchronized void onPause() {
		super.onPause();
	}

	/**
	 * Called when the activity is stopped.
	 */
	@Override
	public void onStop() {
		super.onStop();
	}

	/**
	 * Called when the activity is destroyed.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void createDirectory() {
		new Thread(new Runnable() {
			public void run() {
				boolean running = true;
				com.google.api.services.drive.model.File myStress_dir = null;
				com.google.api.services.drive.model.File body;

				while (running == true) {
					try {
						Drive service = getDriveService2();
						SerialPortLogger
								.debugForced("trying to find myStress recordings directory in root");

						List<com.google.api.services.drive.model.File> files = service
								.files()
								.list()
								.setQ("mimeType = 'application/vnd.google-apps.folder' AND trashed=false AND 'root' in parents")
								.execute().getItems();
						for (com.google.api.services.drive.model.File f : files) {
							if (f.getTitle().compareTo(GDrive_Folder) == 0)
								myStress_dir = f;
						}

						if (myStress_dir == null) {
							SerialPortLogger
									.debugForced("...need to create myStress recordings directory");

							// create recordings directory in root!
							body = new com.google.api.services.drive.model.File();
							body.setTitle(GDrive_Folder);
							body.setParents(Arrays.asList(new ParentReference()
									.setId("root")));
							body.setMimeType("application/vnd.google-apps.folder");
							myStress_dir = service.files().insert(body).execute();
							if (myStress_dir != null)
								SerialPortLogger
										.debugForced("Created folder with id = "
												+ myStress_dir.getId());
						}
						
						Log.i("myStress","DriveAccount connected successfully");
						
						running = false;
					} catch (Exception e) {
						SerialPortLogger
								.debugForced("something went wrong with folder creation: "
										+ e.toString());
					}
				}

				finish();
			}
		}).start();
	}
	
	public Drive getDriveService2() {
		String SERVICE_ACCOUNT_EMAIL = "509211771570-ost52oo87929t8s96cifgk4rg0p1fcbr@developer.gserviceaccount.com";
	    HttpTransport httpTransport = new NetHttpTransport();
	    JsonFactory jsonFactory = new JacksonFactory();
	    GoogleCredential credential;
	    Drive service = null;
	    List<String> scopes = Arrays.asList(DriveScopes.DRIVE);


	    try {
		    InputStream input = getAssets().open("key.p12");
		    File tmpFile = File.createTempFile("key", "p12");
		    tmpFile.deleteOnExit();
		    try(FileOutputStream out = new FileOutputStream(tmpFile)){
		    	IOUtils.copy(input, out);
		    }
	        credential = new GoogleCredential.Builder()
	                .setTransport(httpTransport)
	                .setJsonFactory(jsonFactory)
	                .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
	                .setServiceAccountScopes(scopes)
	                .setServiceAccountPrivateKeyFromP12File(tmpFile)
	                .build();
	        service = new Drive.Builder(httpTransport, jsonFactory, null)
	        		.setApplicationName("myStress")
	                .setHttpRequestInitializer(credential)
	                .build();
	    } catch (Exception e) {
	        Log.e("myStress","Error on connecting GDrive: " + e.getMessage());
	    }
	    return service;
	}

}
