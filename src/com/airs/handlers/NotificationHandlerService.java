/*
Copyright (C) 2013, TecVis LP, support@tecvis.co.uk

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
package com.airs.handlers;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.airs.R;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.alchemyapi.api.AlchemyAPI;
import com.alchemyapi.api.AlchemyAPI_NamedEntityParams;

/**
 * Class to implement the AIRS accessibility service
 */
public class NotificationHandlerService extends AccessibilityService
{	
	boolean started = true;
	boolean typing = false;
	boolean newText = false;
	double typingStartTime, typingEndTime;
	int typedChars;
	int maxTextLength;
	String typedText;
		
	String alchemyApiKey = "d8c9013818eb2aeb7baa7ad558f7487db4ded10c";
	AlchemyAPI api = AlchemyAPI.GetInstanceFromString(alchemyApiKey);
	
	/**
	 * Called when an accessibility event occurs - here, we check the particular component packages that fired the event, filtering out the ones we support
	 * @param event Reference to the fired {android.view.accessibility.AccessibilityEvent}
	 * @see android.accessibilityservice.AccessibilityService#onAccessibilityEvent(android.view.accessibility.AccessibilityEvent)
	 */
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) 
	{
		int eventType = event.getEventType();
		
    	Log.e("AIRS", "notification: " + event.getPackageName().toString() + ", " + eventType + ", " + event.getClassName());
	    if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) 
	    {
	    	// get notification shown
	    	Notification notification = (Notification)event.getParcelableData();
	    	
	    	if (notification != null)
	    	{
		    	// now parse the specific packages we support
		    	// start with GTalk
		    	if (event.getPackageName().toString().compareTo("com.google.android.talk") == 0)
		    	{
			        // now broadcast the capturing of the accessibility service to the handler
					Intent intent = new Intent("com.airs.accessibility");
					intent.putExtra("NotifyText", "gtalk");//::" + notification.tickerText);
					sendBroadcast(intent);
		    	}
		    	
		    	// anything from Skype?
		    	if (event.getPackageName().toString().compareTo("com.skype.raider") == 0)
		    	{
			        // now broadcast the capturing of the accessibility service to the handler
					Intent intent = new Intent("com.airs.accessibility");
					intent.putExtra("NotifyText", "skype::Message from " + notification.tickerText);		
					sendBroadcast(intent);
		    	}
		    	// anything from Spotify?
		    	if (event.getPackageName().toString().compareTo("com.spotify.music") == 0)
		    	{
			        // now broadcast the capturing of the accessibility service to the handler	    		
		    		// anything delivered?
		    		if (notification.tickerText != null)
		    		{
		    			// split information in tokens
		    			String tokens[] = notification.tickerText.toString().split(getString(R.string.accessibility_spotify));
		    			    			
		    			// try other '-', if previous one did not work
		    			if (tokens.length != 2)
		    				tokens = notification.tickerText.toString().split("-");
		    			
		    			if (tokens.length == 2)
		    			{
			    			// signal as play state changed event
							Intent intent = new Intent("com.android.music.playstatechanged");
							
							intent.putExtra("track", tokens[0].trim());		
							intent.putExtra("artist", tokens[1].trim());							
							intent.putExtra("album", "");		
							sendBroadcast(intent);
						}
		    			else
		    				Log.e("AIRS", "Can't find token in '" + notification.tickerText +"'");
		    		}				
		    	}
		    	if(event.getPackageName().toString().compareTo("com.whatsapp") == 0){
		    		//Log.e("AIRS", "whatsapp message");
			        // now broadcast the capturing of the accessibility service to the handler
					Intent intent = new Intent("com.airs.accessibility");
					intent.putExtra("NotifyText", "whatsapp::" + getText(notification));
					sendBroadcast(intent);	
		    	}
		    	if(event.getPackageName().toString().compareTo("com.facebook.orca") == 0){
		    		// now broadcast the capturing of the accessibility service to the handler
					Intent intent = new Intent("com.airs.accessibility");
					intent.putExtra("NotifyText", "fbm::" + getText(notification));
					sendBroadcast(intent);
		    	}
		    	if(event.getPackageName().toString().compareTo("com.facebook.katana") == 0){
		    		// now broadcast the capturing of the accessibility service to the handler
					Intent intent = new Intent("com.airs.accessibility");
					intent.putExtra("NotifyText", "fb::" + getText(notification));
					sendBroadcast(intent);
		    	}
	    	}
	    }
	    else if(eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED){
//	    	Log.e("AIRS", event.getBeforeText() + ", " + event.getText().toString());
	    	if(event.isPassword()) return;
	    	
//	    	String diff="";
	    	String text = event.getText().toString();
	    	String beforeText = event.getBeforeText().toString();
	    	text = text.substring(1, text.length()-1);
	    	boolean del;
	    	int length_diff = text.length()-beforeText.length();
	    	typedText = text;
	    	
	    	if(text.length() > maxTextLength) maxTextLength = text.length();
	    	
	    	if(typing == false){
	    		if(length_diff == 1){
	    			typing = true;
	    			typingStartTime = (double)System.currentTimeMillis();
	    			typingEndTime = typingStartTime;
	    			typedChars = 1;
	    		}
	    	}
	    	else{
	    		typingEndTime = (double)System.currentTimeMillis();
		    	typedChars++;
	    	}
	    	
	    	if(text.length() < beforeText.length()){
	    		del = true;
	    		length_diff = -length_diff;
	    		
//	    		int i=0;
//	    		while(i<text.length()){
//	    			if(text.charAt(i)!=beforeText.charAt(i)) break;
//	    			i++;
//	    		}
//	    		int start = i;
//	    		i=0;
//	    		while(i<length_diff){
//	    			diff = diff + beforeText.charAt(start+i);
//	    			i++;
//	    		}
	    	}
	    	else{
	    		del = false;
	    		
//	    		int i=0;
//	    		while(i<beforeText.length()){
//	    			if(text.charAt(i)!=beforeText.charAt(i)) break;
//	    			i++;
//	    		}
//	    		int start = i;
//	    		i=0;
//	    		while(i<length_diff){
//	    			diff = diff + text.charAt(start+i);
//	    			i++;
//	    		}
	    	}

	    	
	    	if(del == true){
	    		Log.e("AIRS", "text deleted");//: " + diff);
				Intent intent = new Intent("com.airs.accessibility");
				intent.putExtra("KeyLogger", length_diff + " characters deleted");
				sendBroadcast(intent);
	    	}
	    	else{
//	    		Log.e("AIRS", "text inserted: " + diff);
//				Intent intent = new Intent("com.airs.accessibility");
//				intent.putExtra("KeyLogger", "text inserted: " + );		
//				sendBroadcast(intent);
	    	}
	    }
	    else if(eventType == AccessibilityEvent.TYPE_VIEW_CLICKED || eventType == AccessibilityEvent.TYPE_VIEW_SELECTED){
	    	if(typing == true){
	    		if(typingEndTime != typingStartTime){
		    		double typingDuration = (typingEndTime - typingStartTime)/1000.0d;
		    		double typingSpeed = typedChars / typingDuration;
		    		Log.e("AIRS", "Tippgeschwindigkeit: "+typingSpeed + " Zeichen pro Sekunde");
		    		Intent intent = new Intent("com.airs.accessibility");
		    		intent.putExtra("TypingSpeed", ((Double)typingSpeed).toString());
		    		sendBroadcast(intent);
	    		}
	    		typing = false;
	    	}
	    	
	    	if(maxTextLength > 0){
	    		Log.e("AIRS", "Textlänge: "+maxTextLength);
	    		Intent intent = new Intent("com.airs.accessibility");
	    		intent.putExtra("TextLength", ((Integer)maxTextLength).toString());
	    		
	    		try{
		    		Log.e("AIRS", "Text: "+typedText);
		    		//übersetzen
	    			String translatedText = Translate.execute(typedText, Language.GERMAN, Language.ENGLISH);
	    			Log.e("AIRS", "translated: "+translatedText);
//		    		// Sentimentanalyse
//		    		AlchemyAPI_NamedEntityParams nep = new AlchemyAPI_NamedEntityParams();
//		    		nep.setSentiment(true);
//		    		Document doc = null;
//	    			doc = api.URLGetRankedNamedEntities(translatedText, nep);
//		    		Element el = doc.getDocumentElement();
//		    		NodeList items = el.getElementsByTagName("text");
//		    		NodeList sentiments = el.getElementsByTagName("sentiment");
	    		}catch(Exception e){
	    			Log.e("AIRS", e.getMessage());
	    		}
	    		
	    		maxTextLength = 0;
	    		typedText = "";
	    		sendBroadcast(intent);
	    	}
	    }
	}
	
	/**
	 * Called when the service is started (usually after boot). We register the events we are interested in (change of notification) and also register to the start broadcast event, sent by AIRS
	 * Originally, we set the package filters to something that does not exist, so that we minimise the firing of the callback
	 * After the start broadcast event will be received, the proper package names will be set for recording
	 * @see android.accessibilityservice.AccessibilityService#onServiceConnected()
	 */
	@Override
	protected void onServiceConnected() 
	{
//	    Log.e("AIRS", "connecting NotificationHandlerService");

		// register for any input from the accessbility service
		IntentFilter intentFilter = new IntentFilter("com.airs.accessibility.start");
        registerReceiver(SystemReceiver, intentFilter);
               
        // now switch off initially
	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
	    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
	    info.notificationTimeout = 100;
//	    info.packageNames = new String[] {"com.airs.helpers" };
//	    info.packageNames = new String[] {"com.skype.raider", "com.google.android.gsf" };

	    Log.e("AIRS", "NotificationHandlerService connected");
	    
	    setServiceInfo(info);
	    
		Translate.setClientId("myStress");
		Translate.setClientSecret("ZwRLv7hsttnjqql26s7MglS8enqHG5Wi+uLfzt7Jsgw=");
	}

	/*
	 * Called when interrupting the service
	 * @see android.accessibilityservice.AccessibilityService#onInterrupt()
	 */
	@Override
	public void onInterrupt() 
	{
	}
	
	/*
	 * Called when destroying the service
	 * @see android.app.Service#onDestroy()
	 */
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        unregisterReceiver(SystemReceiver);
    }
    
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // if anything sent from the accessbility service
            if (action.equals("com.airs.accessibility.start")) 
            {
            	// start the gathering?
            	if (intent.getBooleanExtra("start", true) == true)
            	{
            	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//            	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
            	    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
            	    info.notificationTimeout = 100;
//            	    info.packageNames = new String[] {"com.skype.raider", "com.google.android.talk", "com.spotify.music", "com.whatsapp", "com.facebook.orca", "com.facebook.katana"};
            	    setServiceInfo(info);
            	    
            	    started = true;
            	    
//            	    Log.e("AIRS", "gathering started");
            	}
            	else	// or stop it?
            	{
            	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//            	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
            	    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
            	    info.notificationTimeout = 100;
//            	    info.packageNames = new String[] {"com.airs.helpers" };
            	    setServiceInfo(info);
            	    
            	    started = false;
            	    
//            	    Log.e("AIRS", "gathering stopped");
            	}
            }
        }
    };
    
    @SuppressLint("NewApi")
	public static List<String> getText(Notification notification)
    {
        // We have to extract the information from the view
        RemoteViews views;
        if (Build.VERSION.SDK_INT >= 16) views = notification.bigContentView;
        else views = notification.contentView;
        if (views == null) return null;

        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        List<String> text = new ArrayList<String>();
        try
        {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions)
            {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;

                // View ID
                parcel.readInt();

                String methodName = parcel.readString();
                if (methodName == null) continue;

                // Save strings
                else if (methodName.equals("setText"))
                {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();

                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    text.add(t);
                }

                // Save times. Comment this section out if the notification time isn't important
                else if (methodName.equals("setTime"))
                {
                    // Parameter type (5 = Long)
                    parcel.readInt();

                    String t = new SimpleDateFormat("h:mm a").format(new Date(parcel.readLong()));
                    text.add(t);
                }

                parcel.recycle();
            }
        }

        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e)
        {
            Log.e("NotificationClassifier", e.toString());
        }

        return text;
    }
}

