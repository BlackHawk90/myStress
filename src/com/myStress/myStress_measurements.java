/*
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de

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

import java.io.File;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore.Images;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.myStress.platform.HandlerManager;

/**
 * Activity to view the current measurements of the ongoing local recording
 *
 * @see AIRS_local
 */
public class myStress_measurements extends Activity implements OnItemClickListener, OnItemLongClickListener
{
	 private TextView mTitle;
	 private TextView mTitle2;
	 private myStress_local myStress_locally;
	 private ListView values;
	 private Activity act;

	 /** Called when the activity is first created. 
	     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
	     */
	 @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        
	        act = this;
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
			setContentView(R.layout.values);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle2 = (TextView) findViewById(R.id.title_right_text);
	        mTitle.setText(R.string.app_name);
		    
			// Find and set up the ListView for values
			values 	= (ListView)findViewById(R.id.valueList);        
	        // set listener for sensor list
			values.setOnItemClickListener(this);
			values.setOnItemLongClickListener(this);
			
	        // bind to service
	        if (bindService(new Intent(this, myStress_local.class), mConnection, 0)==false)
	     		Toast.makeText(getApplicationContext(), getString(R.string.binding_failed), Toast.LENGTH_LONG).show();	        
	    }

	 /** Called when the activity is restarted. 
	     */
	 @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    	// ask service to update value adapter
	    	if (myStress_locally!=null)
	    	{
	    		myStress_locally.setShowValues(true);
	    		myStress_locally.refresh_values();
	    	}
	    }

	 /** Called when the activity is stopped. 
	     */
	 @Override
	    public void onStop() 
	    {
	        super.onStop();
	    	// stop service from updating value adapter
	    	if (myStress_locally!=null)
	    		myStress_locally.setShowValues(false);
	    }

	 /** Called when the activity is destroyed. 
	     */
	 @Override
	    public void onDestroy() 
	    {	    		    	
	        super.onDestroy();
    		unbindService(mConnection);
	    }

	    /**
	     * Called when called {@link android.app.Activity} has finished. See {@link android.app.Activity} how it works
	     * @param requestCode ID being used when calling the Activity
	     * @param resultCode result code being set by the called Activity
	     * @param data Reference to the {@link android.content.Intent} with result data from the called Activity
	     */
	    public void onActivityResult(int requestCode, int resultCode, Intent data) 
	    {
	    	return;
	    }
	    
	    /** Called when the configuration of the activity has changed.
	     * @param newConfig new configuration after change 
	     */
	    @Override
	    public void onConfigurationChanged(Configuration newConfig) 
	    {
	      //ignore orientation change
	      super.onConfigurationChanged(newConfig);
	    }
	    
	    @Override
	    /**
	     * Called for dispatching key events sent to the Activity
	     * @param event Reference to the {@link android.view.KeyEvent} being pressed
	     * @return true, if consumed, false otherwise
	     */
	    public boolean dispatchKeyEvent(KeyEvent event) 
	    {
	 		// key de-pressed?
			if (event.getAction() == KeyEvent.ACTION_UP)
				// is it the BACK key?
				if (event.getKeyCode()==KeyEvent.KEYCODE_BACK)
				{
			    	// stop service from updating value adapter
			    	if (myStress_locally!=null)
			    	{
			    		myStress_locally.setShowValues(false);
//			    		unbindService(mConnection);
			    	}
	                finish();
	                return true;
				}

	        return super.dispatchKeyEvent(event);
	    }
	    
	    /** Called when a button has been long-clicked on by the user
	     * @param parent Reference to parent view
	     * @param view Reference to the View of the button
	     * @param position position within parent view
	     * @param id index in a list view
	     */
	    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
	    {
	    	String value;
	    	
	    	// show info for sensor selected
		    if (myStress_locally != null)
		    {
		    	// do we share media?
		    	if (myStress_locally.getSymbol((int)id).compareTo("MW") == 0)
		    	{
		    		value = myStress_locally.getValue((int)id);
		    		
					String [] tokens = value.split(":");
					// is it a camera picture?
					if (tokens[0] != null)
						if (tokens[1].trim().compareTo("camera") == 0)
						{
							// get filename
							if (tokens[2] != null)
							{
								// cut off '[file]' suffix
								String name = tokens[2].trim().substring(0, tokens[2].length() - " [file]".length());
								// form full path
								File file = new File(HandlerManager.readRMS("MediaWatcherHandler::CameraDirectory", Environment.getExternalStorageDirectory()+"/DCIM/Camera"), name.trim());
								Uri shareUri;
								
								// search for file in media store
								Cursor c = act.getContentResolver().query(Images.Media.EXTERNAL_CONTENT_URI, null, Images.Media.DATA + "=?", new String[] { file.getAbsolutePath() }, null);

								// if found -> share
								if (c.getCount() > 0 && c.moveToFirst())
								{
		                             shareUri = Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, ""+ c.getInt(c.getColumnIndex(Images.Media._ID)));

						 		       // now build and start chooser intent
						             Intent intent = new Intent(Intent.ACTION_SEND);
						                
						    	     intent.setType("*/*");
						    		 intent.putExtra(Intent.EXTRA_STREAM, shareUri);
						             intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.Snapshot_publish) + " (https://market.android.com/details?id=com.myStress)\n");
						    	     intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); 
						    		 act.startActivity(Intent.createChooser(intent,getString(R.string.Snapshot_share)));			
								}
								else
						     		Toast.makeText(getApplicationContext(), getString(R.string.Snapshot_nothing_to_share), Toast.LENGTH_LONG).show();

								c.close();
								
							}
						}
						else
				     		Toast.makeText(getApplicationContext(), getString(R.string.Snapshot_only_pictures), Toast.LENGTH_LONG).show();
		    	}
		    	else
		    	{
		    		// get sharing text first
		    		value = myStress_locally.share((int)id);
		    		// only share if there's something to share
		    		if (value != null)
		    		{
		 		       // now build and start chooser intent
		                Intent intent = new Intent(Intent.ACTION_SEND);
		                intent.setType("text/plain");
		                intent.putExtra(Intent.EXTRA_TEXT, value + "\n\n-------------------------------------------\n" + getString(R.string.Snapshot_publish) + " (https://market.android.com/details?id=com.myStress)\n");
		                act.startActivity(Intent.createChooser(intent,getString(R.string.Measurements_share)));
		    		}
		    	}
		    }
	    	return true;
	    }
	    
	   /**
	    * Called when clicking on a list item
	    * @param av parent view
	    * @param v View of clicked item
	    * @param arg2 don't actually know
	    * @param arg3 item in {@link android.view.ListView} being clicked
	    */
	    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) 
	    {
	    	// show info for sensor selected
		    if (myStress_locally != null)
		    	myStress_locally.show_info((int)arg3);
	    }
	    
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
	  	        if (myStress_locally.isRunning() == false)
		     		Toast.makeText(getApplicationContext(), "AIRS local::Sensing not running!\nThe service might have crashed!", Toast.LENGTH_SHORT).show();
	  	        // now set the adapter for the values
				values.setAdapter(myStress_locally.getMValuesArrayAdapter());	
				
				// set threads to show values and refresh latest they have
				myStress_locally.setShowValues(true);
				myStress_locally.refresh_values();
		        // set title according to paused state of local service
		        if (myStress_locally.isPaused())
			        mTitle2.setText("Local Sensing...Paused");
		        
		        // construct title right side with possible template name
		        if (myStress_locally.getTemplate() != null)
		        	mTitle2.setText("Local Sensing: " + myStress_locally.getTemplate());
		        else
		        	mTitle2.setText("Local Sensing");

	  	    }
	
	  	    public void onServiceDisconnected(ComponentName className) {
	  	        // This is called when the connection with the service has been
	  	        // unexpectedly disconnected -- that is, its process crashed.
	  	        // Because it is running in our same process, we should never
	  	        // see this happen.
	  	    	myStress_locally = null;
	  	    }
	    };
}
