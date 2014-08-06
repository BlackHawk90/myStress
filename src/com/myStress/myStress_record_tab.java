package com.myStress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.myStress.database.myStress_upload;
import com.myStress.helper.PopUpManager;
import com.myStress.helper.SerialPortLogger;

/**
 * Activity for the Record tab in the main UI, controlling the recording and managing the templates
 *
 * @see         myStress_local
 */
public class myStress_record_tab extends Activity implements OnClickListener
{
	/**
	 * expose template for other tabs
	 */
	public static String current_template = "";
	
	// Layout Views
    private ImageButton main_record;
    private Spinner main_spinner;

    // preferences
    private SharedPreferences settings;
  
    // other variables
    private myStress_local 	myStress_locally;
    private myStress_record_tab	myStress;
    
	/** Called when the activity is first created. 
     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
     */
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        // Set up the window layout
        super.onCreate(savedInstanceState);
                
        // save current instance for inner classes
        this.myStress = this;
        
        // get default preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        // save activity in debug class
        SerialPortLogger.nors = this;
		// is debugging on?
   		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));
		SerialPortLogger.debug("myStress debug output at " + Calendar.getInstance().getTime().toString());
			
        // set content of View
        setContentView(R.layout.recording);

        // get buttons and set onclick listener
        main_record = (ImageButton)findViewById(R.id.button_record);
        main_record.setOnClickListener(this);
        // get spinner
        main_spinner = (Spinner)findViewById(R.id.spinner_record);
                             
        // now initialise the upload timer
        myStress_upload.setTimer(getApplicationContext());    

	    // start service and connect to it -> then discover the sensors
        getApplicationContext().startService(new Intent(this, myStress_local.class));
        getApplicationContext().bindService(new Intent(this, myStress_local.class), mConnection, Service.BIND_AUTO_CREATE);     

		// check if persistent flag is running, indicating the myStress has been running (and would re-start if continuing)
		if (settings.getBoolean("myStress_local::running", false) == true)
		{
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage(getString(R.string.myStress_running_exit))
    			   .setTitle(getString(R.string.myStress_Sensing))
    		       .setCancelable(false)
    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		        	    // clear persistent flag
    			           	Editor editor = settings.edit();
    			           	editor.putBoolean("myStress_local::running", false);
    		                // finally commit to storing values!!
    		                editor.commit();
    		                // stop service
 		    			    stopService(new Intent(myStress, myStress_local.class));
 		    			    finish();
    		           }
    		       })
    		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
		    			    finish();
    		                dialog.cancel();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
		}
		
		// check if first start in order to show 'Getting Started' dialog
		if (settings.getBoolean("myStress_local::first_start", false) == false)
		{
		    SpannableString s = new SpannableString(getString(R.string.Getting_Started2));
		    Linkify.addLinks(s, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage(s)
    			   .setTitle(getString(R.string.Getting_Started))
    		       .setCancelable(false)
    		       .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		        	    // clear persistent flag
    			           	Editor editor = settings.edit();
    			           	editor.putBoolean("myStress_local::first_start", true);
    		                // finally commit to storing values!!
    		                editor.commit();
    		                dialog.dismiss();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
    		
    		// Make the textview clickable. Must be called after show()
    	    ((TextView)alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    	    
    	    try
    	    {
	        	// get editor for settings
	        	Editor editor = settings.edit();
    			// put version code into store
                editor.putInt("Version", this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode);
                editor.putString("UniqueID", "" +
                		((TelephonyManager)getApplicationContext()
                				.getSystemService(TELEPHONY_SERVICE))
                				.getDeviceId()
                				.hashCode()
                				);
                
                // finally commit to storing values!!
                editor.commit();
    	    }
    	    catch(Exception e)
    	    {	    	
    	    }
		}
    }

	/** Called when the activity is resumed. 
     */
	@Override
    public synchronized void onResume() 
    {
        super.onResume();     
    }

	/** Called when the activity is paused. 
     */
	@Override
    public synchronized void onPause() 
    {
        super.onPause();
    }

	/** Called when the activity is stopped. 
     */
	@Override
    public void onStop() 
    {
        super.onStop();
    }

	/** Called when the activity is destroyed. 
     */
	@Override
    public void onDestroy() 
    {
       super.onDestroy();
       
       if (myStress_locally!=null)
       {
		   if (myStress_locally.running == false)
			   getApplicationContext().stopService(new Intent(this, myStress_local.class));
		   // unbind from service
		   getApplicationContext().unbindService(mConnection);
       }
    }
    
	/** Called when the configuration of the activity has changed.
     * @param newConfig new configuration after change 
     */
	@Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
    	super.onConfigurationChanged(newConfig);
    }
 
	/** Called when the Options menu is opened
     * @param menu Reference to the {@link android.view.Menu}
     */
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
		inflater.inflate(R.menu.options_main, menu);    			
        
        return true;
    }

	/** Called when an option menu item has been selected by the user
     * @param item Reference to the {@link android.view.MenuItem} clicked on
     */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    	  	
        switch (item.getItemId()) 
        {
        case R.id.main_copyright:
    		try
    		{
    			PopUpManager.AboutDialog("myStress V" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName , getString(R.string.Copyright) + getString(R.string.ReleaseNotes), this);
    		}
    		catch(Exception e)
    		{
    		}
    		break;
        case R.id.main_about:
        	PopUpManager.AboutDialog(getString(R.string.Help) , getString(R.string.RecordAbout), this);
			break;
        case R.id.main_uniqueID:
        	PopUpManager.AboutDialog(getString(R.string.uniqueID) , ((TelephonyManager)getApplicationContext()
    				.getSystemService(TELEPHONY_SERVICE))
    				.getDeviceId()
    				.hashCode()
    				+"",this);
        	break;
        }
        return false;
    }
        
	/** Called when a button has been clicked on by the user
     * @param v Reference to the {android.view.View} of the button
     */
	public void onClick(View v) 
    {
		Editor editor = settings.edit();
		
    	switch(v.getId())
    	{
    	case R.id.button_record:
            //checks if ACCESSIBILITY_SERVICE is activated
    		if(!((AccessibilityManager)getApplicationContext().getSystemService(ACCESSIBILITY_SERVICE)).isEnabled()) {
                if(!startAccessibility())
                	return;
    		}
    		
    		if (main_spinner.getSelectedItemPosition() == 0)
    		{
    			editor.putString("UploadFrequency", "30");
    			myStress_upload.setTimer(getApplicationContext());
    			initializeStart();
    		}
    		else
    		{
    			editor.putString("UploadFrequency", "0");
    			myStress_upload.setTimer(getApplicationContext());
    			initializeStart();
    		}
    		editor.commit();
    		break;
    	}
    }
        
    // local service connection
    private ServiceConnection mConnection = new ServiceConnection() 
    {
  	    public void onServiceConnected(ComponentName className, IBinder service) 
  	    {
  	        // This is called when the connection with the service has been
  	        // established, giving us the service object we can use to
  	        // interact with the service.  Because we have bound to a explicit
  	        // service that we know is running in our own process, we can
  	        // cast its IBinder to a concrete class and directly access it.
  	    	myStress_locally = ((myStress_local.LocalBinder)service).getService();
  	    }

  	    public void onServiceDisconnected(ComponentName className) {
  	        // This is called when the connection with the service has been
  	        // unexpectedly disconnected -- that is, its process crashed.
  	        // Because it is running in our same process, we should never
  	        // see this happen.
  	    	myStress_locally = null;
  	    }
  	};    	
    
    private void initializeStart(){
    	AlertDialog.Builder builder;
    	AlertDialog alert;
        
    	// check if persistent flag is running, indicating the myStress has been running (and would re-start if continuing)
		if (settings.getBoolean("myStress_local::running", false) == true)
		{
    		builder = new AlertDialog.Builder(this);
    		builder.setMessage(getString(R.string.myStress_running_exit))
    			   .setTitle(getString(R.string.myStress_Sensing))
    		       .setCancelable(false)
    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		        	    // clear persistent flag
    			           	Editor editor = settings.edit();
    			           	editor.putBoolean("myStress_local::running", false);
    		                // finally commit to storing values!!
    		                editor.commit();
    		                // stop service
 		    			    stopService(new Intent(myStress, myStress_local.class));
 		    			    finish();
    		           }
    		       })
    		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		                dialog.cancel();
    		           }
    		       });
    		alert = builder.create();
    		alert.show();
		}
		else
		{		           
     		// start measurements now 
    		if (myStress_locally != null)
    		{
    			// merely restart without GUI
    			myStress_locally.Restart(false);
                // service running message
    			if (settings.getBoolean("myStress_local::running", false) == true)
    				Toast.makeText(getApplicationContext(), getString(R.string.myStress_started_local), Toast.LENGTH_LONG).show();     
               	// finish UI
    			finish();
    		}
       	}
    }
    
    private boolean startAccessibility(){
        // Provider not enabled, prompt user to enable it
        final AlertDialog alertDialog = new AlertDialog.Builder(myStress_record_tab.this).create();
        alertDialog.setCancelable(false);
        alertDialog.setTitle(getResources().getString(R.string.accessibility_titel));
        alertDialog.setMessage(getResources().getString(R.string.accessibility_message));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.accessibility_goto), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                alertDialog.cancel();
                Intent myIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                myStress_record_tab.this.startActivity(myIntent);
            }
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.Back), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                alertDialog.cancel();
            }
        });

        alertDialog.show();
        
        return ((AccessibilityManager)getApplicationContext().getSystemService(ACCESSIBILITY_SERVICE)).isEnabled();
    }
}

