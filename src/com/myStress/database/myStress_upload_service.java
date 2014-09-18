/*
Copyright (C) 2012-2013, Dirk Trossen, airs@dirk-trossen.de
Copyright (C) 2014, FIM Research Center, myStress@fim-rc.de

This program is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation as version 2.1 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, Inc.,
59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*/

package com.myStress.database;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Permission;
import com.myStress.helper.Waker;

@SuppressLint("NewApi")
public class myStress_upload_service extends Service{ // implements MediaHttpUploaderProgressListener {
	// current batch of recordings for sync
	private static final int SYNC_BATCH = 5000;

	private final IBinder mBinder = new LocalBinder();
	private SharedPreferences settings;
	private Editor editor;
	private ConnectivityManager cm;
	private long synctime, currenttime, new_synctime;
	private File sync_file;
	private Uri share_file;
	private File fconn; // public for sharing file when exiting
	private BufferedOutputStream os = null;
	private boolean at_least_once_written = false;
	private SQLiteDatabase myStress_storage;
	private myStress_database database_helper;
	private Drive service;
	private Context context;
	private boolean wifi_only;
	private String currentFilename;

	private String uploadCounterString;
	private double uploadCounterDouble;
	
	public class LocalBinder extends Binder {
		myStress_upload_service getService() {
			return myStress_upload_service.this;
		}
	}

	/**
	 * Returns current instance of myStress_upload_service Service to anybody
	 * binding to it
	 * 
	 * @param intent
	 *            Reference to calling {@link android.content.Intent}
	 * @return current instance to service
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	/**
	 * Called when system is running low on memory
	 * 
	 * @see android.app.Service
	 */
	@Override
	public void onLowMemory() {
	}

	/**
	 * Called when starting the service the first time around
	 * 
	 * @see android.app.Service
	 */
	@Override
	public void onCreate() {
		Log.v("myStress", "Started upload service");

		// save for later
		context = this.getApplicationContext();

		// now get connectivity manager for net type check
		cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		// get default preferences and editor
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		editor = settings.edit();
		
		//Get next UploadCounter
		uploadCounterDouble = Double.parseDouble(settings.getString("UploadCounter", "1"));
		uploadCounterString = new DecimalFormat("000").format(uploadCounterDouble);


		// get settings for upload preference
		wifi_only = settings.getBoolean("UploadWifi", true);
		
		// get handle to Google Drive
		service = getDriveService();

		if (service != null) {
			try {
				// get database
				database_helper = new myStress_database(context);
				myStress_storage = database_helper.getWritableDatabase();

				// start sync thread
				new SyncThread();
			} catch (Exception e) {
				Log.e("myStress", "Cannot open myStress database for upload!");
				myStress_upload.setTimer(context); // timer will fire again soon!
			}
		}
	}

	/**
	 * Called when service is destroyed, e.g., by stopService() Here, we tear
	 * down all recording threads, close all handlers, unregister receivers for
	 * battery signal and close the thread for indicating the recording
	 */
	@Override
	public void onDestroy() {
		Log.v("myStress", "...destroyed upload service");
	}

	private class SyncThread implements Runnable {
		SyncThread() {
			new Thread(this).start();
		}

