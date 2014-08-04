/*
Copyright (C) 2011, Dirk Trossen, myStress@dirk-trossen.de

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;

import com.myStress.R;
import com.myStress.helper.SerialPortLogger;
import com.myStress.platform.SensorRepository;

/**
 * Class to read self-annotated mood sensor, specifically the MO sensor
 * @see Handler
 */
public class MoodButtonHandler implements Handler
{
	private Context nors;
	private Semaphore event_semaphore 	= new Semaphore(1);
	private Vibrator vibrator;
	private String mood= null, old_mood = null;
	private boolean registered = false, shutdown = false;
	
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
	 * Here, we register the broadcast receiver to receive the mood widget notification, if not done before
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.myStress.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public byte[] Acquire(String sensor, String query)
	{
		long[] pattern = {0l, 450l, 250l, 450l};
		StringBuffer readings;
		
		// are we shutting down?
		if (shutdown == true)
			return null;

		// mood button?
		if(sensor.compareTo("MO") == 0)
		{
			// not yet registered -> then do so!!
			if (registered == false)
			{
				// check intents and set booleans for discovery
				IntentFilter intentFilter = new IntentFilter("com.myStress.moodbutton");
		        nors.registerReceiver(SystemReceiver, intentFilter);
		        intentFilter = new IntentFilter("com.myStress.moodselected");
		        nors.registerReceiver(SystemReceiver, intentFilter);
		        registered = true;
			}
			
			// block until semaphore available -> fired
			wait(event_semaphore); 

			if (mood != null)
			{
				// prepare readings
				readings = new StringBuffer(sensor);
				readings.append(mood);
					
				// vibrate with pattern
				vibrator.vibrate(pattern, -1);
				
				// store mood
				old_mood = null;
				old_mood = new String(mood);
				
				// garbage collect mood string
				mood = null;
		        
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
	 * @see com.myStress.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{		
		if (old_mood != null)
			return "I'm currently " + old_mood + "!";
		else
			return null;
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol - doing nothing in this handler
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.myStress.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
	}
	
	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.myStress.platform.Sensor} entries will be added to the {@link com.myStress.platform.SensorRepository}
	 * @see com.myStress.handlers.Handler#Discover()
	 * @see com.myStress.platform.Sensor
	 * @see com.myStress.platform.SensorRepository
	 */
	public void Discover()
	{
		SensorRepository.insertSensor(new String("MO"), new String("Mood"), nors.getString(R.string.MO_d), nors.getString(R.string.MO_e), new String("str"), 0, 0, 6, false, 0, this);	    
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's only arming the semaphore and getting a reference to the {@link android.os.Vibrator} service
	 * @param nors Reference to the calling {@link android.content.Context}
	 */
	public MoodButtonHandler(Context nors)
	{
		this.nors = nors;
		try
		{
			// charge the semaphores to block at next call!
			wait(event_semaphore); 

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
	 * @see com.myStress.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down!
		shutdown = true;
		
		if (registered == true)
		{
			mood = null;
			nors.unregisterReceiver(SystemReceiver);
		}
	}
		
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // if mood button widget has been pressed -> start Activity for selecting mood icon
            if (action.equals("com.myStress.moodbutton")) 
            {
    			try
    			{
    	            Intent startintent = new Intent(nors, MoodButton_selector.class);
    	            startintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	            nors.startActivity(startintent);          
    			}
    			catch(Exception e)
    			{
    			}
    			return;
            }
            
            // if mood has been selected, signal to handler
            if (action.equals("com.myStress.moodselected")) 
            {
            	// get mood from intent
            	mood = intent.getStringExtra("Mood");
            	
				event_semaphore.release();		// release semaphore
				return;
            }
        }
    };
}