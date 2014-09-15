/*
Copyright (C) 2004-2006 Nokia Corporation
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de
Copyright (C) 2013, TecVis LP, support@tecvis.co.uk
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.myStress.database.myStress_database;
import com.myStress.helper.SerialPortLogger;
import com.myStress.helper.Waker;
import com.myStress.platform.HandlerManager;
import com.myStress.platform.Sensor;
import com.myStress.platform.SensorRepository;

/**
 * Class to implement the local recording
 *
 * @see myStress_remote
 * @see myStress_main
 * @see android.app.Service
 */
@SuppressLint("HandlerLeak")
public class myStress_local extends Service
{
	private List<String> activatedSensors;
	// states for handler 
	private static final int REFRESH_VALUES 		= 1;
	private static final int SHOW_NOTIFICATION 	= 2;
	private static final int BATTERY_KILL 		= 3;
    private static final String LINE = "LINE";
    private static final String TEXT = "TEXT";

    private boolean show_values=false;
    private HandlerThread[] threads = null;
    private int no_threads = 0;
    private Context myStress = null; // Context of the main Service
    private String template; // Template being used for starting the recording, if any used
	private int	no_values = 0;
    private boolean localIntent_b;
    private boolean localStore_b;
    private boolean localDisplay_b;
    private int Reminder_i;
    private boolean Vibrate, Lights;
    private String LightCode;
    private NotificationManager mNotificationManager;
    private int BatteryKill_i;
    private boolean Wakeup_b;
	private String url = "myStress_values";
	private File mconn, path;
	private BufferedOutputStream os2;
	private int numberSensors = 0;
    private ListView sensors;
//    private boolean discovered = false; // Flag if discovery has been done
    private boolean running = false; // Flag if myStress is recording
//    private boolean restarted = false; // Flag if myStress has been restarted
    private boolean started = false; // Flag if myStress has been started as a service already
//    public boolean start = false; // Flag that myStress should be started initially
    private boolean paused = false; // Flag that recording should be paused (where possible)
    private boolean registered = false; // Flag that sensors have been registered
    private ArrayList<SensorEntry> mSensorsArrayList;
    private MyCustomBaseAdapter mSensorsArrayAdapter;
    private ArrayAdapter<String> mValuesArrayAdapter; // List of Values, being used in visualisation activity
    private long nextDay;		// milliseconds for next day starting
    private final IBinder mBinder = new LocalBinder(); // This is the object that receives interactions from clients
    private VibrateThread Vibrator;
    private Notification notification;
    private WakeLock wl = null;
    // database variables
    static private myStress_database database_helper; // Reference to current myStress_database
    static private SQLiteDatabase myStress_storage; // Reference to myStress database
    
	// private thread for reading the sensor handlers
	 private class HandlerThread implements Runnable
	 {
		 	private Sensor current;
		 	private String line;
		 	private String values_output = null;
		 	private long values_time;
		 	private int number_values = 0;
		 	private Thread thread;
		 	private boolean interrupted = false, pause = false;;
		 	private boolean started = true;	
		 	private String value_intent;
		    private long nextDay;		// milliseconds for next day starting

			protected void sleep(long millis) 
			{
				try 
				{
					Thread.sleep(millis);
				} 
				catch (InterruptedException ignore) 
				{
					interrupted = true;
				}
			}
			/***********************************************************************
			 Function    : HandlerThread()
			 Input       : current sensor, number for sensor in UI element
			 Output      :
			 Return      :
			 Description : stores parameters of this query
			***********************************************************************/
			HandlerThread(Sensor current, int j)
			{
				// copy parameters 
				this.current = current;
				line = Integer.toString(j);
				values_output = new String(current.Description + " : - [" + current.Unit + "]");
			
				// get current day and set time to last millisecond of that day for next day indicator
		        Calendar cal = Calendar.getInstance();
		        cal.set(Calendar.HOUR_OF_DAY, 23);
		        cal.set(Calendar.MINUTE, 59);
		        cal.set(Calendar.MILLISECOND, 999);
		        nextDay = cal.getTimeInMillis();
		         
				(thread = new Thread(this, "myStress: " + current.Symbol)).start();;
			}

			// return true if sensor has historical data
			public boolean hasHistory()
			{
				return current.hasHistory;
			}
			
			public String share()
			{
				return current.handler.Share(current.Symbol);
			}

			public String info()
			{
				Calendar cal = Calendar.getInstance();
				
				cal.setTimeInMillis(values_time);
				if(number_values>0)
					return new String("'" + current.Description + "'" + getString(R.string.sensed) + " " + number_values + " " + getString(R.string.sensed2) + " " + DateFormat.format("dd MMM yyyy k:mm:ss", cal) + " " + getString(R.string.sensed3) + values_output);
				else
					return new String("'" + current.Description + "'" + getString(R.string.sensed) + " " + number_values + " mal");
			}
			
			private void output(String text)
			{
				this.output(text, false);
			}
			
			private void output(String text, boolean refresh)
			{				
				// save output for later
				if (refresh == false)
				{
					values_output = new String(text);	// store output for later
					number_values++;					// count number of sensed values
					// store timestamp
					values_time = System.currentTimeMillis();
				}
				
				// shall values been shown?
				if (show_values == true)
				{
					if (text != null)
					{
				        Message msg = mHandler.obtainMessage(REFRESH_VALUES);
				        Bundle bundle = new Bundle();
				        bundle.putString(TEXT, text);
						bundle.putString(LINE, line);
						msg.setData(bundle);
				        mHandler.sendMessage(msg);
					}
				}
			}
			
			public void refresh()
			{
				output(values_output, true);
			}