		public void run() {
			com.google.api.services.drive.model.File body;
			com.google.api.services.drive.model.File file;
			java.io.File fileContent;
			FileContent mediaContent;
			Drive.Files.Insert insert;
			MediaHttpUploader uploader;
			boolean right_network = true, try_upload = true;

			// now create the syncfile
			if (createSyncFile(context) == true) {
				// try to upload until right network is available
				while (try_upload == true) {
					try {
						right_network = checkRightNetwork();

						// only if right network is available, try to upload
						if (right_network == true) {
							// File's binary content
							fileContent = new java.io.File(share_file.getPath());
							mediaContent = new FileContent("text/plain",
									fileContent);

							// File's metadata
							body = new com.google.api.services.drive.model.File();
							body.setTitle(fileContent.getName());
							body.setMimeType("text/plain");

							Log.v("myStress", "...trying to upload myStress recordings");

							// now get the uploader handle and set resumable
							// upload
							insert = service.files().insert(body, mediaContent);
							uploader = insert.getMediaHttpUploader();
							uploader.setDirectUploadEnabled(false);
							uploader.setChunkSize(MediaHttpUploader.DEFAULT_CHUNK_SIZE);
							Log.v("myStress", "...executing upload myStress recordings");

							do {
								right_network = checkRightNetwork();

								// only if right network is available, try to
								// upload
								if (right_network == true) {
									// now execute the upload
									file = insert.execute();
									insertPermission(service, file.getId());
									if (file != null) {
										Log.v("myStress",
												"...writing new sync timestamp");
										// write the time until read for later
										// syncs
										// put sync timestamp into store
										editor.putLong("SyncTimestamp",
												new_synctime);
										editor.putString("UploadCounter", ""+(uploadCounterDouble+1));
										
										// finally commit to storing values!!
										editor.commit();

										// remove temp files
										sync_file.delete();

										// now finish this loop since we are
										// done!
										try_upload = false;
									}
								} else {
									Log.v("myStress",
											"...sleeping until right network becomes available");
									Waker.sleep(3600000);
								}
							} while (right_network == false);
						} else {
							Log.v("myStress",
									"...sleeping until right network becomes available");
							Waker.sleep(3600000);
						}
					} catch (Exception e) {
						Log.e("myStress",
								"something went wrong in uploading the sync data: "
										+ e.toString());
					}
				}
			} else {
				Log.v("myStress", "...nothing to sync, it seems");
				Log.v("myStress", "...writing new sync timestamp");
				// write the time until read for later syncs
				// put sync timestamp into store
				editor.putLong("SyncTimestamp", new_synctime);

				// finally commit to storing values!!
				editor.commit();
			}

			// set timer again
			if(settings.getBoolean("myStress_local::running", false) == true)
				myStress_upload.setTimer(context);

			// now stop the overall service all together!
			stopSelf();
		}
	}

	private boolean checkRightNetwork() {
		boolean right_network = true;
		NetworkInfo netInfo;

		// check network connectivity
		netInfo = cm.getActiveNetworkInfo();
		// any network available?
		if (netInfo != null) {
			// is it the right network (in case wifi only is enabled)?
			if (netInfo.getType() != ConnectivityManager.TYPE_WIFI
					&& wifi_only == true)
				right_network = false;
		} else
			right_network = false; // no network available anyways

		return right_network;
	}

	@SuppressLint("NewApi")
	public Drive getDriveService() {
		//FIXME: EMAIL ENTFERNEN
		String SERVICE_ACCOUNT_EMAIL = "509211771570-ost52oo87929t8s96cifgk4rg0p1fcbr@developer.gserviceaccount.com";
		HttpTransport httpTransport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();
		GoogleCredential credential;
		Drive service = null;
		List<String> scopes = Arrays.asList(DriveScopes.DRIVE);
		//FIXME Die Key Datei im assets Ordner löschen!!!!!!!!!!!
		try {
			InputStream input = getAssets().open("key.p12");
			File tmpFile = File.createTempFile("key", "p12");
			tmpFile.deleteOnExit();
			try (FileOutputStream out = new FileOutputStream(tmpFile)) {
				IOUtils.copy(input, out);
			}
			credential = new GoogleCredential.Builder()
					.setTransport(httpTransport).setJsonFactory(jsonFactory)
					.setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
					.setServiceAccountScopes(scopes)
					.setServiceAccountPrivateKeyFromP12File(tmpFile).build();
			service = new Drive.Builder(httpTransport, jsonFactory, null)
					.setApplicationName("myStress")
					.setHttpRequestInitializer(credential).build();
		} catch (Exception e) {
	        Log.e("myStress","Error on connecting GDrive: " + e.getMessage());
		}
		return service;
	}

	private boolean createSyncFile(Context context) {
		if (createValueFile(context) == true)
			return createNoteFile(context);

		return false;
	}

