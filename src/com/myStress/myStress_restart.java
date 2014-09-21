/*
Copyright (C) 2012-2013, Dirk Trossen, airs@dirk-trossen.de

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

import com.myStress.database.myStress_upload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class myStress_restart extends BroadcastReceiver
{
	// preferences
    private SharedPreferences settings;

	/** Called when the receiver is fired 
     * @param context a pointer to the {@link android.content.Context} of the application
     * @param intent a pointer to the originating {@link android.content.Intent}
     */
    @Override
    public void onReceive(Context context, Intent intent) 
    {
    	String action = intent.getAction();
    	    	
    	// need restart mystress?
        if (action != null)
        {
        	if (action.equals("android.intent.action.PACKAGE_REPLACED"))
	        {
        		// get default preferences
                settings = PreferenceManager.getDefaultSharedPreferences(context);

                if (intent.getDataString().contains("com.myStress"))
                {
        			Log.e("myStress", "myStress was updated!");
	        		if (settings.getBoolean("myStress_local::running", false) == true)
	        		{	
	        		    // start service and connect to it -> then discover the sensors
	        			context.getApplicationContext().startService(new Intent(context, myStress_local.class));
	        			Log.e("myStress", "Restart myStress since it was running when updated!");
	        		}
                }
	        }
        	if (action.equals("android.intent.action.BOOT_COMPLETED")){
        		if (settings.getBoolean("myStress_local::running", false) == true)
        		{	
	    		    // start service and connect to it -> then discover the sensors
	    	        context.getApplicationContext().startService(new Intent(context, myStress_local.class));
			  		myStress_upload.setTimer(context); 
	    			Log.e("myStress", "Restart myStress since reboot");
        		}
        	}
        	if(action.equals(Intent.ACTION_BATTERY_CHANGED)){
        		if (settings.getBoolean("myStress_local::running", false) == true)
        		{	
	    		    // start service and connect to it -> then discover the sensors
	    	        context.getApplicationContext().startService(new Intent(context, myStress_local.class));
			  		myStress_upload.setTimer(context); 
	    			Log.e("myStress", "Restart myStress since battery kill");
        		}
        	}
    	}
    }
}
