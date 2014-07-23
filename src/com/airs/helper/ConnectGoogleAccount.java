package com.airs.helper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.airs.R;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.services.CommonGoogleClientRequestInitializer;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ParentReference;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;

@SuppressLint("NewApi")
public class ConnectGoogleAccount extends Activity {
	static private final int REQUEST_AUTHORIZATION = 2;

	// the actual credential token
	private Credential credential;

	// preferences
	private SharedPreferences settings;

	// GDrive folder
	private String GDrive_Folder;

	//API KEY
	//FIXME: In Datei auslagern
	private static final String API_KEY = "AIzaSyCfrJZhFH6o0TitB003tS__bx4Jl5wYYDI";


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

		// FIXME: Add unique Folder key
		GDrive_Folder = settings.getString("GDriveFolder", "AIRS");

		try {
			// create upload directory here for continuing with the
			// authentication
			createDirectory();

			// clear persistent flag
			Editor editor = settings.edit();
			editor.putString("AIRS_local::accountname",
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
				com.google.api.services.drive.model.File AIRS_dir = null;
				com.google.api.services.drive.model.File body;

				while (running == true) {
					try {
						Drive service = getDriveService();
						SerialPortLogger
								.debugForced("trying to find AIRS recordings directory in root");

						List<com.google.api.services.drive.model.File> files = service
								.files()
								.list()
								.setQ("mimeType = 'application/vnd.google-apps.folder' AND trashed=false AND 'root' in parents")
								.execute().getItems();
						for (com.google.api.services.drive.model.File f : files) {
							if (f.getTitle().compareTo(GDrive_Folder) == 0)
								AIRS_dir = f;
						}

						if (AIRS_dir == null) {
							SerialPortLogger
									.debugForced("...need to create AIRS recordings directory");

							// create recordings directory in root!
							body = new com.google.api.services.drive.model.File();
							body.setTitle(GDrive_Folder);
							body.setParents(Arrays.asList(new ParentReference()
									.setId("root")));
							body.setMimeType("application/vnd.google-apps.folder");
							AIRS_dir = service.files().insert(body).execute();
							if (AIRS_dir != null)
								SerialPortLogger
										.debugForced("Created folder with id = "
												+ AIRS_dir.getId());
						}

						running = false;
					} catch (UserRecoverableAuthIOException e) {
						SerialPortLogger
								.debugForced("Require authorization - starting activity!");
						startActivityForResult(e.getIntent(),
								REQUEST_AUTHORIZATION);
						return;
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

	public static Drive getDriveService() throws GeneralSecurityException,
			IOException, URISyntaxException {
		Collection<String> scope = new ArrayList<String>();
		scope.add(DriveScopes.DRIVE);
		
		HttpTransport httpTransport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();
		
		AppIdentityCredential credential = new AppIdentityCredential.Builder(scope).build();
		GoogleClientRequestInitializer keyInitializer = new CommonGoogleClientRequestInitializer(
				API_KEY);
		Drive service = new Drive.Builder(httpTransport, jsonFactory, null)
				.setHttpRequestInitializer(credential)
				.setGoogleClientRequestInitializer(keyInitializer)
				.setApplicationName("myStress")
				.build();
		return service;
	}

}