			/***********************************************************************
			 Function    : HandlerTask()
			 Input       : 
			 Output      :
			 Return      :
			 Description : task for resolving a query - to be started by the 
			 			   Acquisition component (usually in the callback for a dialog)!!
			 			   if sending NOTIFY fails, thread returns, i.e., ends
			 			   NOTIFY could fail due to termination of dialog (e.g., BYE) 
			***********************************************************************/
			public void run() 
			{
					 byte[] sensor_data=null;
			 		 int    integer;
			 		 double scaler;
			 		 int 	i;
			 		 String fileOut = null, fileIMG = null;
		
			 		 scaler = 1;
			 		 if (current.scaler>0)
			 			 for (i=0;i<current.scaler;i++)
			 				 scaler *=10;
			 		 else
			 			 for (i=current.scaler;i<0;i++)
			 				 scaler /=10;
			 			
			 		 try
			 		 {
				 		 while(interrupted == false)
				 		 {	 
				 			// pause while told to
				 			while(pause == true)
				 				sleep(500);
		
				 			// handle sensor status
				 			switch(current.status)
				 			{
				 			// is sensor not valid anymore -> terminate!
				 			case Sensor.SENSOR_INVALID:
				 				if (current.statusString != null)
				 					output(current.Symbol + " : " + getString(R.string.Sensor_invalid) + " " + current.statusString, false);
				 				else
				 					output(current.Symbol + " : " + getString(R.string.Sensor_invalid2), true);

				 				// now wait 
					        	wait();
				 				break;
				 			case Sensor.SENSOR_SUSPEND:
				 				if (current.statusString != null)
				 					output(current.Symbol + " : " + getString(R.string.Sensor_suspended) + " " + current.statusString, false);
				 				else
				 					output(current.Symbol + " : " + getString(R.string.Sensor_suspended2), false);
				 				
								SerialPortLogger.debug("HandlerThread for " + current.Symbol + ": suspended, now waiting");

								while (current.status == Sensor.SENSOR_SUSPEND)
									sleep(5000);
		    					
		    					output(current.Symbol + " : - [" + current.Unit + "]", false);
								SerialPortLogger.debug("HandlerThread for " + current.Symbol + ": woken up again");
				 				break;
				 			case Sensor.SENSOR_VALID:		 			
					    		// acquire latest value
				    			sensor_data = current.handler.Acquire(current.Symbol, null);
				    			// anything?
			    				if (sensor_data!=null)
			    				{
			    					// here we handle int/float sensor values
					    			if(current.type.equals("int") || current.type.equals("float"))
					    			{
					    				// do long int first
					    				integer = ((int)(sensor_data[2] & 0xFF) << 24) 
					     		         | ((int)(sensor_data[3] & 0xff) << 16) 
					      				 | ((int)(sensor_data[4] & 0xFF) << 8) 
					      				 | ((int)sensor_data[5] & 0xFF);
					    				
						    			// set text item in value field
					    				if (localDisplay_b == true)
					    				{
						    				if (current.scaler != 0)
						    					output(current.Description + " : " + String.valueOf((double)integer*scaler) + " [" + current.Unit + "]");
						    				else
						    					output(current.Description + " : " + String.valueOf(integer) + " [" + current.Unit + "]");
					    				}
					    				else
					    				{
					    					no_values++;
					    					output("# of values : " + String.valueOf(no_values));
					    				}
					    				
					    				// need to store locally?
					    				if (localStore_b == true)
					    				{
					    					if (current.scaler !=0)
					    						fileOut = new String("'"+ String.valueOf(System.currentTimeMillis()) + "','" + current.Symbol + "','" + String.valueOf((double)integer*scaler) + "'");
					    					else
					    						fileOut = new String("'" + String.valueOf(System.currentTimeMillis()) + "','" + current.Symbol + "','" + String.valueOf(integer) + "'");
					    				}
					    				
					    				// need to create myStress intent?
					    				if (localIntent_b == true)
						    				if (current.scaler != 0)
						    					value_intent = new String(String.valueOf((double)integer*scaler));
						    				else
						    					value_intent = new String(String.valueOf((double)integer));
					    			}
					    			
			    					// here we handle txt sensor values
					    			if(current.type.equals("txt") || current.type.equals("str"))
					    			{
						    			// set text item in value field
					    				if (localDisplay_b == true)
					    				{
					    					output(current.Description + " : " + new String(sensor_data, 2, sensor_data.length - 2) + " [" + current.Unit + "]");
					    				}
					    				else
					    				{
					    					no_values++;
					    					output("# of values : " + String.valueOf(no_values));
					    				}			    					
			
					    				// need to store locally?
					    				if (localStore_b == true)
				    				    	fileOut = new String("'" + String.valueOf(System.currentTimeMillis()) + "','" + current.Symbol + "','" + new String(sensor_data, 2, sensor_data.length - 2) + "'");
					    				
					    				// need to create myStress intent?
					    				if (localIntent_b == true)
					    					value_intent = new String(new String(sensor_data, 2, sensor_data.length - 2));
					    			}
			
				   					// here we handle img and arr sensor values
					    			if(current.type.equals("img") || current.type.equals("arr"))
					    			{
						    			// set text item in value field, here only the length of the sensor value field
					    				if (localDisplay_b == true)
					    				{
					    					output(current.Description + " : " + Integer.toString(sensor_data.length));
					    				}
					    				else
					    				{
					    					no_values++;
					    					output("# of values : " + String.valueOf(no_values));
					    				}			    					
			
					    				// need to store locally?
					    				if (localStore_b == true)
					    				{
					    				    try 
					    				    {
					    				    	fileIMG = new String(url + String.valueOf(System.currentTimeMillis()) + "_" + current.Symbol + ".jpg" );
					    				    	// open file with read/write - otherwise, it will through a security exception (no idea why)
					    				        mconn = new File(path, fileIMG);
					    			    		os2 = new BufferedOutputStream(new FileOutputStream(mconn, true));
			
					    			    		// store sensor data
					    			    		os2.write(sensor_data, 2, sensor_data.length-2);
					    			    		os2.close();
					    			    		// store filename in recording file
					    				    	fileOut = new String("'" + String.valueOf(System.currentTimeMillis()) + "','" + current.Symbol + "','" + fileIMG + "'");

							    				// need to create myStress intent?
							    				if (localIntent_b == true)
							    					value_intent = new String(fileIMG);
					    				    } 
					    				    catch(Exception e)
					    				    {
					    			    		debug("Exception in opening file connection");
					    				    }
					    				}					    				
					    			}
					    			
					    			// anything to send via intents?
					    			if (localIntent_b == true)
					    				if (value_intent != null)
					    				{
					    					// send broadcast intent to signal end of selection to mood button handler
					    					Intent intent = new Intent("com.myStress.sensor." + current.Symbol);
					    					intent.putExtra("Value", value_intent);
					    					
					    					sendBroadcast(intent);

					    					// free memory
					    					value_intent = null;
					    				}
					    			
					    			// anything to write to file?
					    			if (fileOut != null)
					    			{
				    				    try
				    				    {
				    				    	long timemilli = System.currentTimeMillis();
				    				    	// write into database
				    				    	execStorage(timemilli, "INSERT into myStress_values (Timestamp, Symbol, Value) VALUES ("+ fileOut + ")");
				    				    	
				    				    	// write sensor value in table for faster retrieval later!
				    				    	if (started == true || timemilli > nextDay)
				    				    	{
				    				    		// save time of writing
				    				    		current.time_saved = timemilli;

				    							// is the current data at the next day?
				    							if (current.time_saved > nextDay)
				    							{
				    								 // get current day and set time to last millisecond of that day 
				    						         Calendar cal = Calendar.getInstance();
				    						         cal.set(Calendar.HOUR_OF_DAY, 23);
				    						         cal.set(Calendar.MINUTE, 59);
				    						         cal.set(Calendar.MILLISECOND, 999);
				    						         nextDay = cal.getTimeInMillis();				    						         
				    							}
				    				    		
				    				    		// try to enter into database
				    				    		try
				    				    		{
				    				    			execStorage(0, "INSERT into myStress_sensors_used (Timestamp, Symbol) VALUES ('" + String.valueOf(current.time_saved) + "','" + current.Symbol + "')");
				    				    		}
				    				    		catch(Exception e)
				    				    		{
				    				           	 	execStorage(0, myStress_database.DATABASE_TABLE_CREATE3);
				    				           	 	execStorage(0, myStress_database.DATABASE_TABLE_INDEX3);
				    				    			execStorage(0, "INSERT into myStress_sensors_used (Timestamp, Symbol) VALUES ('" + String.valueOf(current.time_saved) + "','" + current.Symbol + "')");
				    				    		}
				    				    		
				    				    		started = false;
				    				    	 }
				    				    	 else
				    				    		// if it's GPS or media watcher, data can be removed in Storica, so check if it's still there!
				    				    		if(current.Symbol.compareTo("GI") == 0 || current.Symbol.compareTo("MW") == 0)
					    				    	{
				    				    			try
				    				    			{
				    				    				// try to retrieve the previously saved entry
						    							String query = new String("SELECT Symbol from 'myStress_sensors_used' WHERE Timestamp = " + String.valueOf(current.time_saved) + " AND Symbol='" + current.Symbol + "'");
						    							Cursor values = myStress_storage.rawQuery(query, null);
	
						    							// has old entry been deleted -> then save again
						    							if (values.getCount() == 0)
						    							{
							    				    		current.time_saved = System.currentTimeMillis();

							    				    		try
							    				    		{
							    				    			execStorage(0, "INSERT into myStress_sensors_used (Timestamp, Symbol) VALUES ('" + String.valueOf(current.time_saved) + "','" + current.Symbol + "')");
							    				    		}
							    				    		catch(Exception e)
							    				    		{
							    				    		}
						    							}
						    							
						    							// and return memory
						    							values.close();
				    				    			}
					    				    		catch(Exception e)
					    				    		{
					    				    			Log.e("myStress", "error re-writing removed sensors:" + e.toString());
						    				    		current.time_saved = System.currentTimeMillis();

						    				    		try
						    				    		{
						    				    			execStorage(0, "INSERT into myStress_sensors_used (Timestamp, Symbol) VALUES ('" + String.valueOf(current.time_saved) + "','" + current.Symbol + "')");
						    				    		}
						    				    		catch(Exception ex)
						    				    		{
						    				    		}
					    				    		}
					    				    		
					    				    	}
				    				    }
				    					catch(Exception e) 
				    					{    					
				    					}	
				    					
				    					// free memory
				    					fileOut = null;
					    			}
			    				}
			    				
			    				// are we waiting for the next poll?
			    				if (current.polltime>0 && interrupted ==false)
			    					Thread.sleep(current.polltime);
			    				break;
				 			}
				 		 }
					}
					catch(Exception e)
					{
						debug("HandlerThread: interrupted and terminating 1..." + current.Symbol);
						return;
					}
					
					debug("HandlerThread: interrupted and terminating 2..."  + current.Symbol);
					return;
			}
	 }
    
