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
package com.myStress.handlers;

//import java.net.URL;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
//import org.w3c.dom.Document;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.alchemyapi.api.AlchemyAPI;
import com.alchemyapi.api.AlchemyAPI_NamedEntityParams;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.myStress.R;

/**
 * Class to implement the myStress accessibility service
 */
public class NotificationHandlerService extends AccessibilityService
{	
	NotificationHandlerService service;
	boolean started = true;
//	boolean typing = false;
	boolean newText = false;
	double typingStartTime, typingEndTime;
//	int typedChars;
//	int maxTextLength;
	String typedText;
	boolean wasending1 = false, wasending2 = false, sending = false, mailsending = false;
	boolean sent = false;//, old = false;
//	boolean sending = false, sent = false, clicked = false;
//	double sendingTimeout;
//	double sendingOffset = 5000;
		
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
		String packageName = event.getPackageName().toString();
		String className = event.getClassName().toString();
		
    	Log.e("myStress", "notification: " + packageName + ", " + eventType + ", " + className);
	    if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED){
	    	processNotification(event);
	    }
	    else if(eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED){
	    	if(event.isPassword()) return;
	    	
	    	String text = event.getText().toString();
	    	String beforeText = event.getBeforeText().toString();
	    	text = text.substring(1, text.length()-1);
	    	
			if(packageName.equals("com.whatsapp")){
				if(wasending1 && text.length() == 1){//< typedText.length()){
			    	wasending2 = true;
			    	sendButtonClicked(packageName);
				}
			}
			else if(packageName.equals("com.facebook.orca")){
				wasending1 = false;
			}
			else if(packageName.equals("com.facebook.katana")){
				wasending1 = false;
			}
			else if(packageName.equals("com.android.email")){
				wasending1=false;
			}
			else{
				wasending1 = false;
				if(text.length() == 0){
					sending = true;
					sendButtonClicked(packageName);
				}
			}
	    	
	    	boolean del;
	    	int length_diff = text.length()-beforeText.length();
	    	typedText = text;
	    	
//    		maxTextLength = text.length();
	    	
	    	if(length_diff == 1 && text.length() == 1){
//    			typing = true;
    			typingStartTime = (double)System.currentTimeMillis();
    			typingEndTime = typingStartTime;
	    	}
	    	else{
    			typingEndTime = (double)System.currentTimeMillis();
	    	}
	    	
	    	if(text.length() < beforeText.length()){
	    		del = true;
	    		length_diff = -length_diff;
	    	}
	    	else{
	    		del = false;
	    	}
	    	
	    	if(del == true){
	    		Log.e("myStress", "text deleted");//: " + diff);
				Intent intent = new Intent("com.myStress.accessibility");
				intent.putExtra("KeyLogger", length_diff + " characters deleted");
				sendBroadcast(intent);
	    	}
	    }
	    else if(eventType == AccessibilityEvent.TYPE_VIEW_CLICKED){
	    	if(packageName.equals("com.whatsapp")){
	    		if(className.equals("android.widget.ImageButton")){
    				wasending1 = true;
		    	}
	    		else if(className.equals("com.whatsapp.EmojiPicker$EmojiImageView")){
	    			wasending1 = false;
	    			wasending2 = false;
	    		}
	    		else if(className.equals("android.widget.ListView") || className.equals("android.widget.ImageView"));
	    		else{
	    			if(wasending1) wasending2 = true;
	    		}
	    	}
	    	else{
	    		if(wasending1) wasending2 = true;
	    		
		    	if(packageName.equals("com.facebook.orca")){
		    		if(className.equals("com.facebook.orca.compose.ComposerButton")){
		    			sending = true;
		    			sendButtonClicked(packageName);
		    		}
		    	}
		    	else if(packageName.equals("com.facebook.katana")){
		    		if(className.equals("com.facebook.widget.text.SimpleVariableTextLayoutView")){
		    			sending = true;
		    			sendButtonClicked(packageName);
		    		}
		    	}
		    	else if(packageName.equals("com.android.email")){
		    		if(className.equals("android.widget.ImageButton")){
		    			if(!event.getText().toString().equals("")){
		    				sending = true;
		    				sendButtonClicked(packageName);
		    			}
		    		}
		    	}
	    	}
	    }
	    else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
	    	if(wasending1){
	    		wasending2=true;
	    		sendButtonClicked(packageName);
	    		sent = true;
	    	}
	    }
	    
	    if(sent == false){
	    	sendButtonClicked(packageName);
	    }
	    sent = false;
	}
	
	/**
	 * Called when the service is started (usually after boot). We register the events we are interested in (change of notification) and also register to the start broadcast event, sent by myStress
	 * Originally, we set the package filters to something that does not exist, so that we minimise the firing of the callback
	 * After the start broadcast event will be received, the proper package names will be set for recording
	 * @see android.accessibilityservice.AccessibilityService#onServiceConnected()
	 */
	@Override
	protected void onServiceConnected() 
	{
//	    Log.e("myStress", "connecting NotificationHandlerService");

		// register for any input from the accessbility service
		IntentFilter intentFilter = new IntentFilter("com.myStress.accessibility.start");
        registerReceiver(SystemReceiver, intentFilter);
               
        // now switch off initially
	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
	    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
	    info.notificationTimeout = 100;
//	    info.packageNames = new String[] {"com.myStress.helpers" };
//	    info.packageNames = new String[] {"com.skype.raider", "com.google.android.gsf" };

	    service = this;
	    Log.e("myStress", "NotificationHandlerService connected");
	    
	    setServiceInfo(info);
	    
		Translate.setClientId("myStress");
		Translate.setClientSecret("ZwRLv7hsttnjqql26s7MglS8enqHG5Wi+uLfzt7Jsgw=");
	}
	
	protected void onServiceDisconnected(){
		service = null;
		Log.e("myStress", "NotificationHandlerService disconnected");
	}
	
	protected void onUnbind(){
		service = null;
		Log.e("myStress", "NotificationHandlerService unbound");
	}
    
    public void sendButtonClicked(String packageName){
		if((wasending1 && wasending2) || sending){
//	    	if(typing == true){
			double typingDuration;
			double typingSpeed = 0;
    		if(typingEndTime != typingStartTime){
	    		typingDuration = (typingEndTime - typingStartTime)/1000.0d;
	    		typingSpeed = typedText.length() / typingDuration;
	    		Log.e("myStress", "Tippgeschwindigkeit: "+typingSpeed + " Zeichen pro Minute");
    		}
//	    		typing = false;
//	    	}
	    	
	    	if(typedText.length() > 0){
	    		new SAThread(typedText, typingSpeed, packageName);
	    	}
			wasending1 = false;
			wasending2 = false;
			sending = false;
			sent = true;
		}
    }
    
    public void processNotification(AccessibilityEvent event){
    	// get notification shown
    	Notification notification = (Notification)event.getParcelableData();
    	
    	if (notification != null)
    	{
	    	// now parse the specific packages we support
	    	// start with GTalk
	    	if (event.getPackageName().toString().compareTo("com.google.android.talk") == 0)
	    	{
		        // now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.myStress.accessibility");
				intent.putExtra("NotifyText", "gtalk");//::" + notification.tickerText);
				sendBroadcast(intent);
	    	}
	    	
	    	// anything from Skype?
	    	if (event.getPackageName().toString().compareTo("com.skype.raider") == 0)
	    	{
		        // now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.myStress.accessibility");
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
	    				Log.e("myStress", "Can't find token in '" + notification.tickerText +"'");
	    		}				
	    	}
	    	if(event.getPackageName().toString().compareTo("com.whatsapp") == 0){
	    		//Log.e("myStress", "whatsapp message");
		        // now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.myStress.accessibility");
				intent.putExtra("NotifyText", "whatsapp");//::" + getText(notification));
				sendBroadcast(intent);	
	    	}
	    	if(event.getPackageName().toString().compareTo("com.facebook.orca") == 0){
	    		// now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.myStress.accessibility");
				intent.putExtra("NotifyText", "fbm");//::" + getText(notification));
				sendBroadcast(intent);
	    	}
	    	if(event.getPackageName().toString().compareTo("com.facebook.katana") == 0){
	    		// now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.myStress.accessibility");
				intent.putExtra("NotifyText", "fb");//::" + getText(notification));
				sendBroadcast(intent);
	    	}
    	}
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
            if (action.equals("com.myStress.accessibility.start")) 
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
            	    
