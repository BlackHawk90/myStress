/*
Copyright (C) 2013, TecVis LP, support@tecvis.co.uk

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
import com.myStress.helper.SerialPortLogger;
import com.myStress.platform.SensorRepository;

/**
 * Class to read blood pressure sensors, specifically the BP sensor
 * 
 * @see Handler
 */
public class BloodPressureButtonHandler implements Handler {
	private Context myStress;
	private Semaphore event_semaphore = new Semaphore(1);
	private boolean registered = false, shutdown = false;
	private StringBuffer BP_reading;
	private String blood_pressure;

	private void wait(Semaphore sema) {
		try {
			sema.acquire();
		} catch (Exception e) {
		}
	}

	/**
	 * Method to acquire sensor data Here, we register for the broadcast from
	 * the widget, if not done before
	 * 
	 * @param sensor
	 *            String of the sensor symbol
	 * @param query
	 *            String of the query to be fulfilled - not used here
	 * @see com.myStress.handlers.Handler#Acquire(java.lang.String,
	 *      java.lang.String)
	 */
	public byte[] Acquire(String sensor, String query) {
		// are we shutting down?
		if (shutdown == true)
			return null;

		BP_reading = new StringBuffer("BT");

		// not yet registered -> then do so!!
		if (registered == false) {
			// check intents and set booleans for discovery
			IntentFilter intentFilter = new IntentFilter(
					"com.myStress.bloodpressurebutton");
			myStress.registerReceiver(SystemReceiver, intentFilter);
			intentFilter = new IntentFilter("com.myStress.bloodpressure");
			myStress.registerReceiver(SystemReceiver, intentFilter);
			registered = true;
		}

		wait(event_semaphore); // block until semaphore available -> fired

		// now append reading itself
		BP_reading.append(blood_pressure);

		return BP_reading.toString().getBytes();
	}

	/**
	 * Method to share the last value of the given sensor
	 * 
	 * @param sensor
	 *            String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.myStress.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor) {
		return "My last blood pressure was " + blood_pressure;
	}

	/**
	 * Method to view historical chart of the given sensor symbol - doing
	 * nothing in this handler
	 * 
	 * @param sensor
	 *            String of the symbol for which the history is being requested
	 * @see com.myStress.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor) {
	}

	/**
	 * Method to discover the sensor symbols support by this handler As the
	 * result of the discovery, appropriate {@link com.myStress.platform.Sensor}
	 * entries will be added to the
	 * {@link com.myStress.platform.SensorRepository}
	 * 
	 * @see com.myStress.handlers.Handler#Discover()
	 * @see com.myStress.platform.Sensor
	 * @see com.myStress.platform.SensorRepository
	 */
	public void Discover() {
		SensorRepository.insertSensor(new String("BP"), new String("mmHg"),
				myStress.getString(R.string.BP_d), myStress.getString(R.string.BP_e),
				new String("txt"), 0, 0, 1, false, 0, this);
	}

	/**
	 * Constructor, allocating all necessary resources for the handler Here,
	 * it's only arming the semaphore
	 * 
	 * @param myStress
	 *            Reference to the calling {@link android.content.Context}
	 */
	public BloodPressureButtonHandler(Context myStress) {
		this.myStress = myStress;
		try {
			// charge the semaphores to block at next call!
			wait(event_semaphore);
		} catch (Exception e) {
			SerialPortLogger.debugForced("Semaphore!!!!");
		}
	}

	/**
	 * Method to release all handler resources Here, we unregister the broadcast
	 * receiver
	 * 
	 * @see com.myStress.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler() {
		// we are shutting down
		shutdown = true;

		if (registered == true) {
			myStress.unregisterReceiver(SystemReceiver);
		}
	}

	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// if mood button widget has been pressed -> start Activity for
			// selecting mood icon
			if (action.equals("com.myStress.bloodpressurebutton")) {
				try {
					Intent startintent = new Intent(myStress,
							BloodPressureButton_selector.class);
					startintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					myStress.startActivity(startintent);
				} catch (Exception e) {
				}
				return;
			}

			// If event button pressed, signal to Acquire()
			if (action.equals("com.myStress.bloodpressure")) {
				// get mood from intent
				blood_pressure = intent.getStringExtra("BloodPressure");

				event_semaphore.release(); // release semaphore
				return;
			}
		}
	};
}
