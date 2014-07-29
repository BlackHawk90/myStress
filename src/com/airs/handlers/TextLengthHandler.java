/*
Copyright (C) 2013, Dirk Trossen, support@tecvis.co.uk

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
package com.airs.handlers;

import java.util.concurrent.Semaphore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.airs.R;
import com.airs.platform.SensorRepository;

/**
 * Class to read notification related sensors, specifically the NO sensor
 * @see Handler
 */
public class TextLengthHandler implements Handler
{
	private Context airs;
	
//	private Semaphore notify_semaphore 	= new Semaphore(1);
//	private Semaphore key_semaphore = new Semaphore(1);
//	private Semaphore speed_semaphore = new Semaphore(1);
	private Semaphore length_semaphore = new Semaphore(1);
	
//	private String notify_text;
//	private String keylog;
	private int textlength;
//	private double typingspeed;
	
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
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public synchronized byte[] Acquire(String sensor, String query)
	{
		
		// are we shutting down?
		if (shutdown == true)
			return null;

		Log.w("AIRS", "Acquire "+sensor);
		
//		if(sensor.compareTo("NO") == 0){
//			wait(notify_semaphore);
//			StringBuffer readings = new StringBuffer("NO");
//			readings.append(notify_text.replaceAll("'","''"));
//			
//			return readings.toString().getBytes();
//		}
//		else if(sensor.compareTo("KL") == 0){
//			wait(key_semaphore);
//			StringBuffer readings_kl = new StringBuffer("KL");
//			readings_kl.append(keylog);
//			
//			return readings_kl.toString().getBytes();
//		}
		if(sensor.compareTo("TL") == 0){
			wait(length_semaphore);
			byte[] reading = new byte[4 + 2];
			reading[0] = (byte)sensor.charAt(0);
			reading[1] = (byte)sensor.charAt(1);
			reading[2] = (byte)((textlength>>24) & 0xff);
			reading[3] = (byte)((textlength>>16) & 0xff);
			reading[4] = (byte)((textlength>>8) & 0xff);
			reading[5] = (byte)(textlength & 0xff);
			
			return reading;
		}
//		else if(sensor.compareTo("TS") == 0){
//			wait(speed_semaphore);
//			StringBuffer readings = new StringBuffer("TS");
//			readings.append(typingspeed);
//			
//			return readings.toString().getBytes();
//		}
		else{
			return null;
		}
	}
	
	/**
	 * Method to share the last value of the given sensor - here doing nothing
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public synchronized String Share(String sensor)
	{		
		return null;		
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol - here doing nothing
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public synchronized void History(String sensor)
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
//	    SensorRepository.insertSensor(new String("KL"), new String("text"), airs.getString(R.string.KL_d), airs.getString(R.string.KL_e), new String("txt"), 0, 0, 1, false, 0, this);
//	    SensorRepository.insertSensor(new String("NO"), new String("text"), airs.getString(R.string.NO_d), airs.getString(R.string.NO_e), new String("txt"), 0, 0, 1, false, 0, this);
	    SensorRepository.insertSensor(new String("TL"), new String("chars"), airs.getString(R.string.TL_d), airs.getString(R.string.TL_e), new String("int"), 0, 0, 10000, false, 0, this);
//	    SensorRepository.insertSensor(new String("TS"), new String("chars/s"), airs.getString(R.string.TS_d), airs.getString(R.string.TS_e), new String("float"), 0, 0, 20, false, 0, this);
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's only arming the semaphore and registering the accessibility broadcast event as well as firing the start event to the accessibility service
	 * @param airs Reference to the calling {@link android.content.Context}
	 */
	public TextLengthHandler(Context airs)
	{
		this.airs = airs;
		
		// arm semaphore
//		wait(notify_semaphore);
//		wait(key_semaphore);
		wait(length_semaphore);
//		wait(speed_semaphore);
		
		// register for any input from the accessbility service
		IntentFilter intentFilter = new IntentFilter("com.airs.accessibility");
        airs.registerReceiver(SystemReceiver, intentFilter);
        
        // now broadcast the start of the accessibility service
		Intent intent = new Intent("com.airs.accessibility.start");
		intent.putExtra("start", true);		
		airs.sendBroadcast(intent);			
	}

	/**
	 * Method to release all handler resources
	 * Here, we unregister the broadcast receiver and release all semaphores
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down!
		shutdown = true;
		
		// release all semaphores for unlocking the Acquire() threads
//		notify_semaphore.release();
//		key_semaphore.release();
		length_semaphore.release();
//		speed_semaphore.release();
		
		// unregister the broadcast receiver
		airs.unregisterReceiver(SystemReceiver);

        // now broadcast the stop of the accessibility service
		Intent intent = new Intent("com.airs.accessibility.start");
		intent.putExtra("start", false);
		airs.sendBroadcast(intent);				
	}
	
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // if anything sent from the accessbility service
            if (action.equals("com.airs.accessibility")) 
            {
            	// get mood from intent
//            	if(intent.hasExtra("NotifyText")){
//            		notify_text = intent.getStringExtra("NotifyText");
//                	notify_semaphore.release();		// release semaphore
//            	}
//            	if(intent.hasExtra("KeyLogger")){
//            		keylog =  intent.getStringExtra("KeyLogger");
//            		key_semaphore.release();
//            	}
            	if(intent.hasExtra("TextLength")){
            		textlength = Integer.parseInt(intent.getStringExtra("TextLength"));
            		length_semaphore.release();
            	}
//            	if(intent.hasExtra("TypingSpeed")){
//            		typingspeed = Double.parseDouble(intent.getStringExtra("TypingSpeed"));
//            		speed_semaphore.release();
//            	}
            }
        }
    };
}
