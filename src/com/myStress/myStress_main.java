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

package com.myStress;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.myStress.database.myStress_upload;
import com.myStress.helper.PopUpManager;
import com.myStress.helper.SerialPortLogger;
import com.myStress.helper.Switch;

/**
 * Activity for the Record tab in the main UI, controlling the recording and
 * managing the templates
 *
 * @see myStress_local
 */
public class myStress_main extends Activity implements
		android.widget.AdapterView.OnItemSelectedListener,
		CompoundButton.OnClickListener {
	/**
	 * expose template for other tabs
	 */
	private static String current_template = "";

	// Layout Views
	private CheckBox cbShowNotification;
	private Switch swStart;
	private Spinner spDateIntervall;
	private Button bRfresh;

	// preferences
	private SharedPreferences settings;
	private int currentVersionCode = 0;

	// other variables
	private myStress_local myStress_locally;
	private myStress_main myStress;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            a Bundle of the saved state, according to Android lifecycle
	 *            model
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set up the window layout
		super.onCreate(savedInstanceState);
		setTheme(R.style.AppThemeLight);

		// save current instance for inner classes
		this.myStress = this;

		// get default preferences
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		//get currentVersionCode
		try {
			currentVersionCode = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}

		// save activity in debug class
		// SerialPortLogger.setBackupActivity(this);
		// is debugging on?
		// SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));
		SerialPortLogger.setDebugging(false);
		SerialPortLogger.debug("myStress debug output at "
				+ Calendar.getInstance().getTime().toString());

		// set content of View
		setContentView(R.layout.main);

		// get switch, initialize it and set onclick listener
		spDateIntervall = (Spinner) this.findViewById(R.id.spDateInterval);
		spDateIntervall.setOnItemSelectedListener(this);
		spDateIntervall.setSelection(settings.getInt("SpinnerPosition", 0));

		// get switch, initialize it and set onclick listener
		bRfresh = (Button) this.findViewById(R.id.bRefresh);
		bRfresh.setOnClickListener(this);

		// get image and set onclick listener
		((ImageView) findViewById(R.id.imgSettings))
				.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						myStress_main.this.openOptionsMenu();
					}
				});

		// get switch, initialize it and set onclick listener
		swStart = (Switch) this.findViewById(R.id.swMain);
		swStart.setChecked(settings
				.getBoolean("myStress_local::running", false));
		swStart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (!((Switch) buttonView).isTriggerListener())
					return;

				if (isChecked) {
					if (!isAccessibilityEnabled()) {
						if (!startAccessibility()) {
							// deactivate listener to supress
							// reaction
							swStart.setTriggerListener(false);

							// check switch again
							swStart.setChecked(false);

							// re-activate listener
							swStart.setTriggerListener(true);

							return;
						}
					}

					else {
						myStress_upload.setTimer(getApplicationContext());
						initializeStart();
					}
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							myStress_main.this);
					builder.setMessage(
							getString(R.string.myStress_running_exit))
							.setTitle(getString(R.string.myStress_Sensing))
							.setCancelable(false)
							.setPositiveButton(getString(R.string.Yes),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											// clear persistent flag
											Editor editor = settings.edit();
											editor.putBoolean(
													"myStress_local::running",
													false);
											// finally commit to storing
											// values!!
											editor.commit();
											// stop service
											stopService(new Intent(myStress,
													myStress_local.class));
											finish();
										}
									})
							.setNegativeButton(getString(R.string.No),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {

											// deactivate listener to supress
											// reaction
											swStart.setTriggerListener(false);

											// check switch again
											swStart.setChecked(true);

											// re-activate listener
											swStart.setTriggerListener(true);

											dialog.cancel();
										}
									});
					AlertDialog alert = builder.create();
					alert.show();
				}
			}
		});

		// define ButtonGroups
		cbShowNotification = (CheckBox) findViewById(R.id.cbShowNotification);
		cbShowNotification.setChecked(settings.getBoolean("showNotification",
				true));
		cbShowNotification
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						boolean showNotification = cbShowNotification
								.isChecked();
						settings.edit()
								.putBoolean("showNotification",
										showNotification).commit();
					}
				});

		if (settings.getBoolean("myStress_local::running", false) == true)
			cbShowNotification.setEnabled(false);
		else
			cbShowNotification.setEnabled(true);

		// start service and connect to it -> then discover the sensors
		getApplicationContext().startService(
				new Intent(this, myStress_local.class));
		getApplicationContext().bindService(
				new Intent(this, myStress_local.class), mConnection,
				Service.BIND_AUTO_CREATE);

		// check if first start in order to show 'Getting Started' dialog
		if (settings.getBoolean("myStress_local::first_start", false) == false) {
			SpannableString s = new SpannableString(
					getString(R.string.Getting_Started2));
			Linkify.addLinks(s, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(s)
					.setTitle(getString(R.string.Getting_Started))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.OK),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// clear persistent flag
									Editor editor = settings.edit();
									editor.putBoolean(
											"myStress_local::first_start", true);
									editor.putString(
											"myStress_local::version", ""
													+ currentVersionCode);
									// finally commit to storing values!!
									editor.commit();
									dialog.dismiss();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();

			// Make the textview clickable. Must be called after show()
			((TextView) alert.findViewById(android.R.id.message))
					.setMovementMethod(LinkMovementMethod.getInstance());
			
			Editor editor2 = settings.edit();
			editor2.putString(
					"myStress_local::version", "");

			// finally commit to storing
			// values!!
			editor2.commit();

			try {
				// get editor for settings
				Editor editor = settings.edit();
				// put version code into store
				editor.putInt("Version", this.getPackageManager()
						.getPackageInfo(this.getPackageName(), 0).versionCode);
				editor.putString("UniqueID", ""
						+ ((TelephonyManager) getApplicationContext()
								.getSystemService(TELEPHONY_SERVICE))
								.getDeviceId().hashCode());

				// finally commit to storing values!!
				editor.commit();
			} catch (Exception e) {
			}
		}
		if(!settings.getString("myStress_local::version", "").trim().equals(
				(""+currentVersionCode).trim())){
			// check if app is updated
				int counter = myStress_local.countPolled(this);
				SpannableString s = new SpannableString(
						getString(R.string.whatsNew2) +" " + counter + " " + getString(R.string.whatsNew3));
				Linkify.addLinks(s, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
				
				PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("StressCounter", counter).commit();

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(s)
						.setTitle(getString(R.string.whatsNew))
						.setCancelable(false)
						.setPositiveButton(getString(R.string.OK),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										// clear persistent flag
										Editor editor = settings.edit();
										editor.putString(
												"myStress_local::version", ""
														+ currentVersionCode);

										// finally commit to storing
										// values!!
										editor.commit();
										dialog.dismiss();
									}
								});
				AlertDialog alert = builder.create();
				alert.show();

				// Make the textview clickable. Must be called after show()
				((TextView) alert.findViewById(android.R.id.message))
						.setMovementMethod(LinkMovementMethod.getInstance());
				//FIXME Vor Veröffentlichung muss die Zahl 32 durch 30 ersetzt werden
		}
		if (settings.getInt("StressCounter", 0)>=33){
			SpannableString s = new SpannableString(getString(R.string.finished));
			Linkify.addLinks(s, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(s)
					.setTitle(getString(R.string.whatsNew))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.OK),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// clear persistent flag
									Editor editor = settings.edit();
									editor.putString(
											"myStress_local::version", ""
													+ currentVersionCode);

									// finally commit to storing
									// values!!
									editor.commit();
									dialog.dismiss();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();

			// Make the textview clickable. Must be called after show()
			((TextView) alert.findViewById(android.R.id.message))
					.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	/**
	 * Called when the activity is resumed.
	 */
	@Override
	public synchronized void onResume() {
		super.onResume();

		// check if persistent flag is running, indicating the myStress has been
		// running (and would re-start if continuing)
		if (settings.getBoolean("myStress_local::running", false) == true) {
			showGUI();
		}
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

		if (myStress_locally != null) {
			if (myStress_locally.isRunning() == false)
				getApplicationContext().stopService(
						new Intent(this, myStress_local.class));
			// unbind from service
			getApplicationContext().unbindService(mConnection);

			myStress_locally.stopService(new Intent(myStress_locally,
					myStress_upload.class));
			Log.e("myStress", "App destroyed");
		}
	}

	/**
	 * Called when the configuration of the activity has changed.
	 * 
	 * @param newConfig
	 *            new configuration after change
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Called when the Options menu is opened
	 * 
	 * @param menu
	 *            Reference to the {@link android.view.Menu}
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuInflater inflater;
		menu.clear();
		inflater = getMenuInflater();
		inflater.inflate(R.menu.options_main, menu);

		return true;
	}

	/**
	 * Called when an option menu item has been selected by the user
	 * 
	 * @param item
	 *            Reference to the {@link android.view.MenuItem} clicked on
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.main_copyright:
			try {
				PopUpManager.AboutDialog(
						"myStress V"
								+ this.getPackageManager().getPackageInfo(
										this.getPackageName(), 0).versionName,
						getString(R.string.Copyright), this);
			} catch (Exception e) {
			}
			break;
		case R.id.main_uniqueID:
			PopUpManager.AboutDialog(getString(R.string.uniqueID),
					((TelephonyManager) getApplicationContext()
							.getSystemService(TELEPHONY_SERVICE)).getDeviceId()
							.hashCode()
							+ "", this);
			break;
		case R.id.main_measurements:
			if (myStress_locally.isRunning() == false)
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.noData),
						Toast.LENGTH_SHORT).show();
			else {
				Intent startintent = new Intent(myStress,
						myStress_measurements.class);
				startintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				myStress.startActivity(startintent);
			}
			break;
		case R.id.main_help:
			PopUpManager.AboutDialog(getResources().getString(R.string.Help),
					getString(R.string.HelpText), this);
			break;
		}

		return false;
	}

	// local service connection
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			myStress_locally = ((myStress_local.LocalBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			myStress_locally = null;
		}
	};

	private void initializeStart() {
		AlertDialog.Builder builder;
		AlertDialog alert;

		// check if persistent flag is running, indicating the myStress has been
		// running (and would re-start if continuing)
		if (settings.getBoolean("myStress_local::running", false) == true) {
			builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.myStress_running_exit))
					.setTitle(getString(R.string.myStress_Sensing))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.Yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// clear persistent flag
									Editor editor = settings.edit();
									editor.putBoolean(
											"myStress_local::running", false);
									// finally commit to storing values!!
									editor.commit();
									// stop service
									stopService(new Intent(myStress,
											myStress_local.class));
									finish();
								}
							})
					.setNegativeButton(getString(R.string.No),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			alert = builder.create();
			alert.show();
		} else {
			// start measurements now
			if (myStress_locally != null) {
				// finish UI
				finish();
				// merely restart without GUI
				myStress_locally.Restart(false);
				// service running message
				Toast.makeText(getApplicationContext(),
						getString(R.string.myStress_started_remote),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void showGUI() {
		boolean snoozed = settings.getBoolean("myStress::snoozed", false);
		if (snoozed) {
			Intent intent = new Intent("com.myStress.pollstress");
			sendBroadcast(intent);
			finish();
		}
	}

	private boolean startAccessibility() {
		// Provider not enabled, prompt user to enable it
		final AlertDialog alertDialog = new AlertDialog.Builder(
				myStress_main.this).create();
		alertDialog.setCancelable(false);
		alertDialog.setTitle(getResources().getString(
				R.string.accessibility_titel));
		alertDialog.setMessage(getResources().getString(
				R.string.accessibility_message));
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources()
				.getString(R.string.accessibility_goto),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						alertDialog.cancel();
						Intent myIntent = new Intent(
								Settings.ACTION_ACCESSIBILITY_SETTINGS);
						myStress_main.this.startActivity(myIntent);
					}
				});
		alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources()
				.getString(R.string.Back),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

						alertDialog.cancel();
					}
				});

		alertDialog.show();

		return isAccessibilityEnabled();
	}

	private boolean isAccessibilityEnabled() {
		int accessibilityEnabled = 0;
		try {
			accessibilityEnabled = Settings.Secure.getInt(
					this.getContentResolver(),
					android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
		} catch (SettingNotFoundException e) {
			Log.e("myStress",
					"Error finding setting, default accessibility to not found: "
							+ e.getMessage());
			return false;
		}

		TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(
				':');

		if (accessibilityEnabled == 1) {
			String settingValue = Settings.Secure.getString(
					getContentResolver(),
					Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
			if (settingValue != null) {
				TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
				splitter.setString(settingValue);
				while (splitter.hasNext()) {
					String accessabilityService = splitter.next();
					if (accessabilityService
							.equalsIgnoreCase("com.myStress/com.myStress.handlers.NotificationHandlerService")) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static String getCurrentTemplate() {
		return current_template;
	}

	/**
	 * Called for dispatching key events sent to the Activity
	 * 
	 * @param event
	 *            Reference to the {@link android.view.KeyEvent} being pressed
	 * @return true, if consumed, false otherwise
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN)
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
				return true;

		// key de-pressed?
		if (event.getAction() == KeyEvent.ACTION_UP)
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
				finish();

		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		Editor editor = settings.edit();
		editor.putInt("SpinnerPosition",
				spDateIntervall.getSelectedItemPosition());
		editor.commit();

		updateTableValues(spDateIntervall.getSelectedItemPosition());
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	private void updateTableValues(int selectedIndex) {
		// 0 heute
		// 1 3 Tage
		// 2 1 Woche
		// 3 2 Wochen
		// 4 Gesamt
		Long begin = (long) 0;

		Long end = Calendar.getInstance().getTimeInMillis();
		switch (selectedIndex) {
		case 0:
			GregorianCalendar tmp = (GregorianCalendar) Calendar.getInstance();
			tmp.set(Calendar.AM_PM, Calendar.AM);
			tmp.set(Calendar.HOUR, 0);
			tmp.set(Calendar.MINUTE, 0);
			tmp.set(Calendar.SECOND, 1);
			begin = tmp.getTimeInMillis();
			break;
		case 1:
			begin = end - 259200000;
			break;
		case 2:
			begin = end - 604800000;
			break;
		case 3:
			begin = end - 1209600000;
			break;
		case 4:
			begin = (long) 0;
			break;
		}

		// set values in table
		if (settings.getBoolean("myStress_local::running", false)) {
			String[] values = myStress_locally.getMainArray(begin, end);
			TextView tmp = (TextView) findViewById(R.id.lblValue1);
			tmp.setText(values[0]);
			tmp = (TextView) findViewById(R.id.lblValue2);
			tmp.setText(values[1]);
			tmp = (TextView) findViewById(R.id.lblValue3);
			tmp.setText(values[2]);
			tmp = (TextView) findViewById(R.id.lblValue4);
			tmp.setText(values[3]);
			tmp = (TextView) findViewById(R.id.lblValue5);
			tmp.setText(values[4]);
			tmp = (TextView) findViewById(R.id.lblValue6);
			tmp.setText(values[5]);
			tmp = (TextView) findViewById(R.id.lblValue7);
			tmp.setText(values[6]);
			tmp = (TextView) findViewById(R.id.lblValue8);
			tmp.setText(values[7]);
		}
	}

	@Override
	public void onClick(View v) {
		updateTableValues(spDateIntervall.getSelectedItemPosition());
	}
}