	/**
	 * Sleep function 
	 * @param millis
	 */
	private void sleep(long millis) 
	{
		Waker.sleep(millis);
	}
    
	private void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}

    public class LocalBinder extends Binder 
    {
    	myStress_local getService() 
        {
            return myStress_local.this;
        }
    }
    
    /**
     * Returns current instance of myStress_local Service to anybody binding to myStress_local
     * @param intent Reference to calling {@link android.content.Intent}
     * @return current instance to service
     */
	@Override
	public IBinder onBind(Intent intent) 
	{
		SerialPortLogger.debug("myStress_local::bound service!");
		return mBinder;
	}
		
	/**
	 * Called when system is running low on memory
	 * @see android.app.Service
	 */
	@Override
	public void onLowMemory()
	{
		SerialPortLogger.debugForced("myStress_local::low memory warning from system - about to get killed!");
	}

	/**
	 * Called when starting the service the first time around
	 * @see android.app.Service
	 */
	@Override
	public void onCreate() 
	{
		myStress = this.getApplicationContext();
		
		SerialPortLogger.debugForced("myStress_local::created service!");
		
		// let's see if we need to restart
		Restart(true);
	}

	/**
	 * Synchronised method for writing to myStress database from the recording threads being created
	 * @param milli timestamp of the recording
	 * @param query the SQL query being executed, formed in the recording threads
	 */
	public synchronized void execStorage(long milli, String query)
	{
		if (myStress_storage == null)
			return;
		
		synchronized(myStress_storage)
		{
			if (myStress_storage == null)
				return;

			myStress_storage.execSQL(query);
		
			// is the current data at the next day?
			if (milli > nextDay)
			{
				 // get current day and set time to last millisecond of that day 
		         Calendar cal = Calendar.getInstance();
		         cal.set(Calendar.HOUR_OF_DAY, 23);
		         cal.set(Calendar.MINUTE, 59);
		         cal.set(Calendar.MILLISECOND, 999);
		         int year = cal.get(Calendar.YEAR);
		         int month = cal.get(Calendar.MONTH) + 1;
		         int day = cal.get(Calendar.DAY_OF_MONTH);
		         nextDay = cal.getTimeInMillis();
		         
		         // mark date now as having values
		         myStress_storage.execSQL("INSERT into myStress_dates (Year, Month, Day, Types) VALUES ('"+ String.valueOf(year) +  "','" + String.valueOf(month) + "','" + String.valueOf(day) + "','1')");
			}
		}
	}
	
	public synchronized int countReceivedSMS(long begin, long end){
		Cursor cur =  myStress_storage.rawQuery("SELECT COUNT(Timestamp) FROM myStress_values WHERE Timestamp > '"+end+"' AND Timestamp < 'end' AND Symbol = 'SR'", null);
		if(cur != null)
			cur.moveToFirst();
		return cur.getInt(0);
	}
	
	public synchronized int countSentSMS(long begin, long end){
		Cursor cur = myStress_storage.rawQuery("SELECT COUNT(Timestamp) FROM myStress_values WHERE Timestamp > '"+end+"' AND Timestamp < 'end' AND Symbol = 'SS'", null);
		if(cur != null)
			cur.moveToFirst();
		return cur.getInt(0);
	}
	
	public synchronized int[] countSentMessages(long begin, long end){
		Cursor cur = myStress_storage.rawQuery("SELECT Values FROM myStress_values WHERE Timestamp > '"+end+"' AND Timestamp < 'end' AND Symbol = 'TI'", null);
		int[] msg = new int[4];
		if(cur == null)
			return null;
		cur.moveToFirst();
		while(!cur.isLast()){
			if(cur.getString(0).contains("com.whatsapp")) msg[0]++; // Sent WhatsApp messages
			else if(cur.getString(0).contains("com.facebook.orca")) msg[1]++; // Sent Facebook Messages 
			else if(cur.getString(0).contains("com.facebook.katana")) msg[2]++; // Facebook Posts
			else if(cur.getString(0).contains("com.sec.android.email") || cur.getString(0).contains("com.htc.android.mail")) msg[3]++; // Sent E-Mails
			cur.moveToNext();
		}
		return msg;
	}
	
	public synchronized double avgAmplitude(long begin, long end){
		Cursor cur = myStress_storage.rawQuery("SELECT Values FROM myStress_values WHERE Timestamp > '"+end+"' AND Timestamp < 'end' AND Symbol = 'AS'", null);
		double sum = 0, count = 0;
		if(cur == null)
			return 0;
		cur.moveToFirst();
		while(!cur.isLast()){
			sum += cur.getDouble(0);
			count++;
			cur.moveToNext();
		}
		return sum/count;
	}
	
	public synchronized int countNotifications(long begin, long end){
		Cursor cur = myStress_storage.rawQuery("SELECT COUNT(Timestamp) FROM myStress_values WHERE Timestamp > '"+end+"' AND Timestamp < 'end' AND Symbol = 'NO' AND Value <> 'com.sec.android.providers.downloads'", null);
		if(cur != null)
			cur.moveToFirst();
		return cur.getInt(0);
	}
	
	public String[][] getMainArray(long begin, long end){
		String[][] arr = new String[8][2];
		arr[0][0] = "Versendete SMS:";
		arr[1][0] = "Empfangene SMS:";
		arr[2][0] = "Versendete WhatsApp-Nachrichten:";
		arr[3][0] = "Versendete Facebook-Nachrichten:";
		arr[4][0] = "Eigene Facebook-Posts:";
		arr[5][0] = "Versendete E-Mails:";
		arr[6][0] = "Erhaltene Benachrichtigungen:";
		arr[7][0] = "Durchschnittliche Umgebungslautstärke:";
		
		int[] temp = countSentMessages(begin, end);
		arr[0][1] = "" + countSentSMS(begin, end);
		arr[1][1] = "" + countReceivedSMS(begin, end);
		arr[2][1] = "" + temp[0];
		arr[3][1] = "" + temp[1];
		arr[4][1] = "" + temp[2];
		arr[5][1] = "" + temp[3];
		arr[6][1] = "" + countNotifications(begin, end);
		arr[7][1] = "" + avgAmplitude(begin, end);
		
		return arr;
	}
	
	private void start_myStress_local()
	{
		// find out whether or not to remind of running
		Reminder_i 			= HandlerManager.readRMS_i("Reminder", 0) * 1000;
		Vibrate    			= HandlerManager.readRMS_b("Vibrator", true);
		Lights     			= HandlerManager.readRMS_b("Lights", true);
		LightCode  			= HandlerManager.readRMS("LightCode", "00ff00");

		// find out whether or not to kill based on battery condition
		BatteryKill_i = HandlerManager.readRMS_i("BatteryKill", 10);

		// find out whether or not to wakeup the sensing on user activity
		Wakeup_b = HandlerManager.readRMS_b("Wakeup", false);

		// find out whether or not to store locally
		localIntent_b = HandlerManager.readRMS_b("myStressIntents", true);

		// find out whether or not to store locally
		localStore_b = HandlerManager.readRMS_b("LocalStore", true);

		// find out whether or not to display locally
		localDisplay_b = HandlerManager.readRMS_b("localDisplay", true);
		
		// if to store locally -> try to open write file on memory card
		if (localStore_b == true)
		{
		    try 
		    {	            
	            // get database
	            database_helper = new myStress_database(this.getApplicationContext());
	            myStress_storage = database_helper.getWritableDatabase();
//	            myStress_storage = SQLiteDatabase.openDatabase(this.getDatabasePath(database_helper.DATABASE_NAME).toString(), null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
	            // have tables been created?
	            if (HandlerManager.readRMS_b("myStress_local::TablesExists", false) == false)
	            {
	           	 	execStorage(0, myStress_database.DATABASE_TABLE_CREATE);
	           	 	execStorage(0, myStress_database.DATABASE_TABLE_CREATE2);
	                HandlerManager.writeRMS_b("myStress_local::TablesExists", true);
	            }
		    } 
		    catch(Exception e)
		    {
	    		debug("myStress_local::Exception in opening file connection");
//		    	localStore_b = false;
		    }
		}
	}

	/**
	 * Called when service is destroyed, e.g., by stopService()
	 * Here, we tear down all recording threads, close all handlers, unregister receivers for battery signal and close the thread for indicating the recording
	 */
	@Override
	public void onDestroy() 
	{
		int i;
		
		SerialPortLogger.debug("myStress_local::destroying service!");

		// get database sync to squeeze out all other threads for killing them
		if (myStress_storage != null)
		{
			synchronized(myStress_storage)
			{
			   	 // kill Handlers and threads
			   	 if (started == true)
			   	 {
			   		 SerialPortLogger.debug("myStress_local::terminating handlers");

			   		 // first kill handlers!
			   		 HandlerManager.destroyHandlers();	
	
			   		 // then kill threads
			   		 try
			   		 {
			   			SerialPortLogger.debug("myStress_local::terminating " + String.valueOf(no_threads) + " HandlerThreads");
				   		 // interrupt all threads for terminated
				   		 for (i = 0; i<no_threads;i++)
				   			 if (threads[i] != null)
				   			 {
					   			 if (threads[i] != null)
					   			 {
					   				 threads[i].interrupted = true;
					   				 threads[i].thread.interrupt();
					   			 }
				   				 threads[i] = null;
				   			 }
				   		 
				   		 // if Vibrator is running, stop it!
				   		 if (Vibrator!=null)
				   		 {
				   			Vibrator.stop = true;
				   			Vibrator.thread.interrupt(); 
				   			Vibrator.thread = null;
				   		 }
			   		 }
			   		 catch(Exception e)
			   		 {
			   			 SerialPortLogger.debugForced("myStress_local::Exception when terminating Handlerthreads!");
			   		 }			   		 	
			   	 }
			   	 
			   	 // is local storage ongoing -> close file!
			   	 if (localStore_b == true)
			   	 {
			   		 try
			   		 {
					    myStress_storage.close();
					    myStress_storage = null;
			   		 }
			   		 catch(Exception e)
			   		 {	   
			   			 SerialPortLogger.debugForced("myStress_local::Exception when closing DB!");
			   		 }
			   	 } 			   	 
			 }
		}
	
		// and kill persistent flag
        HandlerManager.writeRMS_b("myStress_local::running", false, myStress);
                  
        // kill internal flag
        running = false;
         
        // anything to send via intents?
        if (localIntent_b == true)
			{
				// send broadcast intent that myStress has started
				Intent intent = new Intent("com.myStress.local.stopped");				
				sendBroadcast(intent);
			}

	   	// release wake lock if held
	   	if (wl != null)
	   		 if (wl.isHeld() == true)
	   			 wl.release();
	   	 
	   	// if registered for screen activity or battery level -> unregister
	   	if (registered == true)
	         unregisterReceiver(mReceiver);	   	 
  	
 		SerialPortLogger.debug("myStress_local::finished destroying service!");
 		
	}
	
	@Override
	/**
	 * Called when startService() is invoked by other parts of myStress (myStress_record_tab as well as myStress_shortcut)
	 * The sequence of calling this appropriately (be careful to not change this)
	 * 1. startService()
	 * 2. bindService()
	 * 3. set start = true and call startService() again
	 * 4. service.Discover() -> sets discovered == true
	 * 5. startService() again for measurements 
	 * @params intent Reference to the calling {@link Intent}
	 * @params flags Flags set by the caller
	 * @params startId ID created per calling of the service (identifying the caller)
	 * @see myStress_shortcut
	 * @see myStress_record_tab
	 */
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		SerialPortLogger.debugForced("myStress_local::started service ID " + Integer.toString(startId));
		
		// return if intent is null -> service was restarted
		if (intent == null)
		{
			// let's see if we need to restart
			Restart(true);
			
			return Service.START_STICKY;
		}
		
		return Service.START_STICKY;
	}

	/**
	 * Asks threads to refresh latest value
	 */
	 public void refresh_values()
	 {
		 int i;
		 
		 for (i=0; i<no_threads;i++)
			 threads[i].refresh();
	 }

	 /**
	  * Asks threads to pause
	  */
	 public void pause_threads()
	 {
		 int i;
		 
		 for (i=0; i<no_threads;i++)
			 threads[i].pause = true;
		 // signal paused sate
		 paused = true;
	 }

	 /**
	  * Gets Value of the j-th thread
	  * @param j index to thread which should provide the value
	  * @return String of the current recording
	  */
	 public String getValue(int j)
	 {
		 return threads[j].values_output;
	 }
	 
	 /**
	  * Gets Sensor symbol that the j-th thread is recording
	  * @param j index to thread which should provide the symbol
	  * @return String of the sensor symbol, see http://tecvis.co.uk/software/myStress/internal-workings for list of supported sensor symbols
	  */
	 public String getSymbol(int j)
	 {
		 return threads[j].current.Symbol;
	 }
	 
	 /**
	  * Sharing info for sensor entry that j-th thread is recording
	  * @param j index to thread which should provide the sharing string
	  * @return String to be shared or null if nothing to share
	  */
	 public String share(int j)
	 {
		return threads[j].share();
	 }

	 /**
	  * Show timeline, map or similar for sensor entry that j-th thread is recording
	  * @param j index to thread which should show the information
	  */
	 public void show_info(int j)
	 {
		if (threads[j].hasHistory() == true)
			threads[j].current.handler.History(threads[j].current.Symbol);
		else
		{
			String info = threads[j].info();
	  		Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG).show();
		}
	 }

	 /**
	  * Ask threads to pause recording. Where callback receivers are used, such pausing is not supported!
	  */
	 public void resume_threads()
	 {
		 int i;
		 
		 for (i=0; i<no_threads;i++)
			 threads[i].pause = false;
		 // signal paused sate
		 paused = false;
	 }

	 /**
	  * Sets all sensors being selected in the sensor list - called from UI in {@link myStress_main}
	  */
	 public void selectall()
	 {
		 int i;
		 
    	 for (i=0;i<numberSensors;i++)
    		 sensors.setItemChecked(i, true);
     }
	
	 /**
	  * Unselects all sensors in the sensor list - called from UI in {@link myStress_main}
	  */
	 public void unselectall()
	 {    	 
		 int i;

		 // clear checkboxes in sensor list
    	 for (i=0;i<numberSensors;i++)
    		 sensors.setItemChecked(i, false);
     }

	 /**
	  * Builds {@link android.app.AlertDialog} with sensor information
	  */
	 public void sensor_info()
	 {
		 float scaler;
		 int i;
	 	 Sensor current = SensorRepository.root_sensor;
	 	 String infoText = new String();
 		 
	 	 while(current != null)
		 {
	 		 scaler = 1;
	 		 if (current.scaler>0)
	 			 for (i=0;i<current.scaler;i++)
	 				 scaler *=10;
	 		 else
	 			 for (i=current.scaler;i<0;i++)
	 				 scaler /=10;

    		infoText = infoText + current.Symbol + " : " + current.Description + " [" + current.Unit + "] from " + (float)current.min*scaler + " to " + (float)current.max*scaler + " \n\n"; 
	        current = current.next;
	     }
	 		
	 	 AlertDialog.Builder builder = new AlertDialog.Builder(myStress);	
	 	 builder.setMessage(infoText)
    		       .setTitle("Sensor Repository")
    		       .setNeutralButton("OK", new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
 		                dialog.cancel();
    		           }
    		       });	
	 	 AlertDialog alert = builder.create();
	 	 alert.show();	 		
	 }
	 
	/**
	 * Sets up the main threads to run, acquire values, store them locally and display them (if wanted)
	 * @param restarted is this measurements restarted (true) or not (false). 
	 * @return true if successfully started, false otherwise
	 */
	@SuppressLint("Wakelock")
	@SuppressWarnings("deprecation")
	public boolean startMeasurements(boolean restarted)
	 {
		 int i, j =0;
		 Sensor current = null;
		 
		 // count values to be displayed		 
		 for (i=0;i<sensors.getCount();i++)
			if (mSensorsArrayList.get(i).checked == true)
				j++;
		 	 	    
		 notification = new Notification(R.drawable.notification_icon, getString(R.string.Started_myStress), System.currentTimeMillis());

		 // create pending intent for starting the activity
		 PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, myStress_measurements.class),  Intent.FLAG_ACTIVITY_NEW_TASK);
		 // has it been started or restarted?
		 if (restarted == false)
			 notification.setLatestEventInfo(getApplicationContext(), getString(R.string.myStress_Local_Sensing), getString(R.string.running_since), contentIntent);
		 else
			 notification.setLatestEventInfo(getApplicationContext(), getString(R.string.myStress_Local_Sensing), getString(R.string.restarted_since), contentIntent);
		 // set the time again for ICS
		 notification.when = System.currentTimeMillis();
		 // don't allow clearing the notification
		 //FIXME Prozess im Vordergrund sorgt dafür, dass Notification nicht weg geht
		 notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		 
		 SharedPreferences   settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		 boolean showNotification = settings.getBoolean("showNotification", true);
		 if(showNotification)
			 startForeground(1, notification);

		 
         // store start timestamp
         HandlerManager.writeRMS_l("myStress_local::time_started", System.currentTimeMillis());
		 
		 // Find and set up the ListView for values
		 mValuesArrayAdapter = new ArrayAdapter<String>(this, R.layout.sensor_list);

		 // create handler threads for measurements
		 no_threads = j;
		 threads = new HandlerThread[j];

 		 // do I need local display of values?
 		 if (localDisplay_b == true)
 		 {		 			 
 			 for (i=0;i<sensors.getCount();i++)
 			 {
 				if (mSensorsArrayList.get(i).checked == true)
 				{
 	 				current = SensorRepository.findSensor(mSensorsArrayList.get(i).description.substring(0,2));
	 				if (current != null)
	 				{
				    	if (current.type.equals("float") || current.type.equals("int") || current.type.equals("txt") || current.type.equals("str") )
					        mValuesArrayAdapter.add(current.Description + " : - [" + current.Unit + "]");
				    	else
					        mValuesArrayAdapter.add(current.Description + " : - ");
	 				}
 				}
 			 } 			 
 		 }
 		 else	// if no individual values, show at least number of values acquired and memory available   
 			 mValuesArrayAdapter.add("# of values : - ");
 		  		 
		 // now create measurement threads
		 for (i=0, j=0;i<sensors.getCount();i++)
		 {
			current = SensorRepository.findSensor(mSensorsArrayList.get(i).description.substring(0,2));
			if (current != null)
			{
				if (mSensorsArrayList.get(i).checked == true)
				{
 		    		// create reading thread
 		    		if (localDisplay_b == true)
 		    			threads[j] = new HandlerThread(current, j);
 		    		else
 		    			threads[j] = new HandlerThread(current, 0);
 	    			j++;
 	    			// save setting in RMS
 	                HandlerManager.writeRMS("myStress_local::" + current.Symbol, "On");
 				}
		    	else
	               HandlerManager.writeRMS("myStress_local::" + current.Symbol, "Off");
			}
		 } 			  		 
 		 		 
		 if (Reminder_i>0)
			 Vibrator = new VibrateThread();
		 
		 // if wakeup by user activity -> register receiver for screen on/off
		 if (Wakeup_b == true)
		 {
			 IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
			 registerReceiver(mReceiver, filter);
			 filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			 registerReceiver(mReceiver, filter);
			 registered = true;
		 }
		 else	// otherwise get wake lock for keeping running all the time!
		 {
			 // create new wakelock
			 PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
			 
			 wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myStress Local Lock");
			 wl.acquire();
		 }
		 
		 // need battery monitor?
		 if (BatteryKill_i > 0)
		 {
			 // register intent for watching battery
			 IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			 registerReceiver(mReceiver, filter);			 
			 registered = true;
		 }

		 // store persistently that myStress is running
         HandlerManager.writeRMS_b("myStress_local::running", true);
         running = true;
         
		 // get current day and set time to last millisecond of that day 
         Calendar cal = Calendar.getInstance();
         cal.set(Calendar.HOUR_OF_DAY, 23);
         cal.set(Calendar.MINUTE, 59);
         cal.set(Calendar.MILLISECOND, 999);
         int year = cal.get(Calendar.YEAR);
         int month = cal.get(Calendar.MONTH) + 1;
         int day = cal.get(Calendar.DAY_OF_MONTH);
         nextDay = cal.getTimeInMillis();

         // mark date now as having values
         try
         {
	         execStorage(0, "INSERT into myStress_dates (Year, Month, Day, Types) VALUES ('"+ String.valueOf(year) +  "','" + String.valueOf(month) + "','" + String.valueOf(day) + "','1')");	
         }
         catch(Exception e)
         {
        	 execStorage(0, "CREATE TABLE IF NOT EXISTS myStress_dates (Year INT, Month INT, Day INT, Types INT);");
	         execStorage(0, "INSERT into myStress_dates (Year, Month, Day, Types) VALUES ('"+ String.valueOf(year) +  "','" + String.valueOf(month) + "','" + String.valueOf(day) + "','1')");	
         }
         
         // anything to send via intents?
         if (localIntent_b == true)
			{
				// send broadcast intent that myStress has started
				Intent intent = new Intent("com.myStress.local.started");
				
				sendBroadcast(intent);
			}

		 return true;
	 }
	 
	/**
	 * Save all sensor selections in the UI sensor list as persistent values in the preference file
	 */
	 public void saveSelections()
	 {
		 int i;
		 Sensor current = null;

		 // now create measurement threads
		 for (i=0;i<mSensorsArrayList.size();i++)
		 {
			current = SensorRepository.findSensor(mSensorsArrayList.get(i).description.substring(0,2));
			if (current != null)
			{
    			// save setting in RMS
				if (mSensorsArrayList.get(i).checked == true)
 	                HandlerManager.writeRMS("myStress_local::" + current.Symbol, "On");
		    	else
	                HandlerManager.writeRMS("myStress_local::" + current.Symbol, "Off");
			}
		 } 	
	 }
	 
	 /**
	  * Called to start all sensor handlers and inquire the discovered sensors from these handlers
	  * The function also populates the UI sensor list and shows the appropriate view
	  * This function needs calling from a UI thread, e.g., from myStress_record_tab
	  * @param myStress Reference to the current {@link android.app.Activity}
	  */
	 @SuppressWarnings("unchecked")
	public void Discover(Activity myStress)
	 {
			int i;
//			String sensor_setting;
			Sensor current;
			activatedSensors = Arrays.asList(getResources().getString(R.string.activatedSensors).split(";"));

			this.myStress = myStress;
			
			// create timer/alarm handling
			Waker.init(this);
			
			// create handlers
			HandlerManager.createHandlers(this.getApplicationContext());
			
			// start other stuff
			start_myStress_local();

			started = true;

			myStress.setContentView(R.layout.sensors);
 
			// populate list
	        sensors 	= (ListView)myStress.findViewById(R.id.sensorList);
	        sensors.setItemsCanFocus(false); 
		    sensors.setDividerHeight(2);
		    sensors.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	        mSensorsArrayList = new ArrayList<SensorEntry>();
	        mSensorsArrayAdapter = new MyCustomBaseAdapter(this);
	        sensors.setAdapter(mSensorsArrayAdapter);

	        // do discovery on all handlers
			SensorRepository.deleteSensor();
		    // run through all handlers and discover locally first
		    for (i=0; i<HandlerManager.max_handlers;i++)
		    {
		        // is there any handler entry?
		        if (HandlerManager.handlers[i] != null)
		            // call discovery function of handler 
		            HandlerManager.handlers[i].Discover();
		    }
		    		    
		    // now build actual forms
		    current = SensorRepository.root_sensor;
		    i= 0 ;
		    while(current != null)
		    {
	    		// add new sensor choice field
		    	SensorEntry entry = new SensorEntry();
		    	
		    	entry.description = current.Symbol + " (" + current.Description + ")";
		    	entry.explanation = current.Explanation;

		    	// try to read RMS
//		    	sensor_setting = HandlerManager.readRMS("myStress_local::" + current.Symbol, "Off");
		    	// set selected index to setting in RMS
		    	
		    	
//		    	if (sensor_setting.compareTo("On") == 0 || debug.contains((String)current.Symbol))
		    	if (activatedSensors.contains((String)current.Symbol))
		    		entry.checked = true;
		    	else
		    		entry.checked = false;

		    	mSensorsArrayList.add(entry);

		    	// count sensors
		    	i++;
		        current = current.next;
		    }
		    	   
		    Collections.sort(mSensorsArrayList, new SortBasedOnName());

		    // save number of sensors for later!
		    numberSensors = i;

	        // notify ListView of data set change!
	        mSensorsArrayAdapter.notifyDataSetChanged();

			// signal that it is discovered
//			discovered = true;
	 }	

	 /**
	  * Similar to the Discover() function. However, no UI is served here as well as the handlers are assumed to have been created already.
	  * This function is usually called in the restart modus.
	  */
	 @SuppressWarnings("unchecked")
	private void ReDiscover()
	 {
			int i;
//			String sensor_setting;
			Sensor current;
			activatedSensors = Arrays.asList(getResources().getString(R.string.activatedSensors).split(";"));
			
			myStress = this.getApplicationContext();
     
	        sensors 	= (ListView)new ListView(this.getApplicationContext());
	        sensors.setItemsCanFocus(false); 
		    sensors.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		    sensors.setDividerHeight(2);
	        mSensorsArrayList = new ArrayList<SensorEntry>();
	        mSensorsArrayAdapter = new MyCustomBaseAdapter(this);
	        sensors.setAdapter(mSensorsArrayAdapter);

	        // do discovery on all handlers
			SensorRepository.deleteSensor();
		    // run through all handlers and discover locally first
		    for (i=0; i<HandlerManager.max_handlers;i++)
		    {
		        // is there any handler entry?
		        if (HandlerManager.handlers[i] != null)
		            // call discovery function of handler 
		            HandlerManager.handlers[i].Discover();
		    }
		    		    
		    // now build actual forms
		    current = SensorRepository.root_sensor;
		    i= 0 ;
		    while(current != null)
		    {
	    		// add new sensor choice field
		    	SensorEntry entry = new SensorEntry();
		    	
		    	entry.description = current.Symbol + " (" + current.Description + ")";
		    	entry.explanation = current.Explanation;
		    	
		    	// try to read RMS
//		    	sensor_setting = HandlerManager.readRMS("myStress_local::" + current.Symbol, "Off");
		    	// set selected index to setting in RMS
		    	
//		    	if (sensor_setting.compareTo("On") == 0)
		    	if(activatedSensors.contains((String)current.Symbol))
		    		entry.checked = true;
		    	else
		    		entry.checked = false;
		    	
		    	mSensorsArrayList.add(entry);

		    	// count sensors
		    	i++;
		        current = current.next;
		    }
		    	 
		    Collections.sort(mSensorsArrayList, new SortBasedOnName());

		    // save number of sensors for later!
		    numberSensors = i;

	        // notify ListView of data set change!
	        mSensorsArrayAdapter.notifyDataSetChanged();
	        
			// signal that it is discovered
//			discovered = true;
	 }	
	 
	 /**
	  * Restarts the myStress_local recording service, calling the Rediscover() function, restarting all recordings threads and then make the service sticky
	  * @param check Should the service been running (true) and was it therefore restarted?
	  */
	 public void Restart(boolean check)
	 {
		boolean p_running;
		SharedPreferences   settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

		// return if it's already running
		if (running == true)
			return;
		
		try
		{
			p_running	= settings.getBoolean("myStress_local::running", false);
			// should the service been running and was it therefore re-started?
			if (p_running == check)
			{
				SerialPortLogger.debugForced("myStress_local::service was running before, trying to restart recording!");

				if (started == false)
				{
					// set debug state
			   		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));
	
			   		// init Waker resources
			   		Waker.init(this);
			   		
					// create handlers
					HandlerManager.createHandlers(this.getApplicationContext());	
					started = true;
					
					// start other stuff
					start_myStress_local();
				}

				SerialPortLogger.debugForced("myStress_local::re-created handlers");

				// re-discover the sensors
				ReDiscover();
				
				SerialPortLogger.debugForced("myStress_local::re-discovered sensors");

				// start the measurements as being restarted, i.e., it takes the latest discovery and selection that is stored persistently
				running = startMeasurements(check);

				SerialPortLogger.debugForced("myStress_local::re-started measurements");
				
				// restart service in order to make it service
		        startService(new Intent(this, myStress_local.class));
		        
				SerialPortLogger.debugForced("myStress_local::re-started service to make it sticky");
			}

		}
		catch(Exception e)
		{
			SerialPortLogger.debugForced("myStress_local::Restart():ERROR " +  "Exception: " + e.toString());
		}		 
	 }
	 
	 // Vibrate watchdog
	 private class VibrateThread implements Runnable
	 {
		 private Thread thread;
		 private boolean stop = false;
		 
		 VibrateThread()
		 {
			// save thread for later to stop 
			(thread = new Thread(this)).start();			 
		 }
		 
		 public void run()
		 {
			 long vibration[] = {0,200,0};
			 
			 // get power manager
			 PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			 
			 // get notification manager
			 mNotificationManager = (NotificationManager)myStress.getSystemService(Context.NOTIFICATION_SERVICE); 

			 while (stop == false)
			 {		
				 // sleep for the agreed time
			     try
			     {
			    	 Thread.sleep(Reminder_i);

			    	 // only shows "still running" notification when screen is off
			    	 if (pm.isScreenOn() == false)
			    	 {
					     // prepare notification to user by changing the existing notification
					     if (Vibrate == true)
					    	 notification.vibrate			 = vibration;
						 
						 if (Lights == true)
						 {
							 notification.ledOffMS = 0;
							 notification.ledOnMS = 750;
							 notification.ledARGB   = 0xff000000 | Integer.decode(LightCode); // Integer.valueOf(LightCode, 16); 
							 notification.flags     |= Notification.FLAG_SHOW_LIGHTS; 
						 }
			              
						 // now shoot off alert by updating the existing recording notification
						 mNotificationManager.notify(1, notification);   
						 sleep(750);
	
						 // switch off vibrate and lights and update notification again
						 notification.vibrate = null;
						 notification.flags &= ~Notification.FLAG_SHOW_LIGHTS;
						 mNotificationManager.notify(1, notification);   						 
			    	 }
			     }
			     catch(Exception e)
			     {
			    	 debug("myStress_local::Vibrate thread terminated : " + e.toString());
			     }
			 }
		 }
	 }
	 
	 // The Handler that gets information back from the other threads, updating the values for the UI
	 private final Handler mHandler = new Handler() 
     {
        @SuppressWarnings("deprecation")
		@Override
        public void handleMessage(Message msg) 
        {
        	String position;
        	int j;
        	
            switch (msg.what) 
            {
            case REFRESH_VALUES:
            	// parse line from message
            	j = Integer.parseInt(msg.getData().getString(LINE));
            	// refresh appropriate line with given text
		        mValuesArrayAdapter.setNotifyOnChange(false);
	            position = mValuesArrayAdapter.getItem(j);
	            mValuesArrayAdapter.insert(msg.getData().getString(TEXT), j);
	            mValuesArrayAdapter.remove(position);
	            mValuesArrayAdapter.notifyDataSetChanged();
	            break;  
            case SHOW_NOTIFICATION:
               	Toast.makeText(getApplicationContext(), msg.getData().getString(TEXT), Toast.LENGTH_LONG).show();   
               	break;
            case BATTERY_KILL:
            	// stop foreground service
            	stopForeground(true);
            	
            	// now create new notification
            	NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				Notification notification = new Notification(R.drawable.notification_icon, getString(R.string.myStress_killed), System.currentTimeMillis());
       		 	Intent notificationIntent = new Intent(getApplicationContext(), myStress_main.class);
       		 	PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
       			notification.setLatestEventInfo(getApplicationContext(), getString(R.string.myStress_Local_Sensing), getString(R.string.killed_at) + " " + Integer.toString(BatteryKill_i) + "% " + getString(R.string.battery) + "...", contentIntent);
       			
       			// give full fanfare
       			notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
       			notification.ledARGB = 0xffff0000;
       			notification.ledOffMS = 1000;
       			notification.ledOnMS = 1000;
       			
       			mNotificationManager.notify(17, notification);
       			
       			// stop service now!
            	stopSelf();
            	break;
            default:  
            	break;
            }
        }
     };
     
     private final BroadcastReceiver mReceiver = new BroadcastReceiver() 
     {
         @Override
         public void onReceive(Context context, Intent intent) 
         {
        	 int Battery = 100;
        	 
        	 // screen off -> pause sensing
        	 if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
        		 pause_threads();
        	 // screen on -> resume sensing
        	 if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
        		 resume_threads();     
             
        	 // if battery changed...
             if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) 
             {
 	            int rawlevel = intent.getIntExtra("level", -1);
 	            int scale = intent.getIntExtra("scale", -1);
 	            if (rawlevel >= 0 && scale > 0) 
 	                Battery = (rawlevel * 100) / scale;
 	            
 	            // need to trigger battery kill action?
 	            if (Battery < BatteryKill_i)
 	            {
			        Message msg = mHandler.obtainMessage(BATTERY_KILL);
			        mHandler.sendMessage(msg);
 	            }
             }

         }
     };  
     
 	private class ViewHolder 
	{
 		TextView description;
 		TextView explanation;
 		CheckBox checked;
	}
 	
    private class SensorEntry
    {
    	String description;
    	String explanation;
    	boolean checked;
    }

    @SuppressWarnings("rawtypes")
	public class SortBasedOnName implements Comparator
    {
	    public int compare(Object o1, Object o2) 
	    {
	        return ((SensorEntry)o1).description.compareTo(((SensorEntry)o2).description);
	    }
    }
    
	 // Custom adapter for two line text list view with imageview (icon), defined in handlerentry.xml
   	private class MyCustomBaseAdapter extends BaseAdapter 
   	{
   		 private LayoutInflater mInflater;

   		 public MyCustomBaseAdapter(Context context) 
   		 {
   			 mInflater = LayoutInflater.from(context);
   		 }

   		 public int getCount() 
   		 {
   			 return mSensorsArrayList.size();
   		 }

   		 public SensorEntry getItem(int position) 
   		 {
   			 return mSensorsArrayList.get(position);
   		 }

   		 public long getItemId(int position) 
   		 {
   			 return position;
   		 }

   		 @SuppressLint("InflateParams")
		public View getView(int position, View convertView, ViewGroup parent) 
   		 {
   			 
   			 SensorEntry current;
   			 ViewHolder holder;
   			 
			 // get current sensor entry
   			 current = mSensorsArrayList.get(position);
   			 if (convertView == null)
   			 {
   	   			 // inflate view
	   			 convertView = mInflater.inflate(R.layout.sensorselection, null);
	   			 // now get all view information
	   			 holder = new ViewHolder();
	   			 holder.description = (TextView) convertView.findViewById(R.id.selection_description);
	   			 holder.explanation = (TextView) convertView.findViewById(R.id.selection_explanation);
	   			 holder.checked = (CheckBox)convertView.findViewById(R.id.selection_check);
	   			 // and save holder entry in tag
	   			 convertView.setTag(holder);
   			 }
   			 else
   				 holder = (ViewHolder)convertView.getTag();

   			 // set view information
   			 holder.description.setText(current.description);
   			 holder.explanation.setText(current.explanation);
   			 // take care that we get notified of user selections
   			 holder.checked.setTag(current);
   			 holder.checked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                 public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
                 {
                	 ((SensorEntry)buttonView.getTag()).checked = isChecked;
                 }
               });
   			 holder.checked.setChecked(current.checked);
   			 
   			 return convertView;
   		 }
   	}

	public void setShowValues(boolean show_values) {
		this.show_values = show_values;
	}

	public String getTemplate() {
		return template;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isPaused() {
		return paused;
	}

	public ArrayAdapter<String> getMValuesArrayAdapter() {
		return mValuesArrayAdapter;
	}

	public static SQLiteDatabase getMyStressStorage() {
		return myStress_storage;
	}
}
