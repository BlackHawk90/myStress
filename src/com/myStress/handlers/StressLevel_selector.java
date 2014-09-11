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

import java.util.Arrays;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.myStress.*;

/**
 * Activity to self-annotate moods
 * 
 * @see android.app.Activity
 */
public class StressLevel_selector extends Activity implements OnClickListener,
		OnCheckedChangeListener {
	// private TextView mTitle;
	// private TextView mTitle2;
	// private Editor editor;

	// Singleton
	// private static StressLevel_selector instance = null;

	// preferences
	// private SharedPreferences settings;
	private String stress = null, status = null;

	private boolean bGroup1Selected = false;
	private boolean bGroup2Selected = false;
	private boolean bGroup3Selected = false;
	private boolean bGroup4Selected = false;

	private RadioGroup rgOpinion1;
	private RadioGroup rgOpinion2;
	private RadioGroup rgOpinion3;
	private RadioGroup rgOpinion4;

	private SharedPreferences settings;
	private Editor editor;
	private int counter;

	private int[] index = new int[4];
	private int value1;
	private int value2;
	private int value3;
	private int value4;

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
		// settings = PreferenceManager.getDefaultSharedPreferences(this);
		// editor = settings.edit();

		// read last selected mood value
		// try {
		// stress = settings.getString("StressLevelHandler::Mood", "Happy");
		// } catch (Exception e) {
		// }

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

		// define ButtonGroups
		rgOpinion1 = (RadioGroup) findViewById(R.id.radioGroup1);
		rgOpinion1.setOnCheckedChangeListener(this);

		rgOpinion2 = (RadioGroup) findViewById(R.id.radioGroup2);
		rgOpinion2.setOnCheckedChangeListener(this);

		rgOpinion3 = (RadioGroup) findViewById(R.id.radioGroup3);
		rgOpinion3.setOnCheckedChangeListener(this);

		rgOpinion4 = (RadioGroup) findViewById(R.id.radioGroup4);
		rgOpinion4.setOnCheckedChangeListener(this);

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

		// get default preferences and editor
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		editor = settings.edit();

		counter = settings.getInt("StressCounter", 0);

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

		for (int i = 0; i < origin.length; i++) {
			for (int j = 0; j < questions.length; j++) {
				if (origin[i].equalsIgnoreCase(questions[j])) {
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
		if (status == null) {
			status = "exit";
			//finish();
		}

		if (status.equals("polled")) {
			stress = "";

			int[] values = { value1, value2, value3, value4 };
			double tmp;
			for (int i = 0; i < 4; i++) {
				tmp = values[index[i]];
				if (i == 3)
					stress += tmp;
				else
					stress += tmp + ":";
			}

			intent = new Intent("com.myStress.stresslevel");
			intent.putExtra("StressLevel", stress);
			intent.putExtra("StressMeta", status);

			editor.putInt("StressCounter", counter + 1);
			editor.commit();
		} else {
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
			bGroup1Selected = false;
			bGroup2Selected = false;
			bGroup3Selected = false;
			bGroup4Selected = false;

			status = "snooze";
			break;
		case R.id.skip:
			bGroup1Selected = false;
			bGroup2Selected = false;
			bGroup3Selected = false;
			bGroup4Selected = false;

			status = "skip";
			break;
		case R.id.ok:
			if (bGroup1Selected && bGroup2Selected && bGroup3Selected
					&& bGroup4Selected) {
				bGroup1Selected = false;
				bGroup2Selected = false;
				bGroup3Selected = false;
				bGroup4Selected = false;

				status = "polled";
			}
			else return;
			break;
		}
		finish();
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		switch (group.getId()) {
		case R.id.radioGroup1:
			bGroup1Selected = true;
			switch (checkedId) {
			case R.id.radio10:
				value1 = 0;
				break;
			case R.id.radio11:
				value1 = 1;
				break;
			case R.id.radio12:
				value1 = 2;
				break;
			case R.id.radio13:
				value1 = 3;
				break;
			case R.id.radio14:
				value1 = 4;
				break;
			}
			break;
		case R.id.radioGroup2:
			bGroup2Selected = true;
			switch (checkedId) {
			case R.id.radio20:
				value2 = 0;
				break;
			case R.id.radio21:
				value2 = 1;
				break;
			case R.id.radio22:
				value2 = 2;
				break;
			case R.id.radio23:
				value2 = 3;
				break;
			case R.id.radio24:
				value2 = 4;
				break;
			}
			break;
		case R.id.radioGroup3:
			bGroup3Selected = true;
			switch (checkedId) {
			case R.id.radio30:
				value3 = 0;
				break;
			case R.id.radio31:
				value3 = 1;
				break;
			case R.id.radio32:
				value3 = 2;
				break;
			case R.id.radio33:
				value3 = 3;
				break;
			case R.id.radio34:
				value3 = 4;
				break;
			}
			break;
		case R.id.radioGroup4:
			bGroup4Selected = true;
			switch (checkedId) {
			case R.id.radio40:
				value4 = 0;
				break;
			case R.id.radio41:
				value4 = 1;
				break;
			case R.id.radio42:
				value4 = 2;
				break;
			case R.id.radio43:
				value4 = 3;
				break;
			case R.id.radio44:
				value4 = 4;
				break;
			}
			break;
		}

	}
}