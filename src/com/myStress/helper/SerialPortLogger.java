package com.myStress.helper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import android.app.Activity;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

/**
 * Class to handle debug messages to the file system and the ADB
 *
 */
public class SerialPortLogger 
{
	private static boolean isDebugging = true;
	public static Activity nors;
	private static File fconn = null;
	private static BufferedOutputStream os = null;

	/**
	 * Write message to the file system
	 * @param msg String to be written
	 */
	public static synchronized void write(String msg) 
	{
		if (os==null)
		{
		    try 
		    {
		    	// open file and create, if necessary
				fconn = new File(Environment.getExternalStorageDirectory(), "myStress_log.txt");
		    	os = new BufferedOutputStream(new FileOutputStream(fconn, true));
		    }
		    catch(Exception e)
		    {
		    }
		}

		try
		{
			if (os!=null)
			{
				os.write(msg.getBytes(), 0, msg.length());
				os.flush();
			}
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * Forces debug output to the file system, even if debugging is switched off	  
	 */	
	public static synchronized void debugForced(String msg) 
	{
		Calendar cal = Calendar.getInstance();
		
		write(DateFormat.format("MMM dd h:m:s", cal.getTimeInMillis()) + ": " + msg + "\n");
		Log.w("myStress:", msg);
	}

	/**
	 * Debugs only if the debugging flag is switched on
	 * @param msg String to be written to the filesystem
	 */
	public static synchronized void debug(String msg) 
	{
		if (isDebugging)
			write(msg + "\n");
		Log.v("myStress:", msg);
	}

	/**
	 * Checks if debugging is on (true) or off (false)
	 * @return is Debugging
	 */
	public static boolean isDebugging() 
	{
		return isDebugging;
	}

	/**
	 * Set the debug mode
	 * @param b debug (true) or not (false)
	 */
	public static void setDebugging(boolean b) 
	{
		isDebugging = b;
	}

}
