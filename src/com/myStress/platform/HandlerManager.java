/*
Copyright (C) 2005-2006 Nokia Corporation
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

package com.myStress.platform;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.myStress.handlers.*;
import com.myStress.helper.SerialPortLogger;

/**
 * @author trossen
 * 
 *         Initializes the handlers and stores them in static variable later
 *         being used to point to in sensor repository
 */
public class HandlerManager {
	/**
	 * maximum number of handlers supported so far
	 */
	public final static int max_handlers = 24;
	/**
	 * Reference to {@link Handler} entries being instantiated
	 */
	static public Handler handlers[] = new Handler[max_handlers];
	static private int inst_handlers = 0;
	static private SharedPreferences settings;
	static private Editor editor;

	/**
	 * Creates {@link Handler} entries at the start of the remote or local
	 * service
	 * 
	 * @param myStress
	 *            Reference to the {@link android.content.Context} of the
	 *            calling activity
	 * @return true if successful
	 */
	static public boolean createHandlers(Context myStress) {
		// store pointer to preferences
		settings = PreferenceManager.getDefaultSharedPreferences(myStress);
		editor = settings.edit();

		// clear array
		for (int i = 0; i < max_handlers; i++)
			handlers[i] = null;

		inst_handlers = 0;

		// here the handlers are inserted in the field
		// the rule is that raw sensors are inserted first before aggregated
		// sensors are inserted.
		// this is due to the discovery mechanism since aggregated sensor
		// handlers usually check the availability of the raw sensor in order to
		// become 'visible'
		handlers[inst_handlers++] = (Handler) new GPSHandler(myStress);
		handlers[inst_handlers++] = (Handler) new WeatherHandler(myStress);
		handlers[inst_handlers++] = (Handler) new WifiHandler(myStress);
		handlers[inst_handlers++] = (Handler) new CellHandler(myStress);
		handlers[inst_handlers++] = (Handler) new AudioHandler(myStress);
		handlers[inst_handlers++] = (Handler) new SystemHandler(myStress);
		handlers[inst_handlers++] = (Handler) new PhoneSensorHandler(myStress);
		handlers[inst_handlers++] = (Handler) new CalendarHandler(myStress);
		handlers[inst_handlers++] = (Handler) new NotificationHandler(myStress);
		handlers[inst_handlers++] = (Handler) new TextInformationHandler(myStress);
		handlers[inst_handlers++] = (Handler) new CallLogHandler(myStress);
		handlers[inst_handlers++] = (Handler) new StressLevelHandler(myStress);
		handlers[inst_handlers++] = (Handler) new CallAudioHandler(myStress);
		
		
		
		return true;
	}

	/**
	 * Destroys the instantiated {@link Handler} instances
	 */
	static public void destroyHandlers() {
		int i = 0;

		SerialPortLogger.debug("destroying handlers");
		for (i = 0; i < inst_handlers; i++) {
			SerialPortLogger.debug("destroying handler " + String.valueOf(i));
			try {
				handlers[i].destroyHandler();
				handlers[i] = null;
			} catch (Exception e) {
				SerialPortLogger
						.debugForced("AIRS: exception in destroyHandlers with i="
								+ String.valueOf(i));
			}
		}
	}

	/**
	 * Read string from RMS for persistency
	 * 
	 * @param store
	 *            Entry in RMS store
	 * @param defaultString
	 *            Default entry for this entry if it does not exist in store
	 * @return Persistent reading obtained from RMS store
	 */
	static public String readRMS(String store, String defaultString) {
		String value = null;

		try {
			value = settings.getString(store, defaultString);
		} catch (Exception e) {
			SerialPortLogger.debug("ERROR " + "Exception: " + e.toString());
		}
		return value;
	}

