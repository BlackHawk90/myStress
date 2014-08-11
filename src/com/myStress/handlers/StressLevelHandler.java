/*
Copyright (C) 2011, Dirk Trossen, airs@dirk-trossen.de

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
package com.myStress.handlers;

import java.util.concurrent.Semaphore;

import com.myStress.R;
import com.myStress.helper.SerialPortLogger;
import com.myStress.platform.SensorRepository;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.util.Log;

/** 
 * Class to read event annotation sensor, specifically the EB sensor
 * @see Handler
 */
public class StressLevelHandler implements Handler
{
	private Context nors;
	private String Event, old_Event;
	private Semaphore stress_semaphore 	= new Semaphore(1);
	private Vibrator vibrator;
	private boolean registered = false, shutdown = false;
	private int polltime = 60000;
	
	private void wait(Semaphore sema)
	{
		try
		{
			sema.acquire();
		}
		catch(Exception e)
		{
		}
	}
	
	/**
	 * Method to acquire sensor data
	 * Here, we register the broadcast receiver to receive the event widget notification, if not done before
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public byte[] Acquire(String sensor, String query)
	{
		long[] pattern = {0l, 450l, 250l, 450l, 250l, 450l};
		StringBuffer readings;

		// are we shutting down?
		if (shutdown == true)
			return null;

		// event button?
		if(sensor.compareTo("SL") == 0)
		{
			// not yet registered -> then do so!!
			if (registered == false)
			{
				// check intents and set booleans for discovery
				IntentFilter intentFilter = new IntentFilter("com.myStress.stresslevel");
		        nors.registerReceiver(SystemReceiver, intentFilter);
//		        intentFilter = new IntentFilter("com.myStess.eventselected");
//		        nors.registerReceiver(SystemReceiver, intentFilter);
		        registered = true;
			}
			
			try{
				Intent startintent = new Intent(nors, StressLevel_selector.class);
				startintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				nors.startActivity(startintent);
			} catch(Exception e){
				Log.e("myStress", e.getMessage());
			}
            
			wait(stress_semaphore); // block until semaphore available -> fired

			if (Event != null)
			{
				// prepare readings
				readings = new StringBuffer(sensor);
				readings.append(Event);
				
				// vibrate with pattern
				vibrator.vibrate(pattern, -1);
				
				// garbage collect mood string
				Event = null;
		        
				return readings.toString().getBytes();
			}
			else 
				return null;
		}
		
		return null;
	}
	
	/**
	 * Method to share the last value of the given sensor
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{		
		if (old_Event != null)
			return "My last event was " + old_Event + "!";
		else
			return null;
	}

	/**
	 * Method to view historical chart of the given sensor symbol - doing nothing in this handler
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
	}

	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.airs.platform.Sensor} entries will be added to the {@link com.airs.platform.SensorRepository}
	 * @see com.airs.handlers.Handler#Discover()
	 * @see com.airs.platform.Sensor
	 * @see com.airs.platform.SensorRepository
	 */
	public void Discover()
	{
		SensorRepository.insertSensor(new String("SL"), new String("Event"), nors.getString(R.string.SL_d), nors.getString(R.string.SL_e), new String("str"), 0, 0, 1, false, polltime, this);
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's only arming the semaphore and getting a reference to the {@link android.os.Vibrator} service
	 * @param nors Reference to the calling {@link android.content.Context}
	 */
	public StressLevelHandler(Context nors)
	{
		this.nors = nors;
		try
		{
			// charge the semaphores to block at next call!
			wait(stress_semaphore); 

			// get system service for Vibrator
			vibrator = (Vibrator)nors.getSystemService(Context.VIBRATOR_SERVICE);			
		}
		catch(Exception e)
		{
			SerialPortLogger.debugForced("Semaphore!!!!");
		}
	}
	
	/**
	 * Method to release all handler resources
	 * Here, we unregister the broadcast receiver
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down!
		shutdown = true;
		
		if (registered == true)
		{
			Event = null;
			nors.unregisterReceiver(SystemReceiver);
		}
	}
		
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            if (intent.getAction().equals("com.myStress.stresslevel")) 
            {
            	Event = intent.getStringExtra("StressLevel");

            	stress_semaphore.release();		// release semaphore
				return;
            }
        }
    };
}