/*
Copyright (C) 2012, Dirk Trossen, myStress@dirk-trossen.de

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

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;

import com.myStress.R;
import com.myStress.platform.HandlerManager;
import com.myStress.platform.SensorRepository;

/**
 * Class to read calendar sensors, specifically the CA sensor
 * 
 * @see Handler
 */
@SuppressLint("NewApi")
public class CalendarHandler implements Handler {
	private Context myStress;
	// configuration data
	private int polltime = 1000 * 60 * 6;
	private long currentRead = 0;
	private String[] calendars;
	private boolean no_calendars = false, shutdown = false;

	// calendar data
	private StringBuffer reading;

	/**
	 * Method to acquire sensor data
	 * 
	 * @param sensor
	 *            String of the sensor symbol
	 * @param query
	 *            String of the query to be fulfilled - not used here
	 * @see com.myStress.handlers.Handler#Acquire(java.lang.String,
	 *      java.lang.String)
	 */
	public synchronized byte[] Acquire(String sensor, String query) {
		// are we shutting down?
		if (shutdown == true)
			return null;

		// acquire data and send out
		reading = null;
		reading = new StringBuffer(sensor);

		getEntry();

		if (reading != null)
			return reading.toString().getBytes();
		else
			return null;
	}

	/**
	 * Method to share the last value of the given sensor - here doing nothing
	 * 
	 * @param sensor
	 *            String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.myStress.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor) {
		return null;
	}

	/**
	 * Method to view historical chart of the given sensor symbol - here doing
	 * nothing
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
	 * {@link com.myStress.platform.SensorRepository}, if there is at least one
	 * calendar selected by the user
	 * 
	 * @see com.myStress.handlers.Handler#Discover()
	 * @see com.myStress.platform.Sensor
	 * @see com.myStress.platform.SensorRepository
	 */
	public void Discover() {
		if (no_calendars == true)
			SensorRepository.insertSensor(new String("CA"), new String("-"),
					myStress.getString(R.string.CA_d),
					myStress.getString(R.string.CA_e), new String("str"), 0, 0, 1,
					false, polltime, this);
	}

	/**
	 * Constructor, allocating all necessary resources for the handler Here,
	 * reading the various RMS values of the preferences Then, we see if there
	 * are at least one calendar selected
	 * 
	 * @param nors
	 *            Reference to the calling {@link android.content.Context}
	 */
	public CalendarHandler(Context nors) {
		String storedCalendars;
		// store for later
		myStress = nors;

		// retrieve clendars
		// SharedPreferences settings =
		// PreferenceManager.getDefaultSharedPreferences(nors);
		// storedCalendars =
		// settings.getString("CalendarHandler::Calendar_names", null);
		//
		// if (storedCalendars == null)
		// no_calendars = false;
		// else
		// {
		// // retrieve individual calendars now
		// calendars = storedCalendars.split("::");
		// no_calendars = true;
		// }

		calendars = getAllCalendar();

		if (calendars != null)
			no_calendars = true;
		else
			no_calendars = false;

		// now read polltime for audio sampling
		polltime = HandlerManager.readRMS_i("CalendarHandler::samplingpoll",
				60 * 6) * 1000;

	}

	/**
	 * Method to release all handler resources - here doing nothing
	 * 
	 * @see com.myStress.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler() {
		// we are shutting down
		shutdown = true;
	}

	private void getEntry() {
		boolean first = false, calendar = false;
		String title, location, ID;
		long begin, end;
		String[] fields = { Instances.TITLE, Instances.BEGIN, Instances.END,
				Instances.EVENT_LOCATION, Instances.CALENDAR_ID };
		long now = System.currentTimeMillis();
		int i, j, calendar_no = calendars.length;
		Cursor eventCursor;

		currentRead = now - polltime;

		// query anything between now-polltime and now
		if (Build.VERSION.SDK_INT >= 14)
			eventCursor = CalendarContract.Instances.query(
					myStress.getContentResolver(), fields, currentRead, now + 24
							* 60 * 60 * 1000);
		else {
			Uri.Builder builder = Uri.parse(
					"content://com.android.calendar/instances/when")
					.buildUpon();
			ContentUris.appendId(builder, currentRead);
			ContentUris.appendId(builder, now + 24 * 60 * 60 * 1000);
			eventCursor = myStress.getContentResolver().query(
					builder.build(),
					new String[] { "title", "begin", "end", "eventLocation",
							"calendar_id" }, null, null, null);
		}
		// walk through all returns
		eventCursor.moveToFirst();
		for (i = 0; i < eventCursor.getCount(); i++) {
			title = eventCursor.getString(0);
			begin = eventCursor.getLong(1);
			end = eventCursor.getLong(2);
			location = eventCursor.getString(3);
			ID = eventCursor.getString(4);

			calendar = false;
			// see if the entry is from the calendars I want
			for (j = 0; j < calendar_no; j++)
				if (calendars[j].compareTo(ID) == 0) {
					calendar = true;
					break;
				}

			if (calendar == true)
				// is this something new?
				if (begin > currentRead && begin < now) {
					if (first == false) {
						reading.append(title + ":" + location + ":"
								+ String.valueOf(begin) + ":"
								+ String.valueOf(end));
						first = true;
					} else
						reading.append("\n" + title + ":" + location + ":"
								+ String.valueOf(begin) + ":"
								+ String.valueOf(end));
				}

			eventCursor.moveToNext();
		}

		// free data
		eventCursor.close();

		// have we read anything? -> if not, delete reading string
		if (first == false)
			reading = null;
	}

	/**
	 * Function to configure the Preference activity with any preset value
	 * necessary - here we populate the ListPreference for the calendars with
	 * all calendars available on this device
	 * 
	 * @param prefs
	 *            Reference to {@link android.preference.PreferenceActivity}
	 */
	private String[] getAllCalendar() {
		int i;
		String[] fields = { CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
				CalendarContract.Calendars._ID };
		Cursor eventCursor;
		ArrayList<String> allCalendar = new ArrayList<String>();

		// use different way of accessing calendar for Honeycomb
		if (Build.VERSION.SDK_INT >= 14)
			eventCursor = myStress.getContentResolver().query(
					CalendarContract.Calendars.CONTENT_URI, fields, null, null,
					null);
		else
			eventCursor = myStress.getContentResolver().query(
					Uri.parse("content://com.android.calendar/calendars"),
					(new String[] { "displayName", "_id" }), null, null, null);

		if (eventCursor.getCount() == 0)
			return null;

		eventCursor.moveToFirst();

		// collect all calendars
		for (i = 0; i < eventCursor.getCount(); i++) {
			allCalendar.add(eventCursor.getString(1));
			eventCursor.moveToNext();
		}

		return allCalendar.toArray(new String[allCalendar.size()]);
	}
}
