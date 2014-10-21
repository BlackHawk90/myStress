/*
Copyright (C) 2011, Dirk Trossen, airs@dirk-trossen.de
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

import java.util.Calendar;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.myStress.R;
import com.myStress.helper.SerialPortLogger;
import com.myStress.platform.SensorRepository;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

public class StressLevelHandler implements Handler
{
	private Context myStress;
	private String Event, old_Event, status;
	private Semaphore stress_semaphore 	= new Semaphore(1);
	private Semaphore meta_semaphore 	= new Semaphore(1);
	private Semaphore snooze_semaphore  = new Semaphore(1);
	private Vibrator vibrator;
	private boolean registered = false, shutdown = false, juststarted = false;
	private boolean processed_sm = false, processed_sl = false;
	private boolean dont_vibrate = false;
	private int polltime, snoozetime;
	private int time1;
	private int time2;
	private int time3;
	private SharedPreferences settings;
	
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
	
	public boolean checkTime(){
		Calendar c = Calendar.getInstance();
		int hour = c.get(Calendar.HOUR_OF_DAY);
		
		if(hour == time1 || hour == time2 || hour == time3) return true;
		else return false;
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
		juststarted = false;
		
		// are we shutting down?
		if (shutdown == true)
			return null;
		
		// not yet registered -> then do so!!
		if (registered == false)
		{
			// check intents and set booleans for discovery
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction("com.myStress.stresslevel");
			intentFilter.addAction("com.myStress.pollstress");
			
	        myStress.registerReceiver(SystemReceiver, intentFilter);
	        registered = true;
	        juststarted = true;
		}
		
		if(sensor.equals("SL"))
		{
			if(!juststarted){
				String snooze = "snooze";
				String exit = "exit";
				if(!checkTime() && (!snooze.equals(status) && !exit.equals(status))) return null;
				
				try{
					Intent startintent = new Intent(myStress, StressLevel_selector.class);
					startintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					myStress.startActivity(startintent);
					// vibrate with pattern
					if(!dont_vibrate)
						vibrator.vibrate(pattern, -1);
					dont_vibrate = false;
				} catch(Exception e){
					Log.e("myStress", e.getMessage());
				}
			}
			else stress_semaphore.release();
			
			wait(stress_semaphore); // block until semaphore available -> fired
			
			if (Event != null)
			{
				// prepare readings
				readings = new StringBuffer(sensor);
				readings.append(Event);
				
				processed_sl=true;
				clear();
				
				Editor editor = settings.edit();
				editor.putBoolean("myStress::snoozed", false);
				editor.commit();
				
				// FIXME: Strings in strings.xml anpassen
				//poll(polltime);
		        
				return readings.toString().getBytes();
			}
			else{
				if(status == null) return null;
				if(status.equals("snooze") || status.equals("exit")){
					try{
						Editor editor = settings.edit();
						editor.putBoolean("myStress::snoozed", true);
						editor.commit();
						
						boolean manually_opened = snooze_semaphore.tryAcquire(snoozetime, TimeUnit.MILLISECONDS);
						if(manually_opened) dont_vibrate = true;
						
						return Acquire("SL",null);
					}catch(Exception e){
						Log.e("myStress", e.getMessage());
					}
				}
				else if(status.equals("skip")){
					Editor editor = settings.edit();
					editor.putBoolean("myStress::snoozed", false);
					editor.commit();
				}
			}
		}
		else if(sensor.equals("SM")){
			wait(meta_semaphore);
			
			if(status != null){
				StringBuffer sm_readings = new StringBuffer(sensor);
				sm_readings.append(status);
				
				processed_sm=true;
				clear();
				
				return sm_readings.toString().getBytes();
			}
		}
		
		return null;
	}
	
	private void clear() {
		if(processed_sm && processed_sl){
			Event = null;
			status = null;
			processed_sm = processed_sl = false;
		}
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
//		dontFIXME: polltime SL changed
		SensorRepository.insertSensor(new String("SL"), new String("Level"), myStress.getString(R.string.SL_d), myStress.getString(R.string.SL_e), new String("str"), 0, 0, 1, false, polltime, this);
		SensorRepository.insertSensor(new String("SM"), new String("Status"), myStress.getString(R.string.SM_d), myStress.getString(R.string.SM_e), new String("str"), 0, 0, 1, false, 0, this);
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's only arming the semaphore and getting a reference to the {@link android.os.Vibrator} service
	 * @param myStress Reference to the calling {@link android.content.Context}
	 */
	public StressLevelHandler(Context myStress)
	{
		this.myStress = myStress;
		
		polltime = Integer.parseInt(myStress.getString(R.string.stresspolltime));
		snoozetime = Integer.parseInt(myStress.getString(R.string.stresssnoozetime));
		time1 = Integer.parseInt(myStress.getString(R.string.time1));
		time2 = Integer.parseInt(myStress.getString(R.string.time2));
		time3 = Integer.parseInt(myStress.getString(R.string.time3));
		
		settings = PreferenceManager.getDefaultSharedPreferences(myStress);
		
		try
		{
			// charge the semaphores to block at next call!
			wait(stress_semaphore);
			wait(meta_semaphore);
			wait(snooze_semaphore);
			
			// get system service for Vibrator
			vibrator = (Vibrator)myStress.getSystemService(Context.VIBRATOR_SERVICE);
			dont_vibrate = false;
			
//			poll(polltime);
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
			status = null;
			myStress.unregisterReceiver(SystemReceiver);
		}
		
		stress_semaphore.release();
		meta_semaphore.release();
		snooze_semaphore.release();
	}
	
//	public void poll(long time){
//	 	Intent intent = new Intent("com.myStress.stressalarm");
//	 	PendingIntent pi = PendingIntent.getBroadcast(myStress, 0, intent, 0);
//	 	AlarmManager am =  (AlarmManager)myStress.getSystemService(Context.ALARM_SERVICE);
//	 	// first cancel any pending intent
//	 	am.cancel(pi);
//	 	// then set another one
//	 	am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+time, pi);
//	}
		
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
        	if (intent.getAction().equals("com.myStress.stresslevel"))
            {
            	if(intent.hasExtra("StressLevel")){
	            	Event = intent.getStringExtra("StressLevel");
            	}
        		status = intent.getStringExtra("StressMeta");
        		stress_semaphore.release();
        		meta_semaphore.release();
				return;
            }
        	else if (intent.getAction().equals("com.myStress.pollstress"))
            {
            	snooze_semaphore.release();
            }
        }
    };
}