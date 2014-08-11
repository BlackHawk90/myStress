/*
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de

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

import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.myStress.*;

/** Activity to self-annotate moods
 * @see android.app.Activity
 */
public class StressLevel_selector extends Activity implements OnItemClickListener, OnClickListener
{
//	 private TextView mTitle;
//	 private TextView mTitle2;
//	 private Editor editor;

	 // preferences
	 private SharedPreferences settings;
	 private String stress = null;
	 private boolean selected = false;
	 
	 /**
	  * Started when creating the {@link android.app.Activity}
	  * @see android.app.Activity#onCreate(android.os.Bundle)
	  */
	 @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        
	        // read preferences
	        settings = PreferenceManager.getDefaultSharedPreferences(this);
//	        editor = settings.edit();
	        
	        // read last selected mood value
			try
			{
				stress	= settings.getString("StressLevelHandler::Mood", "Happy");
			}
			catch(Exception e)
			{
			}

			// set window title
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			setContentView(R.layout.stress_selection);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get window title fields
//	        mTitle = (TextView) findViewById(R.id.title_left_text);
//	        mTitle2 = (TextView) findViewById(R.id.title_right_text);
/*	        mTitle.setText(getString(R.string.AIRS_Mood_Selector));
	        mTitle2.setText(getString(R.string.AIRS_mood_selection2) + " " + mood);*/
	        
	        // define listeners
	        Button bt = (Button) findViewById(R.id.notnow);
	        bt.setOnClickListener(this);
	        bt = (Button) findViewById(R.id.skip);
	        bt.setOnClickListener(this);
	        
	        String[] questions = getResources().getStringArray(R.array.questions);
	        
	        shuffle(questions);
	        
	        TextView tv = (TextView) findViewById(R.id.q1);
	        tv.setText(questions[0]);
	        
	        tv = (TextView) findViewById(R.id.q2);
	        tv.setText(questions[1]);
	        
	        tv = (TextView) findViewById(R.id.q3);
	        tv.setText(questions[2]);
	        
	        tv = (TextView) findViewById(R.id.q4);
	        tv.setText(questions[3]);
	     }

	 private int[] shuffle(String[] ar) {
		 Random rnd = new Random();
		 int[] index = new int[4];
		 for(int i=ar.length-1;i>0;i--){
			 index[i] = rnd.nextInt(i+1);
			 String a = ar[index[i]];
			 ar[index[i]] = ar[i];
			 ar[i] = a;
		 }
		 return index;
	 }

	 /** Called when restarting the {@link android.app.Activty}
	    * @see android.app.Activity#onRestart()
	    */
	 @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    }

	    
	 /** Called when stopping the {@link android.app.Activty}
	    * @see android.app.Activity#onStop()
	    */
	 @Override
	    public void onStop() 
	    {
	        super.onStop();
	    }

	 /** Called when destroying the {@link android.app.Activty}
	  * Here, we save the selections and send a broadcast to the {@link com.airs.handlers.MoodButtonHandler}
	    * @see android.app.Activity#onDestroy()
	    */
	 @Override
	    public void onDestroy() 
	    {
	    	if (selected == true)
	    	{	
				// send broadcast intent to signal end of selection to mood button handler
				Intent intent = new Intent("com.myStress.stresslevel");
				intent.putExtra("StressLevel", stress);
				
				sendBroadcast(intent);
				
				// clear flag
				selected = false;
	    	}
			
			// now destroy activity
	        super.onDestroy();
	    }

	 	/**
	     * Called when returning from another activity - here we pick up the selection of the mood icon being chosen when defining one's own mood definition
	     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	     */
	 	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	    {
    	    super.onActivityResult(requestCode, resultCode, data); 

	    	// pick up selection
	    	if (resultCode == RESULT_OK)
	    	{
//	    		mood_icon = data.getStringExtra("mood");
//	    		int resid = data.getIntExtra("resid", R.drawable.mood_own);
//	    		mood_iconown.setImageResource(resid);
	    	}
	    }
	    
	 	/**
	 	 * Called when the Options menu botton is pressed, inflating the menu to be shown
	 	 * @param menu Reference of the {@link android.view.Menu} to be shown
	 	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 	 */
	 	@Override
	    public boolean onPrepareOptionsMenu(Menu menu) 
	    {
	    	MenuInflater inflater;

	    	menu.clear();    		
	    	inflater = getMenuInflater();
	    	inflater.inflate(R.menu.options_about, menu);    		
	    	return true;
	    }

	 	/**
		    * Called when a menu item in the Options menu has been selected
		    * @param item Reference to the {@link android.view.MenuItem}
		    * @return true, if item selection was consumed
		    * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
		    */
	 	@Override
	    public boolean onOptionsItemSelected(MenuItem item) 
	    {
	        switch (item.getItemId()) 
	        {
	        case R.id.main_about:
//	        		Toast.makeText(getApplicationContext(), R.string.MoodAbout, Toast.LENGTH_LONG).show();
	            return true;
	        }
	        return false;
	    }


	 	/**
	     * Called if a list item has been clicked on, here any of the mood annotations
	     * @param av Reference to the parent view
	     * @param v Reference to the {@link android.view.View} being clicked on
	     * @param arg2 don't care
	     * @param arg3 index of the list items being clicked on
	     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
	     */
	 	public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
	    {
	    	// get list view entry
//	    	HandlerEntry entry = (HandlerEntry)av.getItemAtPosition((int)arg3);
	    	
	    	// read entries name for the selected mood
//	    	stress = new String(entry.name);
//	    	selected = true;
//	    	finish();
	    }
	    
	 	/**
	     * Called when button, here the own mood definition field, the delete button or the enter button, has been clicked
	     * @param v Reference of the {android.view.View} being clicked
	     * @see android.view.View.OnClickListener#onClick(android.view.View)
	     */
	 	public void onClick(View v) 
		{
//	    	EditText et;
	    	// dispatch depending on button pressed
	    	switch(v.getId())
	    	{
	    	case R.id.notnow:
	    		try{
	    			Thread.sleep(100);
	    		} catch(Exception e){
	    			Log.e("myStress", e.getMessage());
	    		}
	    	case R.id.skip:
	    		
	    		
/*	    	case R.id.mooddefined:
	    		et = (EditText) findViewById(R.id.moodown);
		    	// read input string from edit field
	    		mood = et.getText().toString();
	    		mood = mood.replaceAll("'","''");
	    		mood = mood.replaceAll(":","");
	    		mood = mood.replaceAll("::","");

	    		selected = true;
	    		own_defined = true;
	    		finish();
	    		break;
	    	case R.id.mooddelete:
	    		et = (EditText) findViewById(R.id.moodown);
	    		et.setText("");
	    		break;
	    	case R.id.moodown_icon:
	    		Intent intent = new Intent(getApplicationContext(), MoodButton_iconselector.class);
		    	startActivityForResult(intent, 101);	        	
		    	break;*/
	    	}
		}
}