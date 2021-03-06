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

package com.myStress.visualisations;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

/**
 * Class to implement the overlay of GI/VI sensors in the Map View
 *
 */
@SuppressWarnings("rawtypes")
public class MapViewerOverlay extends ItemizedOverlay 
{
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context myStress;
	
	/**
	 * Constructor
	 * @param arg0 {@link android.graphics.drawable.Drawable} of the Overlay item
	 * @param myStress {@link android.content.Context} of the calling {@link android.app.Activity}
	 */
	public MapViewerOverlay(Drawable arg0, Context myStress) 
	{
		super(boundCenterBottom(arg0));
		this.myStress = myStress;
	}

	/**
	 * Add an item to the overall overlay list
	 * @param overlay {@link com.google.android.maps.OverlayItem} to be added
	 * @param marker {@link android.graphics.drawable.Drawable} of this item
	 */
	public void addOverlay(OverlayItem overlay, Drawable marker)
	{
		// set marker for this item
		overlay.setMarker(boundCenterBottom(marker));
	    mOverlays.add(overlay);
	    populate();
	}

	/**
	 * Add an item with default marker to the overall overlay list
	 * @param overlay {@link com.google.android.maps.OverlayItem} to be added
	 */
	public void addOverlay(OverlayItem overlay) 
	{
	    mOverlays.add(overlay);
	    populate();
	}
	
	@Override
	protected  boolean	onTap(int index) 
	{
		OverlayItem item = mOverlays.get(index);
		
       	Toast.makeText(myStress, "Measured at : " + item.getTitle() + "\n" + item.getSnippet(), Toast.LENGTH_LONG).show();   

		return true;
	}
	
	@Override
	protected OverlayItem createItem(int i) 
	{
	  return mOverlays.get(i);
	}

	/**
	 * Returns number of the overlay items
	 * @return number of items
	 */
	@Override
	public int size() 
	{
		return mOverlays.size();
	}

	/**
	 * Draws the overlay items on the Map View
	 * @param canvas Reference to the {@link android.graphics.Canvas} being drawn on
	 * @param mapView Reference to the {@link com.google.android.maps.MapView} that this overlay appears on
	 * @param shadow draw shadow (true) or not (false) under the overlay item
	 */
	@Override	
    public void draw(Canvas canvas, MapView mapView, boolean shadow)
    {
        if(!shadow)
        {
            super.draw(canvas, mapView, false);
        }
    }
}
