/*
Copyright (C) 2013, TecVis LP, support@tecvis.co.uk
Copyright (C) 2014, FIM Research Center, myStress@fim-rc.de

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

import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

//import com.alchemyapi.api.AlchemyAPI;
//import com.alchemyapi.api.AlchemyAPI_NamedEntityParams;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

/**
 * Class to implement the myStress accessibility service
 */
@SuppressLint("InlinedApi")
public class NotificationHandlerService extends AccessibilityService
{	
	private double typingStartTime, typingEndTime;
	private String typedText;
	private boolean wasending1 = false, wasending2 = false, sending = false;
	private boolean sent = false;
	
	/**
	 * Called when an accessibility event occurs - here, we check the particular component packages that fired the event, filtering out the ones we support
	 * @param event Reference to the fired {android.view.accessibility.AccessibilityEvent}
	 * @see android.accessibilityservice.AccessibilityService#onAccessibilityEvent(android.view.accessibility.AccessibilityEvent)
	 */
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event)
	{
		try{
			int eventType = event.getEventType();
			String packageName = event.getPackageName().toString();
			String className = event.getClassName().toString();
			
		    if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED){
		    	processNotification(event);
		    }
		    else if(eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED){
		    	if(event.isPassword()) return;
		    	
		    	String text = event.getText().toString();
		    	String beforeText = "";
		    	if(event.getBeforeText() != null)
		    		beforeText = event.getBeforeText().toString();
		    	text = text.substring(1, text.length()-1);
		    	
				if(packageName.equals("com.whatsapp")){
					if(wasending1 && text.length() == 1){
				    	wasending2 = true;
				    	sendButtonClicked(packageName);
					}
				}
//				else if(packageName.equals("com.facebook.orca")){
//					wasending1 = false;
//				}
//				else if(packageName.equals("com.facebook.katana")){
//					wasending1 = false;
//				}
//				else if(packageName.equals("com.android.email")){
//					wasending1=false;
//				}
				else{
					wasending1 = false;
//					if(text.length() == 0){
//						sending = true;
//						sendButtonClicked(packageName);
//					}
				}
		    	
		    	boolean del;
		    	int length_diff = text.length()-beforeText.length();
		    	typedText = text;
		    	
		    	if(length_diff == 1 && text.length() == 1){
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
		    			sendButtonClicked(packageName);
		    		}
		    	}
		    	else{
		    		if(wasending1){
		    			wasending2 = true;
		    			sendButtonClicked(packageName);
		    		}
		    		else if(packageName.equals("com.facebook.orca")){
			    		if(event.getContentDescription()!=null){
				    		if(event.getContentDescription().toString().toLowerCase().contains("send")){
				    			sending = true;
				    			sendButtonClicked(packageName);
				    		}
			    		} else {
				    		if(className.equals("com.facebook.orca.compose.ComposerButton")){
				    			sending = true;
				    			sendButtonClicked(packageName);
				    		}
			    		}
			    	}
			    	else if(packageName.equals("com.facebook.katana")){
			    		if(event.getContentDescription()!=null){
				    		if(event.getContentDescription().toString().toLowerCase().contains("post")){
				    			sending = true;
				    			sendButtonClicked(packageName);
				    		}
			    		} else {
				    		if(className.equals("com.facebook.widget.text.SimpleVariableTextLayoutView")){
				    			sending = true;
				    			sendButtonClicked(packageName);
				    		}
			    		}
			    	}
			    	else if(packageName.toLowerCase().contains("mail")){
			    		if(event.getContentDescription()!=null){
				    		if(event.getContentDescription().toString().toLowerCase().contains("send")){
				    			sending = true;
				    			sendButtonClicked(packageName);
				    		}
			    		} else {
				    		if(className.equals("android.widget.ImageButton")){
				    			if(!event.getText().toString().equals("")){
				    				sending = true;
				    				sendButtonClicked(packageName);
				    			}
				    		}
			    		}
			    	}
		    	}
		    }
		    else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
		    	if(wasending1){
		    		wasending2=true;
		    		sendButtonClicked(packageName);
		    	}
		    	
		    	if(packageName.equals("com.android.mms")){
		    		if(className.equals("android.app.ProgressDialog")){
		    			sending = true;
		    			sendButtonClicked(packageName);
		    		}
		    	}
		    }
		    
		    //reset variables
			sending = false;
			sent = false;
		}catch(Exception e){}
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
		// register for any input from the accessbility service
		IntentFilter intentFilter = new IntentFilter("com.myStress.accessibility.start");
        registerReceiver(SystemReceiver, intentFilter);
               
        // now switch off initially
	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
	    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
	    info.notificationTimeout = 100;

	    Log.e("myStress", "NotificationHandlerService connected");
	    
	    setServiceInfo(info);
	    
		Translate.setClientId("myStress");
		Translate.setClientSecret("ZwRLv7hsttnjqql26s7MglS8enqHG5Wi+uLfzt7Jsgw=");
	}
	
	protected void onServiceDisconnected(){
		Log.e("myStress", "NotificationHandlerService disconnected");
	}
	
	protected void onUnbind(){
		Log.e("myStress", "NotificationHandlerService unbound");
	}
    
    public void sendButtonClicked(String packageName){
		if((wasending1 && wasending2) || sending){
			if(wasending1 && wasending2) packageName = "com.whatsapp";
			double typingDuration;
			double typingSpeed = 0;
    		if(typingEndTime != typingStartTime){
	    		typingDuration = (typingEndTime - typingStartTime)/1000.0d;
	    		typingSpeed = typedText.length() / typingDuration;
    		}
	    	
    		if(typedText != null){
		    	if(typedText.length() > 0){
		    		new SAThread(typedText, typingSpeed, packageName);
		    	}
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
	        // now broadcast the capturing of the accessibility service to the handler
			Intent intent = new Intent("com.myStress.accessibility");
			intent.putExtra("NotifyText", event.getPackageName());//::" + notification.tickerText);
			sendBroadcast(intent);
    	}
    }

	/**
	 * Called when interrupting the service
	 * @see android.accessibilityservice.AccessibilityService#onInterrupt()
	 */
	@Override
	public void onInterrupt() 
	{
	}
	
	/**
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
            	    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
            	    info.notificationTimeout = 100;
            	    setServiceInfo(info);
            	}
            	else	// or stop it?
            	{
            	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            	    info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
            	    info.notificationTimeout = 100;
            	    setServiceInfo(info);
            	}
            }
        }
    };
    
	private class SAThread implements Runnable {
		private String text;
		private double speed;
		private String packageName;
		
		public SAThread(String text, double speed, String packageName) {
			this.text = text;
			this.speed = speed;
			this.packageName = packageName;
			new Thread(this).start();
		}

		public void run() {
    		String type="";
    		double score=0;
    		
    		try{
	    		// translate
    			String translatedText = Translate.execute(text, Language.GERMAN, Language.ENGLISH);
    			HttpClient client = new DefaultHttpClient();
    			
    			// analyze sentiment
	    		String getURL = "https://loudelement-free-natural-language-processing-service.p.mashape.com/nlp-text/?text="+URLEncoder.encode(translatedText, "UTF-8");
	    		HttpGet get = new HttpGet(getURL);
	    		//FIXME: EMAIL ENTFERNEN
	    		get.setHeader("X-Mashape-Key", "UfXaXsn0q9mshs2S9vlj2RCA1gzSp1kFZJKjsnA8WvonNOBW7T");
	    		HttpResponse responseGet = client.execute(get);
	    		HttpEntity  resEntityGet = responseGet.getEntity();
	    		if(resEntityGet != null){
	    			String resString = EntityUtils.toString(resEntityGet);
	    			JSONObject result = new JSONObject(resString);
	    			type = result.getString("sentiment-text");
	    			score = result.getDouble("sentiment-score");
	    		}
    		}catch(Exception e){
    			Log.e("myStress", e.getMessage());
    		}finally{    		
				Intent intent = new Intent("com.myStress.accessibility");
				intent.putExtra("TextInformation", packageName + ":" + text.length() + ":" + speed*60 + ":" + type + ":" + score);
				
				sendBroadcast(intent);
    		}
		}
	}
}