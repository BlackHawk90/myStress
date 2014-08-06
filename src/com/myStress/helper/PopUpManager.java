package com.myStress.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import com.myStress.R;

/**
 * Initializes the handler UIs and stores them in static variable
 * later being used to point to in the recording services
 */
public class PopUpManager 
{
    
	/**
	 * Show About Dialog for other UIs
	 * @param title Window title
	 * @param text Text shown in the dialog box
	 */
	static public void AboutDialog(String title, String text, Context myStress)
	{				
		// Linkify the message
	    final SpannableString s = new SpannableString(text);
	    Linkify.addLinks(s, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

		AlertDialog.Builder builder = new AlertDialog.Builder(myStress);
		builder.setTitle(title)
			   .setMessage(s)
			   .setIcon(R.drawable.about)
		       .setNeutralButton(myStress.getString(R.string.OK), new DialogInterface.OnClickListener() 
		       {
		           public void onClick(DialogInterface dialog, int id) 
		           {
		                dialog.dismiss();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();

		// Make the textview clickable. Must be called after show()
	    ((TextView)alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	}
}