	private boolean createValueFile(Context context) {
		String query;
		int t_column, s_column, v_column;
		Cursor values;
		String value, symbol;
		String line_to_write;
		byte[] writebyte;
		int number_values;
		int i;
		Calendar cal = Calendar.getInstance();
		boolean syncing = true;

		// get timestamp of last sync
		synctime = settings.getLong("SyncTimestamp", 0);
		currenttime = synctime;

		// sync until just about now!
		new_synctime = System.currentTimeMillis();

		Log.v("myStress", "start creating values sync file!");

		// path for templates
		File external_storage = context.getExternalFilesDir(null);

		if (external_storage == null)
			return false;

		sync_file = new File(external_storage, "myStress_temp");

		// get files in directory
		String[] file_list = sync_file.list(null);

		// remove files in myStress_temp directory
		if (file_list != null)
			for (i = 0; i < file_list.length; i++) {
				File remove = new File(sync_file, file_list[i]);
				remove.delete();
			}

		try {
			// open file in public directory
			sync_file = new File(external_storage, "myStress_temp");
			// make sure that path exists
			sync_file.mkdirs();
			// open file and create, if necessary
			currentFilename = new String(settings.getString("UniqueID", "" +
	        		((TelephonyManager)getApplicationContext()
	        				.getSystemService(TELEPHONY_SERVICE))
	        				.getDeviceId()
	        				.hashCode()
	        				)+ "_" + uploadCounterString + ".txt");
			fconn = new File(sync_file, currentFilename);
			os = new BufferedOutputStream(new FileOutputStream(fconn, true));
			
			// print version code
			try{
				String version = "myStress v"+context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName+"\n";
				os.write(version.getBytes());
			}catch(Exception e){
				e.printStackTrace();
			}
			
			// build URI for sharing
			share_file = Uri.fromFile(fconn);

			while (syncing == true) {
				query = new String(
						"SELECT Timestamp, Symbol, Value from 'myStress_values' WHERE Timestamp > "
								+ String.valueOf(currenttime)
								+ " AND TimeStamp < "
								+ String.valueOf(new_synctime) + " LIMIT "
								+ String.valueOf(SYNC_BATCH));
				values = myStress_storage.rawQuery(query, null);

				// garbage collect
				query = null;

				if (values == null) {
					if (at_least_once_written == true)
						return true;
					else {
						os.close();
						return false;
					}
				}

				// get number of rows
				number_values = values.getCount();

				// if nothing is read (anymore)
				if (number_values == 0) {
					if (at_least_once_written == true)
						return true;
					else {
						os.close();
						return false;
					}
				}

				// get column index for timestamp and value
				t_column = values.getColumnIndex("Timestamp");
				s_column = values.getColumnIndex("Symbol");
				v_column = values.getColumnIndex("Value");

				if (t_column == -1 || v_column == -1 || s_column == -1) {
					if (at_least_once_written == true)
						return true;
					else {
						os.close();
						return false;
					}
				}

				Log.v("myStress", "...reading next batch!");

				// move to first row to start
				values.moveToFirst();
				// read DB values into arrays
				for (i = 0; i < number_values; i++) {

					// get timestamp
					cal.setTimeInMillis(values.getLong(t_column));
					DateFormat sdf = new SimpleDateFormat(
							"dd.MM.yyyy HH:mm:ss",
							Locale.getDefault());

					// get symbol
					symbol = values.getString(s_column);
					// get value
					value = values.getString(v_column);

					// add empty string as space
					if (value.compareTo("") == 0)
						value = " ";

					// create line to write to file
					line_to_write = new String(new String(sdf.format(cal.getTime())) + ";" + symbol + ";" + value + "\n");

					// now write to file
					writebyte = line_to_write.getBytes();

					os.write(writebyte, 0, writebyte.length);

					// garbage collect the output data
					writebyte = null;
					line_to_write = null;

					// now move to next row
					values.moveToNext();
				}

				if(number_values>0)
					at_least_once_written = true;
				
				// close values to free up memory
				values.close();
				
				syncing = false;
			}
		} catch (Exception e) {
			try {
				if (os != null)
					os.close();
			} catch (Exception ex) {
			}
			// signal end of synchronization
		}

		// now return
		if (at_least_once_written == true)
			return true;
		else
			return false;
	}