	/**
	 * Read long variable from RMS for persistency
	 * 
	 * @param store
	 *            Entry in RMS store
	 * @param defaultint
	 *            Default entry for this entry if it does not exist in store
	 * @return Persistent reading obtained from RMS store
	 */
	static public long readRMS_l(String store, long defaultint) {
		long value = 0;

		try {
			String read = settings.getString(store, Long.toString(defaultint));
			value = Long.parseLong(read);
		} catch (Exception e) {
			SerialPortLogger.debug("ERROR " + "Exception: " + e.toString());
		}
		return value;
	}

	/**
	 * Read int variable from RMS for persistency
	 * 
	 * @param store
	 *            Entry in RMS store
	 * @param defaultint
	 *            Default entry for this entry if it does not exist in store
	 * @return Persistent reading obtained from RMS store
	 */
	static public int readRMS_i(String store, int defaultint) {
		int value = 0;

		try {
			String read = settings.getString(store,
					Integer.toString(defaultint));
			value = Integer.parseInt(read);
		} catch (Exception e) {
			SerialPortLogger.debug("ERROR " + "Exception: " + e.toString());
		}
		return value;
	}

	/**
	 * Read boolean variable from RMS for persistency
	 * 
	 * @param store
	 *            Entry in RMS store
	 * @param defaultint
	 *            Default entry for this entry if it does not exist in store
	 * @return Persistent reading obtained from RMS store
	 */
	static public boolean readRMS_b(String store, boolean defaultint) {
		boolean value = false;

		try {
			value = settings.getBoolean(store, defaultint);
		} catch (Exception e) {
			SerialPortLogger.debug("ERROR " + "Exception: " + e.toString());
		}
		return value;
	}

	/**
	 * Write string variable to RMS for persistency
	 * 
	 * @param store
	 *            Entry in RMS store
	 * @param value
	 *            Value for this entry
	 */
	static public void writeRMS(String store, String value) {
		try {
			// put string into store
			editor.putString(store, value);

			// finally commit to storing values!!
			editor.commit();
		} catch (Exception e) {
			SerialPortLogger.debug("ERROR " + "Exception: " + e.toString());
		}
	}

	/**
	 * Write boolean variable to RMS for persistency
	 * 
	 * @param store
	 *            Entry in RMS store
	 * @param value
	 *            Value for this entry
	 */
	static public void writeRMS_b(String store, boolean value) {
		try {
			// put string into store
			editor.putBoolean(store, value);

			// finally commit to storing values!!
			editor.commit();
		} catch (Exception e) {
			SerialPortLogger.debug("ERROR " + "Exception: " + e.toString());
		}
	}
	
	/**
	 * Write boolean variable to RMS for persistency if no handlers were created
	 * 
	 * @param store
	 *            Entry in RMS store
	 * @param value
	 *            Value for this entry
	 */
	static public void writeRMS_b(String store, boolean value, Context myStress) {
		try {
			settings = PreferenceManager.getDefaultSharedPreferences(myStress);
			editor = settings.edit();
			
			// put string into store
			editor.putBoolean(store, value);

			// finally commit to storing values!!
			editor.commit();
		} catch (Exception e) {
			SerialPortLogger.debug("ERROR " + "Exception: " + e.toString());
		}
	}

	/**
	 * Write long variable to RMS for persistency
	 * 
	 * @param store
	 *            Entry in RMS store
	 * @param value
	 *            Value for this entry
	 */
	static public void writeRMS_l(String store, long value) {
		try {

			// put string into store
			editor.putLong(store, value);

			// finally commit to storing values!!
			editor.commit();
		} catch (Exception e) {
			SerialPortLogger.debug("ERROR " + "Exception: " + e.toString());
		}
	}

	/**
	 * Returns a handler
	 * 
	 * @param handlerName
	 *            Name of the Handler to be returned
	 * @return handler Object
	 */
	static public Handler getHandler(String handlerName) {
		for (int i = 0; i < handlers.length; i++) {
			String className = handlers[i].getClass().toString();
			if (className.contains(handlerName))
				return handlers[i];
		}

		return null;
	}
}
