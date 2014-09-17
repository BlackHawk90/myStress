/*
Copyright (C) 2013, Dirk Trossen, support@tecvis.co.uk
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

package com.myStress.handlers;

import java.util.concurrent.Semaphore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.myStress.R;
import com.myStress.platform.SensorRepository;

/**
 * Class to read notification related sensors, specifically the NO sensor
 * @see Handler
 */
public class NotificationHandler implements Handler
{
	private Context myStress;
	private Semaphore notify_semaphore 	= new Semaphore(1);
	private String notify_text;
	private boolean shutdown = false;
	
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
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.myStress.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public synchronized byte[] Acquire(String sensor, String query)
	{
		
		// are we shutting down?
		if (shutdown == true)
			return null;
		
		if(sensor.compareTo("NO") == 0){
			wait(notify_semaphore);
			StringBuffer readings = new StringBuffer("NO");
			readings.append(notify_text.replaceAll("'","''"));
			
			return readings.toString().getBytes();
		}
		else{
			return null;
		}
	}
	
	/**
	 * Method to share the last value of the given sensor - here doing nothing
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.myStress.handlers.Handler#Share(java.lang.String)
	 */
	public synchronized String Share(String sensor)
	{		
		return null;		
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol - here doing nothing
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.myStress.handlers.Handler#History(java.lang.String)
	 */
	public synchronized void History(String sensor)
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
	    SensorRepository.insertSensor(new String("NO"), new String("App"), myStress.getString(R.string.NO_d), myStress.getString(R.string.NO_e), new String("txt"), 0, 0, 1, false, 0, this);
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's only arming the semaphore and registering the accessibility broadcast event as well as firing the start event to the accessibility service
	 * @param myStress Reference to the calling {@link android.content.Context}
	 */
	public NotificationHandler(Context myStress)
	{
		this.myStress = myStress;
		
		// arm semaphore
		wait(notify_semaphore);
		
		// register for any input from the accessbility service
		IntentFilter intentFilter = new IntentFilter("com.myStress.accessibility");
        myStress.registerReceiver(SystemReceiver, intentFilter);
        
        // now broadcast the start of the accessibility service
		Intent intent = new Intent("com.myStress.accessibility.start");
		intent.putExtra("start", true);		
		myStress.sendBroadcast(intent);			
	}

	/**
	 * Method to release all handler resources
	 * Here, we unregister the broadcast receiver and release all semaphores
	 * @see com.myStress.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down!
		shutdown = true;
		
		// release all semaphores for unlocking the Acquire() threads
		notify_semaphore.release();
		
		// unregister the broadcast receiver
		myStress.unregisterReceiver(SystemReceiver);

        // now broadcast the stop of the accessibility service
		Intent intent = new Intent("com.myStress.accessibility.start");
		intent.putExtra("start", false);
		myStress.sendBroadcast(intent);				
	}
	
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // if anything sent from the accessbility service
            if (action.equals("com.myStress.accessibility")) 
            {
            	// get mood from intent
            	if(intent.hasExtra("NotifyText")){
            		notify_text = intent.getStringExtra("NotifyText");
                	notify_semaphore.release();		// release semaphore
            	}
            }
        }
    };
}
