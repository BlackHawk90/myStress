/*
Copyright (C) 2012, Dirk Trossen, airs@dirk-trossen.de

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
