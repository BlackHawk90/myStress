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
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.myStress.R;
import com.myStress.helper.FFT;
import com.myStress.helper.MFCC;
import com.myStress.helper.SerialPortLogger;
import com.myStress.helper.Waker;
import com.myStress.helper.Window;
import com.myStress.platform.HandlerManager;
import com.myStress.platform.History;
import com.myStress.platform.SensorRepository;

/** 
 * Class to read audio-related sensors, specifically the AS and AF sensor
 * @see Handler
 */
public class CallAudioHandler implements Handler
{
	private Context myStress;
	private static int RECORDER_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
	private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static int RECORDER_SAMPLERATE = 8000;
	
	private static int FFT_SIZE = 8192;
	private static int MFCCS_VALUE = 12;
	private static int MEL_BANDS = 20;
	private static double[] FREQ_BANDEDGES = {50,250,500,1000,2000};
	
	private int bufferSize = 0;
	private int bufferSamples = 0;
	private static int[] freqBandIdx = null;
	
    private FFT featureFFT = null;
    private MFCC featureMFCC = null;
    private Window featureWin = null;
    
    private AudioRecord audioRecorder = null;
	
    public double prevSecs = 0;
	public double[] featureBuffer = null;
	
	// availability of player
	private boolean available = false;
	private boolean shutdown = false;
	
	private String audiofeatures = null;
	private boolean started = false, callactive = false;
	private Semaphore call_semaphore = new Semaphore(1);
	
	/**
	 * Sleep function 
	 * @param millis
	 */
	private void sleep(long millis) 
	{
		Waker.sleep(millis);
	}
	
	private void wait(Semaphore sema)
	{
		try
		{
			sema.acquire();
		}
		catch(Exception e)
		{
		}
	}
	