	private boolean createNoteFile(Context context) {
		String query;
		int y_column, m_column, d_column, a_column, c_column, mo_column;
		Cursor values;
		String value, symbol;
		String line_to_write;
		byte[] writebyte;
		int number_values;
		int i;
		boolean syncing = true;
		Calendar cal = Calendar.getInstance();

		// get timestamp of last sync
		synctime = settings.getLong("SyncTimestamp", 0);
		currenttime = synctime;

		Log.v("myStress", "start creating sync notes file!");

		try {
			// now sync the notes, if any
			syncing = true;
			while (syncing == true) {
				query = new String(
						"SELECT Year, Month, Day, Annotation, created, modified from 'myStress_annotations' WHERE created > "
								+ String.valueOf(currenttime)
								+ " AND created < "
								+ String.valueOf(new_synctime)
								+ " LIMIT "
								+ String.valueOf(SYNC_BATCH));
				values = myStress_storage.rawQuery(query, null);

				// garbage collect
				query = null;

				if (values == null) {
					// purge file
					os.close();

					if (at_least_once_written == true)
						return true;
					else
						return false;
				}

				// get number of rows
				number_values = values.getCount();

				// if nothing is read (anymore)
				if (number_values == 0) {
					// purge file
					os.close();

					if (at_least_once_written == true)
						return true;
					else
						return false;
				}

				// get column index for timestamp and value
				y_column = values.getColumnIndex("Year");
				m_column = values.getColumnIndex("Month");
				d_column = values.getColumnIndex("Day");
				a_column = values.getColumnIndex("Annotation");
				c_column = values.getColumnIndex("created");
				mo_column = values.getColumnIndex("modified");

				if (y_column == -1 || m_column == -1 || d_column == -1
						|| a_column == -1 || c_column == -1 || mo_column == -1) {
					// purge file
					os.close();

					if (at_least_once_written == true)
						return true;
					else
						return false;
				}

				Log.v("myStress", "...reading next batch!");

				// move to first row to start
				values.moveToFirst();
				// read DB values into arrays
				for (i = 0; i < number_values; i++) {
					// get timestamp
					cal.set(values.getInt(y_column), values.getInt(m_column), values.getInt(d_column));
					DateFormat sdf = new SimpleDateFormat(
							"dd.MM.yyyy HH:mm:ss",
							Locale.getDefault());

					// set symbol
					symbol = "UN";
					// create value as concatenation of
					// year:month:day:modified:annotation
					value = String.valueOf(values.getInt(y_column)) + ":"
							+ String.valueOf(values.getInt(m_column)) + ":"
							+ String.valueOf(values.getInt(d_column)) + ":"
							+ String.valueOf(values.getLong(mo_column)) + ":"
							+ values.getString(a_column);

					// create line to write to file
					line_to_write = new String(new String(sdf.format(cal.getTime())) + ";" + symbol + ";" + value + "\n");

					// now write to file
					writebyte = line_to_write.getBytes();

					os.write(writebyte, 0, writebyte.length);

					// garbage collect the output data
					writebyte = null;
					line_to_write = null;

					// now move to next row
					values.moveToNext();
				}

				if(number_values>0)
					at_least_once_written = true;
				
				// close values to free up memory
				values.close();
				
				syncing = false;
			}

			// close output file
			os.close();
		} catch (Exception e) {
			try {
				if (os != null)
					os.close();
			} catch (Exception ex) {
			}
			// signal end of synchronization
		}

		// now return
		if (at_least_once_written == true)
			return true;
		else
			return false;
	}

	/**
	 * Insert a new permission.
	 *
	 * @param service
	 *            Drive API service instance.
	 * @param fileId
	 *            ID of the file to insert permission for.
	 * @param value
	 *            User or group e-mail address, domain name or {@code null}
	 *            "default" type.
	 * @param type
	 *            The value "user", "group", "domain" or "default".
	 * @param role
	 *            The value "owner", "writer" or "reader".
	 * @return The inserted permission if successful, {@code null} otherwise.
	 */
	private static Permission insertPermission(Drive service, String fileId) {
		Permission newPermission = new Permission();

		newPermission.setValue("stressapp.fim@gmail.com");
		newPermission.setType("user");
		newPermission.setRole("writer");
		try {
			return service.permissions()
					.insert(fileId, newPermission)
					.execute();
		} catch (IOException e) {
	        Log.e("myStress","Error on setting Permission for GDrive-File: " + e.getMessage());
		}
		return null;
	}
}
