package com.myStress.helper;

import android.content.Context;

/**
 * Class to implement the sleep function used throughout the platform, right now a simple {@link Thread} sleep()
 *
 */
public class Waker
{
	/**
	 * Initialise the sleep function
	 * @param context Reference to the calling {@link android.content.Context}
	 */
	static public void init(Context context)
	{
	}
	
	/**
	 * Sleep function
	 * @param milli time to sleep in milliseconds
	 */
	static public void sleep(long milli)
	{
		try
		{
			Thread.sleep(milli);
		}
		catch(Exception e)
		{
		}
	}
}