//            	    Log.e("myStress", "gathering started");
            	}
            	else	// or stop it?
            	{
            	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//            	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
            	    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
            	    info.notificationTimeout = 100;
//            	    info.packageNames = new String[] {"com.myStress.helpers" };
            	    setServiceInfo(info);
            	    
            	    started = false;
            	    
//            	    Log.e("myStress", "gathering stopped");
            	}
            }
        }
    };
    
	private class SAThread implements Runnable {
		String text;
		double speed;
		String packageName;
		
		SAThread(String text, double speed, String packageName) {
			this.text = text;
			this.speed = speed;
			this.packageName = packageName;
			new Thread(this).start();
		}

		public void run() {
    		Log.e("myStress", "Textlänge: "+text.length());
    		String type="";
    		double score=0;
    		
    		try{
	    		Log.e("myStress", "Text: "+text);
	    		//übersetzen
    			String translatedText = Translate.execute(text, Language.GERMAN, Language.ENGLISH);
    			Log.e("myStress", "translated: "+translatedText);
	    		// Sentimentanalyse
	    		AlchemyAPI_NamedEntityParams nep = new AlchemyAPI_NamedEntityParams();
	    		nep.setSentiment(true);
	    		
	    		try{
		    		HttpClient client = new DefaultHttpClient();
		    		String getURL = "https://loudelement-free-natural-language-processing-service.p.mashape.com/nlp-text/?text="+URLEncoder.encode(translatedText, "UTF-8");
		    		HttpGet get = new HttpGet(getURL);
		    		get.setHeader("X-Mashape-Key", "UfXaXsn0q9mshs2S9vlj2RCA1gzSp1kFZJKjsnA8WvonNOBW7T");
		    		HttpResponse responseGet = client.execute(get);
		    		HttpEntity  resEntityGet = responseGet.getEntity();
		    		if(resEntityGet != null){
		    			String resString = EntityUtils.toString(resEntityGet);
		    			JSONObject result = new JSONObject(resString);
		    			type = result.getString("sentiment-text");
		    			score = result.getDouble("sentiment-score");
		    		}
	    		} catch(Exception e){
	    			e.printStackTrace();
	    		}
	    		
	    		// old api
/*	    		Document doc = api.TextGetTextSentiment(translatedText, nep);
    			NodeList list = doc.getFirstChild().getChildNodes();
    			for(int i = 0; i < list.getLength();i++) {
    				Node item = list.item(i);
    				
    				if(item.getNodeName() == null)
    					continue;
    				
    				if(item.getNodeName().equals("docSentiment"))
    				{
    					type = item.getChildNodes().item(1).getChildNodes().item(0).getNodeValue();
    					if(!type.equals("neutral"))
    						score = Double.parseDouble(item.getChildNodes().item(3).getChildNodes().item(0).getNodeValue());
    					else
    						score = 0;
    					break;
    				}
    			}*/
    			
    			Log.e("myStress", "sentiment: "+type+" with score "+score);
    		}catch(Exception e){
    			Log.e("myStress", e.getMessage());
    		}
    		
    		Intent intent = new Intent("com.myStress.accessibility");
    		intent.putExtra("TextLength", packageName + ":" + text.length() + ":" + speed*60 + ":" + type + ":" + score);
    		
//    		maxTextLength = 0;
//    		typedText = "";
    		sendBroadcast(intent);
		}
	}
}