package com.myStress.platform;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.myStress.visualisations.MapViewerActivity;
import com.myStress.visualisations.TimelineActivity;

/**
 * Static class to call the {@link TimelineActivity} and {@link MapViewerActivity} for visualisation
 *
 */
public class History 
{ 
	/**
	 * Starts the {@link TimelineActivity}
	 * @param myStress Reference to the {@link android.content.Context} of the calling activity
	 * @param title String with title of the dialog box being created
	 * @param Symbol String with the sensor symbol being visualised
	 */
    static public void timelineView(Context myStress, String title, String Symbol)
    {
    	Intent intent = new Intent(myStress.getApplicationContext(), TimelineActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    	// create bundle
    	Bundle bundle = new Bundle();

    	bundle.putString("com.myStress.Title", title);
    	
    	bundle.putString("com.myStress.Symbol", Symbol);  
    	
        intent.putExtras(bundle);

 	   	myStress.startActivity(intent);
    }

    /**
	 * Starts the {@link MapViewerActivity}
	 * @param myStress Reference to the {@link android.content.Context} of the calling activity
	 * @param title String with title of the dialog box being created
	 * @param Symbol String with the sensor symbol being visualised
     */
    static public void mapView(Context myStress, String title, String Symbol)
    {
    	Intent intent = new Intent(myStress.getApplicationContext(), MapViewerActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    	// create bundle
    	Bundle bundle = new Bundle();

    	// place title
    	bundle.putString("com.myStress.Title", title);
    	
    	bundle.putString("com.myStress.Symbol", Symbol);  

        intent.putExtras(bundle);

    	myStress.startActivity(intent);    	
    }
}
