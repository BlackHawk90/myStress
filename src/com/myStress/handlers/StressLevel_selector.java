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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.myStress.*;

/**
 * Activity to self-annotate moods
 * 
 * @see android.app.Activity
 */
public class StressLevel_selector extends Activity implements OnClickListener,
		OnSeekBarChangeListener {
	// private TextView mTitle;
	// private TextView mTitle2;
	// private Editor editor;
	
	// Singleton
//	private static StressLevel_selector instance = null;

	// preferences
//	private SharedPreferences settings;
	private String stress = null, status = null;

	private boolean bSlider1Moved = false;
	private boolean bSlider2Moved = false;
	private boolean bSlider3Moved = false;
	private boolean bSlider4Moved = false;

	private ArrayList<SeekBar> allSeekBars = new ArrayList<SeekBar>();
	private SeekBar seekBar1;
	private SeekBar seekBar2;
	private SeekBar seekBar3;
	private SeekBar seekBar4;
	
	private int[] index = new int[4];

	/**
	 * Started when creating the {@link android.app.Activity}
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set up the window layout
		super.onCreate(savedInstanceState);

		// read preferences
//		settings = PreferenceManager.getDefaultSharedPreferences(this);
		// editor = settings.edit();

		// read last selected mood value
//		try {
//			stress = settings.getString("StressLevelHandler::Mood", "Happy");
//		} catch (Exception e) {
//		}

		// set window title
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		setContentView(R.layout.stress_selection);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// get window title fields
		// mTitle = (TextView) findViewById(R.id.title_left_text);
		// mTitle2 = (TextView) findViewById(R.id.title_right_text);
		/*
		 * mTitle.setText(getString(R.string.AIRS_Mood_Selector));
		 * mTitle2.setText(getString(R.string.AIRS_mood_selection2) + " " +
		 * mood);
		 */

		// define listeners
		Button bt = (Button) findViewById(R.id.notnow);
		bt.setOnClickListener(this);
		bt = (Button) findViewById(R.id.skip);
		bt.setOnClickListener(this);
		bt = (Button) findViewById(R.id.ok);
		bt.setOnClickListener(this);

		// define SeekBar-listeners
		seekBar1 = (SeekBar) findViewById(R.id.slider1);
		seekBar1.setOnSeekBarChangeListener(this);
		allSeekBars.add(seekBar1);
		
		seekBar2 = (SeekBar) findViewById(R.id.slider2);
		seekBar2.setOnSeekBarChangeListener(this);
		allSeekBars.add(seekBar2);
		
		seekBar3 = (SeekBar) findViewById(R.id.slider3);
		seekBar3.setOnSeekBarChangeListener(this);
		allSeekBars.add(seekBar3);
		
		seekBar4 = (SeekBar) findViewById(R.id.slider4);
		seekBar4.setOnSeekBarChangeListener(this);
		allSeekBars.add(seekBar4);

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
		
		status = null;
	}

	@SuppressLint("NewApi")
	private int[] shuffle(String[] questions) {
		Random rnd = new Random();
		String[] origin = new String[4];
		
		origin = Arrays.copyOf(questions, 4);
		
		for (int i = 0; i < questions.length; i++) {
			int randomPosition = rnd.nextInt(questions.length);
			String temp = questions[i];
			questions[i] = questions[randomPosition];
			questions[randomPosition] = temp;
		}
		
		for(int i = 0; i < origin.length; i++){
			for(int j = 0; j < questions.length; j++){
				if(origin[i].equalsIgnoreCase(questions[j])){
					index[i] = j;
					break;
				}
			}
		}
		
		return index;
	}

	/**
	 * Called when restarting the {@link android.app.Activty}
	 * 
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	public synchronized void onRestart() {
		super.onRestart();
	}

	/**
	 * Called when stopping the {@link android.app.Activty}
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();
	}

	/**
	 * Called when destroying the {@link android.app.Activty} Here, we save the
	 * selections and send a broadcast to the
	 * {@link com.airs.handlers.MoodButtonHandler}
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		Intent intent;
		if(status == null){
			status = "snooze";
			finish();
		}
		
		if(status.equals("polled")){
			stress = "";
			int[] values = {seekBar1.getProgress(),  seekBar2.getProgress(),  seekBar3.getProgress() , seekBar4.getProgress()};
			double tmp;
			for(int i = 0; i < 4; i++){
				tmp = values[index[i]] / 2.0; // change for different stresslevel-scale
				if(i == 3)
					stress += tmp;
				else
					stress += tmp + ":";
			}
			
			intent = new Intent("com.myStress.stresslevel");
			intent.putExtra("StressLevel", stress);
			intent.putExtra("StressMeta", status);
		}
		else{
			intent = new Intent("com.myStress.stresslevel");
			intent.putExtra("StressMeta", status);
		}
		
		// send broadcast intent to signal end of selection to mood button
		// handler
		
		sendBroadcast(intent);
		
		stress = "";
		status = null;
		// now destroy activity
		super.onDestroy();
	}

	/**
	 * Called when button, here the own mood definition field, the delete button
	 * or the enter button, has been clicked
	 * 
	 * @param v
	 *            Reference of the {android.view.View} being clicked
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		// EditText et;
		// dispatch depending on button pressed
		switch (v.getId()) {
		case R.id.notnow:
			try {
				bSlider1Moved = false;
				bSlider2Moved = false;
				bSlider3Moved = false;
				bSlider4Moved = false;
				
				status="snooze";
				
				finish();
				break;
			} catch (Exception e) {
				Log.e("myStress", e.getMessage());
			}
		case R.id.skip:
			bSlider1Moved = false;
			bSlider2Moved = false;
			bSlider3Moved = false;
			bSlider4Moved = false;
			
			status = "skip";
			
			finish();
			break;
		case R.id.ok:
			if ((bSlider1Moved && bSlider2Moved) || (bSlider1Moved && bSlider3Moved) ||
					(bSlider1Moved && bSlider4Moved) || (bSlider2Moved && bSlider3Moved) ||
					(bSlider2Moved && bSlider4Moved) || (bSlider3Moved && bSlider4Moved)) {
				bSlider1Moved = false;
				bSlider2Moved = false;
				bSlider3Moved = false;
				bSlider4Moved = false;
				
				status="polled";
				
				finish();
			}
			break;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {

		switch (seekBar.getId()) {
		case R.id.slider1:
			bSlider1Moved = true;
			break;
		case R.id.slider2:
			bSlider2Moved = true;
			break;
		case R.id.slider3:
			bSlider3Moved = true;
			break;
		case R.id.slider4:
			bSlider4Moved = true;
			break;
		}

//		if (bSlider1Moved && bSlider2Moved && bSlider3Moved && bSlider4Moved) {
//			bSlider1Moved = false;
//			bSlider2Moved = false;
//			bSlider3Moved = false;
//			bSlider4Moved = false;
//			
//			status="polled";
//			
//			finish();
//		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}
}