	/**
	 * Method to acquire sensor data
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.myStress.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public synchronized byte[] Acquire(String sensor, String query)
	{
		// are we shutting down?
		if (shutdown == true)
			return null;

		// acquire data and send out
		if(sensor.equals("AU")){
			wait(call_semaphore);
			
			if(!started){
		        ((TelephonyManager)myStress.getSystemService(Context.TELEPHONY_SERVICE)).listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		        started = true;
			}
			
			if(callactive){
				handleAudioStream();
				
				StringBuffer AU_reading = new StringBuffer(sensor);
				AU_reading.append(audiofeatures);
				
				return AU_reading.toString().getBytes();
			}
		}
		return null;
	}

	/**
	 * Method to share the last value of the given sensor
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.myStress.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{
		// acquire data and send out
//		if(sensor.equals("AU")){
//			return "My current ambient sound level is at "+String.valueOf((double)level / 100) + " dB";
//		}
//		else return null;
		return null;
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.myStress.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
		if(sensor.equals("AU")){
			History.timelineView(myStress, "Call Audio Analysis", "AU");
		}
	}
	
	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.myStress.platform.Sensor} entries will be added to the {@link com.myStress.platform.SensorRepository}
	 * @see com.myStress.handlers.Handler#Discover()
	 * @see com.myStress.platform.Sensor
	 * @see com.myStress.platform.SensorRepository
	 */
	public void Discover()
	{
		// return right away if no player could be created in constructor!
		if (available == false)
			return;
		
		// here some midlet property check as to whether or not audio capture is supported
		SensorRepository.insertSensor(new String("AU"), new String("text"), myStress.getString(R.string.AU_d), myStress.getString(R.string.AU_e), new String("txt"), 0, 0, 15000, true, 0, this);
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, reading the various RMS values of the preferences
	 * Then, determining the minimal buffer size for the recording
	 * Then, creating an AudioPlayer just to see if it works (tear it down again right after creation)
	 * @param myStress Reference to the calling {@link android.content.Context}
	 */
	public CallAudioHandler(Context myStress)
	{
		// store for later
		this.myStress = myStress;
		
    	bufferSize = AudioRecord.getMinBufferSize(
        		RECORDER_SAMPLERATE,
        		RECORDER_CHANNELS,
        		RECORDER_AUDIO_ENCODING);

	    bufferSize = Math.max(bufferSize, RECORDER_SAMPLERATE*2);
	    bufferSamples = bufferSize/2;
	    
	    
	    //allocateFrameFeatureBuffer(STREAM_FEATURES);
	    
	    featureFFT = new FFT(FFT_SIZE);
	    featureWin = new Window(bufferSamples);
	    featureMFCC = new MFCC(FFT_SIZE, MFCCS_VALUE, MEL_BANDS, RECORDER_SAMPLERATE);
	    
	    freqBandIdx = new int[FREQ_BANDEDGES.length];
	    for (int i = 0; i < FREQ_BANDEDGES.length; i ++)
	    {
	    	freqBandIdx[i] = Math.round((float)FREQ_BANDEDGES[i]*((float)FFT_SIZE/(float)RECORDER_SAMPLERATE));
	    	//writeLogTextLine("Frequency band edge " + i + ": " + Integer.toString(freqBandIdx[i]));
	    }
	    
	    boolean buffer_error = true;
	    do
	    {
	    	try
	    	{
			    audioRecorder = new AudioRecord(
			    		RECORDER_SOURCE,
						RECORDER_SAMPLERATE,
						RECORDER_CHANNELS,
						RECORDER_AUDIO_ENCODING,
						bufferSize);
			    if(audioRecorder != null)
			    	if(audioRecorder.getState() == AudioRecord.STATE_INITIALIZED)
			    	{
					    prevSecs = (double)System.currentTimeMillis()/1000.0d;
						// release player again until needed
					    audioRecorder.release();
					    audioRecorder = null;
					    available = true;
			    	    buffer_error = false;
			    	}
	    	} catch (Exception e) {
	    		buffer_error = true;
				// increase buffer size by 10%
	    		bufferSize += bufferSize/10;
	    	}
	    } while(buffer_error);

	}
	
	/**
	 * Method to release all handler resources
	 * Here, we stop any playing AudioPlayer and release its resources
	 * @see com.myStress.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down!
		shutdown = true;
		if(audioRecorder!= null) {
			if (audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
				audioRecorder.stop();
	        audioRecorder.release();
	        audioRecorder = null;
		}
		call_semaphore .release();
	}
	
	private void handleAudioStream()
	{
        short data16bit[] = new short[bufferSamples];
    	byte data8bit[] = new byte[bufferSize];
    	double fftBufferR[] = new double[FFT_SIZE];
    	double fftBufferI[] = new double[FFT_SIZE];
    	double featureCepstrum[] = new double[MFCCS_VALUE];
    	
	    int readAudioSamples = 0;
	    boolean havePlayer = ((AudioHandler) HandlerManager.getHandler("AudioHandler")).havePlayer;
		while (havePlayer == true)
			sleep(100);
		// now we have the player!
		((AudioHandler) HandlerManager.getHandler("AudioHandler")).havePlayer = true;
	    
		try
		{
			audioRecorder = new AudioRecord(
		   		RECORDER_SOURCE,
				RECORDER_SAMPLERATE,
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING,
				bufferSize);
	    
			if (audioRecorder != null)
			{
				if (audioRecorder.getState() == AudioRecord.STATE_INITIALIZED)
				{
					audioRecorder.startRecording();
					
					readAudioSamples = audioRecorder.read(data16bit, 0, bufferSamples);
			    	double currentSecs = (double)(System.currentTimeMillis())/1000.0d;
//			    	double diffSecs = currentSecs - prevSecs;
			    	prevSecs = currentSecs;
				    
			    	audiofeatures = new String("");
			    	
//			    	JsonObject data = new JsonObject();
			    	
			    	if (readAudioSamples > 0)
			    	{
			    		double fN = (double)readAudioSamples;
			
//			    		data.addProperty(DIFF_SECS, diffSecs);
//			    		audiofeatures = new String();
			
			    		// Convert shorts to 8-bit bytes for raw audio output
			    		for (int i = 0; i < bufferSamples; i ++) {
			    			data8bit[i*2] = (byte)data16bit[i];
			    			data8bit[i*2+1] = (byte)(data16bit[i] >> 8);
			    		}
			
			    		// L1-norm
			    		double accum = 0;
			    		for (int i = 0; i < readAudioSamples; i ++)
			    			accum += Math.abs((double)data16bit[i]);
			    		
//			    		data.addProperty(L1_NORM, accum/fN);
			    		// Write L1_NORM
			    		audiofeatures += accum/fN + ":";
			
			    		// L2-norm
			    		accum = 0;
			    		for (int i = 0; i < readAudioSamples; i ++)
			    			accum += (double)data16bit[i]*(double)data16bit[i];
//			    		data.addProperty(L2_NORM, Math.sqrt(accum/fN));
			    		// Write L2_NORM
			    		audiofeatures += Math.sqrt(accum/fN) + ":";
			
			    		// Linf-norm
			    		accum = 0;
			    		for (int i = 0; i < readAudioSamples; i ++)
			    			accum = Math.max(Math.abs((double)data16bit[i]),accum);
//			    		data.addProperty(LINF_NORM, Math.sqrt(accum));
			    		
			    		// Write LINF_NORM
			    		audiofeatures += Math.sqrt(accum) + ":";
			    		
			    		// Frequency analysis
			    		Arrays.fill(fftBufferR, 0);
			    		Arrays.fill(fftBufferI, 0);
			
			    		// Convert audio buffer to doubles
			    		for (int i = 0; i < readAudioSamples; i++)
			    			fftBufferR[i] = data16bit[i];
			
			    		// In-place windowing
			    		featureWin.applyWindow(fftBufferR);
			
			    		// In-place FFT
			    		featureFFT.fft(fftBufferR, fftBufferI);
			
			    		// Get PSD across frequency band ranges
			    		double[] psdAcrossFrequencyBands = new double[FREQ_BANDEDGES.length - 1];
			    		for (int b = 0; b < (FREQ_BANDEDGES.length - 1); b ++)
			    		{
			    			int j = freqBandIdx[b];
			    			int k = freqBandIdx[b+1];
			    			accum = 0;
			    			for (int h = j; h < k; h ++)
			    				accum += fftBufferR[h]*fftBufferR[h] + fftBufferI[h]*fftBufferI[h];

			    			psdAcrossFrequencyBands[b] = accum/((double)(k - j));
			    		}
			//    		Gson gson = getGson();
			//    		data.add(PSD_ACROSS_FREQUENCY_BANDS, gson.toJsonTree(psdAcrossFrequencyBands));
			    		// Write PSD
			    		int i=0;
			    		for(;i<FREQ_BANDEDGES.length-2;i++)
			    			audiofeatures += psdAcrossFrequencyBands[i]+",";
			    		
			    		audiofeatures += psdAcrossFrequencyBands[i]+":";
			    		
			    		// Get MFCCs
			    		featureCepstrum = featureMFCC.cepstrum(fftBufferR, fftBufferI);
			    		// Write MFCCs
			    		for(i=0;i<MFCCS_VALUE-2;i++)
			    			audiofeatures += featureCepstrum[i]+",";
			    		audiofeatures += featureCepstrum[i]+"";
			//    		data.add(MFCCS, gson.toJsonTree(featureCepstrum));
			    		Log.e("myStress",audiofeatures);
			    		
			    	}
				}
			}
	    } catch (Exception e) {
	    	SerialPortLogger.debug("CallAudioHandler:Exception when requesting AudioRecord!");
	    } finally {
			// don't need player anymore
			((AudioHandler) HandlerManager.getHandler("AudioHandler")).havePlayer = false;
    		if(callactive) call_semaphore.release();
	    }
	}
	
	public final PhoneStateListener phoneListener = new PhoneStateListener() 
	{
        @Override
        public void onCallStateChanged(int state, String incomingNumber){
        	switch(state){
        	case TelephonyManager.CALL_STATE_OFFHOOK:
        		callactive = true;
        		call_semaphore.release();
        		break;
        	default:
        		callactive = false;
        		break;
        	}
        }
    };
}
