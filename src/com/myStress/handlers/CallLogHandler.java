/*
Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
Copyright (C) 2014, FIM Research Center, myStress@fim-rc.de
-
This program is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation as version 2.1 of the License.
-
This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
License for more details.
-
You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, Inc.,
59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
*/

package com.myStress.handlers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.myStress.R;
import com.myStress.platform.HandlerManager;
import com.myStress.platform.History;
import com.myStress.platform.SensorRepository;

/** 
 * Class to read random number generator sensors, specifically the Rd sensor
 * @see Handler
 */
public class CallLogHandler implements Handler
{
	private Context myStress;
	// create field that holds acquisition data
	private byte[] readings;
	private int polltime=1000*60*6;
	private Long lastUpdate;
	
	/**
	 * Method to acquire sensor data
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public synchronized byte[] Acquire(String sensor, String query)
	{
		if(sensor.compareTo("PH") == 0)
		{
			String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
			Uri callUri = Uri.parse("content://call_log/calls");
			Cursor cur = myStress.getContentResolver().query(callUri, null, null, null, strOrder);
			
			
			String[] entries = new String[20];
			Long callDate=0L;
			int i=0;
			
			while(cur.moveToNext()){
				callDate = cur.getLong(cur.getColumnIndex(android.provider.CallLog.Calls.DATE));
				if(callDate < lastUpdate) break;
				
				SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.GERMANY);
				String dateString = formatter.format(new Date(callDate));
				
				entries[i] = dateString;
				entries[i] += "::"+cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.DURATION));
				entries[i] += "::"+cur.getString(cur.getColumnIndex(android.provider.CallLog.Calls.TYPE));
				
				i++;
			}
			lastUpdate = System.currentTimeMillis();
			
			StringBuffer reading = new StringBuffer("PH");
			
			if(i==0) readings = null;
			else{
				while(i>0){
					reading.append(entries[i-1]+";");
					i--;
				}
				
				readings = reading.toString().getBytes();
			}
		}
		
		return readings;
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
		History.timelineView(myStress, "Calls", "PH");
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
	    SensorRepository.insertSensor(new String("PH"), new String("Calls"), myStress.getString(R.string.PH_d), myStress.getString(R.string.PH_e), new String("txt"), 0, 0, 65535, true, polltime, this);
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's reading the interval from the preferences
	 * @param airs Reference to the calling {@link android.content.Context}
	 */
	public CallLogHandler(Context myStress)
	{
		this.myStress = myStress;
		// read polltime
		polltime = HandlerManager.readRMS_i("CallLogHandler::SamplingRate", 60*6) * 1000;
		lastUpdate = System.currentTimeMillis();
	}
	
	/**
	 * Method to release all handler resources - doing nothing here
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
	}
}
