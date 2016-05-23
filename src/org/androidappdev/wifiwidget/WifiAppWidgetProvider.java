package org.androidappdev.wifiwidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

/* 
 * Code is largely based on
 * com.android.settings.widget.SettingsAppWidgetProvider
 * from Android 1.6 (git tag: android-1.6_r1)
 */
public class WifiAppWidgetProvider extends AppWidgetProvider {
	static final String TAG = "WifiAppWidgetProvider";

	private static final int BUTTON_WIFI = 0;

	private static final int STATE_DISABLED = 0;
	private static final int STATE_ENABLED = 1;
	private static final int STATE_INTERMEDIATE = 2;
	
	 private final int MILLISEC_BETWEEN_WIFI_RETRIES = 5000;
	 private final int NUMBER_OF_SCAN_RETRIES = 5;
	 private final int MILLISEC_BETWEEN_SCAN_RETRIES = 2000;
	 
	 public static final int DATA_TO_CONNECT_IS_AVAILABLE = 0;	
	 public static final int NO_NETWORK_IN_SCAN = 1;
	 public static final int NO_SAVED_NETWORK = 2;
	 public static final int NO_SAVED_NETWORK_IN_SCAN = 3;	
	 
	 private final int MILLISEC_BETWEEN_WIFI_FIXES_RETRIES = 10000; //10 seconds
	 private final int NUMBER_OF_WIFI_FIXES = 3;
	
	 
	// For wifi Fixer
	private static final int NULLVAL = -1;
	private static int lastAP = NULLVAL;
	
	private static final String COLON = ":";
	private static final String NEWLINE = "\n";
	private static final String SEMICOLON = " ; ";
	private static final String LINE_SEPERATOR = "---------------------------------------------------------";
	
	// Constants used for different security types
    public static final String PSK = "PSK";
    public static final String WEP = "WEP";
    public static final String EAP = "EAP";
    public static final String OPEN = "Open";
	
	private List<WFConfig> knownbysignal;

	static final ComponentName THIS_APPWIDGET = new ComponentName(
			"org.androidappdev.wifiwidget",
			"org.androidappdev.wifiwidget.WifiAppWidgetProvider");

	private static int previousState;
	
	static int[] mAppWidgetIds;
	static AppWidgetManager widgetManager;
	private static MyThreadClass msignalLevelThreadObject = null;
	
	public static Object lockObject= new Object();	
	
	private MyHandler hm ;
    
    public MyHandler returnHandler(){
        return hm;
    }
    
   
	
	private class MyHandler extends Handler
	{
		Context handlerContext; 
		
		public MyHandler(Context context)
		{
			handlerContext = context;			
		}
		
		public void handleMessage(Message m) {
        	
        	try
        	{        	
	        	String message = m.getData().getString("MESSAGE");
	            // toast code
	        	Toast toast = Toast.makeText(handlerContext, message, Toast.LENGTH_SHORT);
	    		toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
	    		toast.show();
        	}
        	catch(Exception e)
        	{
        		Log.e(TAG,"Exception in showing toast in handler");
        		if((e != null) && (e.getMessage() !=null))
					Log.e(TAG,e.getMessage());
        	}
        }        	
	}
	
	
	public class MyThreadClass extends Thread {		
		
		private Context thread_Context = null;
		public Boolean stopThread = false;
		// constructor
	    public MyThreadClass (Context context){
	    	thread_Context = context ;
	    }
	    
	    public void setContext (Context context){
	    	synchronized(lockObject) {
	    	thread_Context = context ;
	    	}
	    }
	    
	    public Context getContext (){
	    	Context myContext = null;
	    	synchronized(lockObject) {
	    		myContext = thread_Context ;
	    	}	    	
	    	return myContext;
	    }
		
		public synchronized void  run() {	
			
			Log.i(TAG, "Entered the thread run function");
			int recvdSignalStrength = -100; 
			
			int nloopCount = 0 ; 
        	
        	while ((!stopThread) && (nloopCount < 100)) //Exit thread after 200 seconds
    		{	      		    			
        		nloopCount++;
        		try
        		{
	        		if(isConnectedToWifi(thread_Context)) {
	    				
	    				//crash on startup after factory reset - so moved inside the loop   				
	    				WifiManager wifiManager = (WifiManager) (getContext().getSystemService(Context.WIFI_SERVICE));    	        	
	    				if(wifiManager != null)
	    					recvdSignalStrength = wifiManager.getConnectionInfo().getRssi(); 
	    				
	    				//Update the view
	    	        	RemoteViews views = new RemoteViews(getContext().getPackageName(), R.layout.main);		        	    	        	
			        	
			        	if (views != null) //check for view found
			        	{
		    				if (recvdSignalStrength > -60)
				        	{	        			        		
				        		views.setTextColor(R.id.ssid, Color.GREEN);	        		
				        	}
				        	else if (recvdSignalStrength > -80)
				        	{
				        		views.setTextColor(R.id.ssid, Color.YELLOW);	
				        	}
				        	else // signal level less than -80
				        	{
				        		views.setTextColor(R.id.ssid, Color.RED);
				        		Log.i(TAG,"Received wifi signal is low: " + recvdSignalStrength);
				        	}
			        	}
			        	
			        	//Message messageObject = new Message();
			        	//messageObject.arg1 = recvdSignalStrength;
			        	//returnHandler().updateColor(messageObject);	
			        	
			        	// update all AppWidget ID's (in most
			            // circumstances just the one) with our views
			        	if ((widgetManager != null) && (mAppWidgetIds != null))
			        		widgetManager.updateAppWidget(mAppWidgetIds, views);	        	
	
	    			}
	    			else {				
	    				//not connected - do nothing		
	    			}
        		}
        		catch(Exception e)
				{
        			Log.e(TAG,"A general exception has occured, will ignore this and continue to run.");	
					if((e != null) && (e.getMessage() != null))
						Log.e(TAG,e.getMessage());	
				}  
    			
    			try
				{
					Thread.sleep(MILLISEC_BETWEEN_SCAN_RETRIES); //every 2 seconds
				}
				catch(Exception e)
				{
					Log.e(TAG,e.getMessage());	
				}   								    		
    		}
        	
        	Log.i(TAG, "Exited the thread run function");
    	};
	};
	
	
	public void updateSignalLevelColor(final Context lthread_Context)
	{
			//On every update we want to start this thread with the new context
			try
	    	{			
				if(msignalLevelThreadObject == null) {
					Log.i(TAG, "msignalLevelThreadObject - will create a new object and start run");
					msignalLevelThreadObject = new MyThreadClass(lthread_Context);
					msignalLevelThreadObject.start();
				}
				else {//object exists				
					//stop thread
					/* msignalLevelThreadObject.stopThread = true;
					Thread.sleep(5000); //sleep for 5 seconds for thread to stop
					if(!msignalLevelThreadObject.isAlive())
					{
						msignalLevelThreadObject = null;
						msignalLevelThreadObject = new MyThreadClass(thread_Context);
					}
					else { //is alive
						Log.e(TAG,"Thread is still alive, did not stop");	
						msignalLevelThreadObject.stopThread = false;//continue
					} */
					
					//update new context
					msignalLevelThreadObject.setContext(lthread_Context);
					if(!msignalLevelThreadObject.isAlive())
					{
						msignalLevelThreadObject.stopThread = false;
						msignalLevelThreadObject.start();
					}
					else { 
						Log.e(TAG,"Thread is alive, but context updated ...");	
					} 					
				}			
	    	}
	    	catch (Exception e)
	    	{
	    		Log.e(TAG, "Exception in (re)starting the thread updateSignalLevelColor.");		
	    		if ((e != null) && (e.getMessage()!= null))
					Log.e(TAG, e.getMessage());			        		
	    	}  				
			return;		
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		WifiAppWidgetProvider.previousState = getWifiState(context);
		// Update each requested appWidgetId
		RemoteViews views = buildUpdate(context, -1);

		for (int i = 0; i < appWidgetIds.length; i++) {
			appWidgetManager.updateAppWidget(appWidgetIds[i], views);
		}
		
		// lets copy our stuff
	    mAppWidgetIds = new int[appWidgetIds.length];
	    for (int a = 0; a < appWidgetIds.length; a++)
	    	mAppWidgetIds[a] = appWidgetIds[a];

		widgetManager = appWidgetManager ;
	}

	/**
	 * Receives and processes a button pressed intent or state change.
	 * 
	 * @param context
	 * @param intent
	 *            Indicates the pressed button.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		hm = new MyHandler(context);
		
		if("com.pdiarm.cloneslave.startup".equals(intent.getAction()))
	    {
			ShowToast(context,"Cloner requested to connect to the internet",Toast.LENGTH_SHORT);
			Log.i(TAG, "Cloner requested to connect to the internet - will initiate wifi connection algorithm.");
			enableAndConnectToWifi(context);
	    }
		
		if("com.android.launcher.restorewifi".equals(intent.getAction()))
	    {
			ShowToast(context,"Launcher requested to connect to the internet",Toast.LENGTH_SHORT);
			Log.i(TAG, "Launcher requested to connect to the internet - will initiate wifi connection algorithm.");
			enableAndConnectToWifi(context);
	    }
		
		if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
			Uri data = intent.getData();
			int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
			if (buttonId == BUTTON_WIFI) {
				enableAndConnectToWifi(context);
				
				//Call the signal level color updater thread
				//Show color only on user request
				//Log.i(TAG, "Will also update the signal level color");
				//updateSignalLevelColor(context);				
			}
		}
		
		//Android connectivity bug: Does not automatically connect on reboot
		//Here we invoke the widget enableAndConnectToWifi function if the wifi state is set to true
		final String action = intent.getAction();
	    if ( (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) || (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action))){
	        
	    	Log.i(TAG, "In Wifiwidget enabled or update");	        
	        if(STATE_ENABLED == getWifiState(context)) {
				//We must be connected if the state is enabled
				if (!isConnectedToWifi(context)) { 				
					enableAndConnectToWifi(context);
				}
				//else // good we are connected			
			}
			//else //do nothing
	    }
		// State changes fall through
		updateWidget(context);			
	}

	/**
	 * Check if we're connected
	 * 
	 * @param context
	 * @return true if we're connected, false otherwise
	 */
	private static boolean isConnectedToWifi(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (cm != null) {
			networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		}
		return networkInfo == null ? false : networkInfo.isConnected();
	}
	
	private static boolean isConnectedAny(Context context) {
        ConnectivityManager connectivityManager 
              = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
        activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

	/**
	 * Enables and calls the EnableWifiAlgorithm
	 * 
	 * @param context
	 */
	private void enableAndConnectToWifi(Context context) {
		
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		int wifiState = getWifiState(context);
		
		if (isConnectedToWifi(context)) //connected to wifi
		{
			ShowToast(context,"Wifi Network already connected",Toast.LENGTH_SHORT);
			return;
		}
		else //wifi not connected
		{
			Log.i(TAG, "Wifi is not connected");
			if(isConnectedAny(context))
			{
				Log.i(TAG, "Ethernet is connected");
				ShowToast(context,"Ethernet Network already connected",Toast.LENGTH_SHORT);
				return;
			}
			else
			{
				Log.i(TAG, "Neither wifi nor ethernet is connected");
				//loop through SSIDs and connect
				if (wifiState == STATE_ENABLED) {			
					//PDi - do not disable
					//wifiManager.setWifiEnabled(false);
					Log.e(TAG, "Network is not connected but Wifi state is enabled");
					ShowToast(context,"Wifi State is already enabled, please wait for a couple of seconds ... ",Toast.LENGTH_SHORT);
				} 
				else 	
				{ //if (wifiState == STATE_DISABLED) {	
					Log.i(TAG, "Network not connected and Wifi state is not enabled. Will now set wifi to enabled ...");
					wifiManager.setWifiEnabled(true);
					//sleep for 5 seconds - to let the enabled network connect by itself
					//ShowToast(context,"Wifi State has been set to enabled, please wait for a couple of seconds ... ",Toast.LENGTH_SHORT);		
					//toast does not show if sleeping
					try
					{
						Thread.sleep(5000);
					}
					catch(Exception e)
					{
						if ((e != null) && (e.getMessage()!= null))
								Log.e(TAG, e.getMessage());
					}
					ShowToast(context,"Wifi State has been set to enabled, please wait for a couple of seconds ... ",Toast.LENGTH_SHORT);										
				}
				
				Log.i(TAG, "Will now call the enable wifi algorithm");
				EnableWifiAlgorithm(context);				
			}
		}
		
		
	}
	
	private void ShowToast(Context context, String message, int duration)
	{
		Toast toast = Toast.makeText(context, message, duration);
		toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 0);
		toast.show();			
	}
	
	private void ShowToastFromThread(Context context, String message, int duration)
	{
		Message messageObject = new Message();
		Bundle myBundle = new Bundle();
		myBundle.putString("MESSAGE", message);
		myBundle.putInt("DURATION", duration);
		messageObject.setData(myBundle); //costly - use ints
		
		this.returnHandler().sendMessage(messageObject);	
	}
	
	public static WifiManager getWifiManager(final Context context) {		
		
		return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

	}
	
	private static WifiConfiguration getNetworkByNID(Context context,
			final int network) {
		List<WifiConfiguration> configs = getWifiManager(context)
				.getConfiguredNetworks();
		for (WifiConfiguration w : configs) {
			if (w.networkId == network)
				return w;
		}
		return null;
	}
	
	private static void logScanResultAllScanned(final Context context,
			final List<ScanResult> scanResultsList) {
		
		Log.i(TAG, LINE_SEPERATOR);
		Log.i(TAG, "Available wifi networks are the following: ");
		for (ScanResult sResult : scanResultsList) 
		{	
			StringBuilder out = new StringBuilder(context.getString(R.string.found_ssid));
			out.append(sResult.SSID);
			out.append(SEMICOLON);
			out.append("BSSID-");
			out.append(sResult.BSSID);
			out.append(SEMICOLON);
			out.append(context.getString(R.string.capabilities));
			out.append(sResult.capabilities);
			out.append(SEMICOLON);
			out.append("Client frequency(MHz)-");
			out.append(String.valueOf(sResult.frequency));
			out.append(SEMICOLON);
			out.append(context.getString(R.string.signal_level));
			out.append(String.valueOf(sResult.level));
			out.append(SEMICOLON);
			Log.i(TAG, out.toString());
		}
		Log.i(TAG, LINE_SEPERATOR);
	}
	
	private static void logScanResultAllSaved(final Context context,
			final List<WifiConfiguration> wifiConfigs) {
		
		Log.i(TAG, LINE_SEPERATOR);
		Log.i(TAG, "Saved wifi networks are the following: ");
		for (WifiConfiguration sResult : wifiConfigs) 
		{		
			StringBuilder out = new StringBuilder(context.getString(R.string.found_ssid));
			out.append(sResult.SSID);
			out.append(SEMICOLON);
			out.append(context.getString(R.string.capabilities));
			out.append(sResult.allowedKeyManagement);
			out.append(SEMICOLON);
			out.append(context.getString(R.string.priority));
			out.append(String.valueOf(sResult.priority));
			Log.i(TAG, out.toString());
		}
		Log.i(TAG, LINE_SEPERATOR);
	}
	
	private void logKnownBySignal(final Context context) {
		
		Log.i(TAG, LINE_SEPERATOR);		
		Log.i(TAG, "The following are Saved wifi networks with signal levels");
		for (WFConfig sResult : knownbysignal) //private List<WFConfig> 
		{		
			StringBuilder out = new StringBuilder(context.getString(R.string.found_ssid));
			out.append(sResult.wificonfig.SSID);
			out.append(SEMICOLON);
			out.append(context.getString(R.string.capabilities));
			out.append(sResult.wificonfig.allowedKeyManagement);
			out.append(SEMICOLON);
			out.append(context.getString(R.string.priority));
			out.append(String.valueOf(sResult.wificonfig.priority));
			out.append(SEMICOLON);
			out.append(context.getString(R.string.signal_level));
			out.append(String.valueOf(sResult.level));
			Log.i(TAG, out.toString());
		}
		Log.i(TAG, LINE_SEPERATOR);
	}
	

	private static int containsSSID_return_int(final String ssid,
			final List<WifiConfiguration> wifiConfigs) {
		for (WifiConfiguration sResult : wifiConfigs) {
			if (StringUtil.removeQuotes(sResult.SSID).equals(ssid))
				return sResult.networkId;
		}
		return -1;
	}
	
	private static boolean containsBSSID(final String bssid,
			final List<WFConfig> results) {
		for (WFConfig sResult : results) {
			if (sResult.wificonfig.BSSID.equals(bssid))
				return true;
		}
		return false;
	}
	
	private static boolean containsSSID(final String ssid,
			final List<WFConfig> results) {
		for (WFConfig sResult : results) {
			if (StringUtil.removeQuotes(sResult.wificonfig.SSID).equals(ssid))
				return true;
		}
		return false;
	}
	
	/**
     * @return The security of a given {@link ScanResult}.
     */
    public static String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = { WEP, PSK, EAP };
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }
        
        return OPEN;
    }
	
	
	private synchronized int ConnectToKnownAPs(final Context context) {
		
		int returnCode = ConnectToSavedNetworks(context);
		
		//Just return if we are connected
		if (isConnectedToWifi(context)) //connected to wifi
		{
			ShowToastFromThread(context,"Wifi Network connected",Toast.LENGTH_SHORT);
			Log.i(TAG, "Wifi network is connected");
			return 0;
		}
		
		//If we are here, implies we could not connect using the saved list
		//we now have to go through the scan list and attempt to connect to them one by one
		//Note that we can only try the open/unsecured ones
		ShowToastFromThread(context,"Could not connect to wifi using the saved networks ...",Toast.LENGTH_SHORT);

		//NIH:"Tablet needs to connect only to saved networks."
		//Back to default Android Behavior
		//ConnectToUnsavedOpenNetworks(context);		
		
		//Act based on the current return code
		if (returnCode != DATA_TO_CONNECT_IS_AVAILABLE)
		{
			ShowMessageFromThread( context, returnCode);
			return 1;
		}
		
		//We are here if the data to connect is available
		//But the connection has not been established
		
		int l_counter = NUMBER_OF_WIFI_FIXES - 1;
		do
		{			
			if (isConnectedToWifi(context)) //connected to wifi
			{
				ShowToastFromThread(context,"Wifi Network connected",Toast.LENGTH_SHORT);
				Log.i(TAG, "Wifi network is connected");
				break;
			}
			else
			{
				ShowToastFromThread(context,"Still working on connecting to a saved network ...",Toast.LENGTH_SHORT);
				
				try
				{
					Thread.sleep(MILLISEC_BETWEEN_WIFI_FIXES_RETRIES * (NUMBER_OF_WIFI_FIXES - l_counter)); 
				}
				catch(Exception e)
				{
					Log.e(TAG,e.getMessage());	
				}		
			}
			
			Log.i(TAG, "Invoking wifi fix number : " + (NUMBER_OF_WIFI_FIXES - l_counter));
			WifiFix(context, l_counter);
			
			l_counter--;				    						    			
		
		}while(l_counter >= 0 );		
		
		return 0;			
		
	}
	
	
	private void WifiFix(Context context, int stepNumber)
	{
		switch(stepNumber) {
		
		case 0:
			 //finally - ask the user for the reboot option 
			 //system user cannot kill netd
			 ShowToastFromThread(context,"Not able to connect to the saved network.",Toast.LENGTH_SHORT);
			 ShowMessageFromThread( context, DATA_TO_CONNECT_IS_AVAILABLE);		
			break;
		case 1:				
			/*TODO: restart a particular service instead of system
			 * 
			ShowToastFromThread(context,"Attempting to restart system service ",Toast.LENGTH_SHORT);
			ActivityManager manager = (ActivityManager) (context.getSystemService(Context.ACTIVITY_SERVICE));
			List<RunningAppProcessInfo> services = manager.getRunningAppProcesses();		 
			 
			 for (RunningAppProcessInfo service : services) {					
				 String servicename = service.processName;
				 if(servicename.equalsIgnoreCase("system"))
				 {
					 Log.i(TAG, "Going to kill " + servicename);
					 android.os.Process.killProcess(service.pid);
				 }	
			 }
			 */
			break;
		case 2:
			//change state to disabled and then again enabled
			//helps sometimes
			ShowToastFromThread(context,"Attempting to toggle wifi state ...",Toast.LENGTH_SHORT);			
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			Log.i(TAG, "Setting wifi state to disabled ...");			
			wifiManager.setWifiEnabled(false);
			try {Thread.sleep(2000);	} catch(Exception e){Log.e(TAG,e.getMessage());}	
			Log.i(TAG, "Setting wifi state to enabled ...");
			wifiManager.setWifiEnabled(true);
			break;
		default:
				break;		
		}
				
	}
	
	private void ShowMessageFromThread(Context context, int messageId)
	{
		Intent intent = new Intent(context, ShowDialog.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("MESSAGEID", messageId);
		context.startActivity(intent);		
	}
	

	
	private int ConnectToSavedNetworks(final Context context)
	{
		
		/*
		 * Comparator class for sorting results for WFConfig
		 */
		class SortByPriorityAndSignal implements Comparator<WFConfig> {
			@Override
			public int compare(WFConfig o2, WFConfig o1) {
				/*
				 * Sort by signal and priority
				 */
				if (o1.wificonfig.priority < o2.wificonfig.priority) 
				{
					return -1;
				}
				else if (o1.wificonfig.priority == o2.wificonfig.priority)
				{
					if(o1.level < o2.level)
						return -1;
					else if (o1.level > o2.level)
						return 1;
					else
						return 0;
					
				}
				else if (o1.wificonfig.priority > o2.wificonfig.priority)
				{
					return 1;
				}
				return 0;					
			}
		}

		/*
		 * Acquire scan results
		 */
		List<ScanResult> scanResults = getWifiManager(context).getScanResults();
		/*
		 * Catch null if scan results fires after wifi disabled or while wifi is
		 * in intermediate state
		 */
		int l_counter = 0;
		do
		{
			if(scanResults !=null) 
			{
				Log.i(TAG, "Scan result is available");    			
				break;
			}
			else
			{				
				try
				{
					Thread.sleep(MILLISEC_BETWEEN_SCAN_RETRIES); 
				}
				catch(Exception e)
				{
					Log.e(TAG,e.getMessage());	
				}
				
				Log.e(TAG, "Attempting to get the scan results. Repeat attempt number " + l_counter + " .");
				scanResults = getWifiManager(context).getScanResults();				
			}
			
			l_counter++;				    						    			
		
		}while(l_counter < NUMBER_OF_SCAN_RETRIES );
		
		
		if (scanResults == null) {
			Log.e(TAG, "No wifi network Available, scan returned null");
			ShowToastFromThread(context,"No wifi network Available, scan returned null, contact administrator",Toast.LENGTH_LONG);
			return NO_NETWORK_IN_SCAN;
		}
		else
		{
			logScanResultAllScanned(context,scanResults);
		}
		
		/*
		 * Known networks from supplicant.
		 */
		List<WifiConfiguration> wifiConfigs =  getWifiManager(context).getConfiguredNetworks();
		
		if ((wifiConfigs == null) || ((wifiConfigs != null) && (wifiConfigs.isEmpty()))){
			Log.e(TAG, "No saved networks, WifiConfiguration returned null");
			ShowToastFromThread(context,"No Saved Networks, not usual ...",Toast.LENGTH_LONG);
			return NO_SAVED_NETWORK;
		}
		else
		{
			logScanResultAllSaved(context,wifiConfigs);
		}
		
		
		//First pass - Attempt to connect to networks that are
		//returned by scan result AND also saved 
		//order by Priority
		//then order by signal strength
		//Connect to one  .... wait for 5 seconds ... try next
		
		int index;
		
		if (knownbysignal == null)
			knownbysignal = new ArrayList<WFConfig>();
		else
			knownbysignal.clear();
		
		for (ScanResult sResult : scanResults) {
			/*
			 * Look for scan result in our known list
			 */
			index = containsSSID_return_int(sResult.SSID, wifiConfigs); 
			if (index > -1) {
				
				//scanned is in saved
				Log.i(TAG, "A scanned network is in the saved list - " + sResult.SSID +"-" + sResult.BSSID + " ; " + "Signal Level:" + sResult.level);
				WifiConfiguration wfResult = getNetworkByNID(context, index);							
				
					/*
					 * Add result to known by signal Using containsSSID to avoid
					 * dupes 
					 */
					if (!containsSSID(sResult.SSID, knownbysignal))
						knownbysignal.add(new WFConfig(sResult,wfResult));
					else {
						//Log.i(TAG, "Found another BSSID with the same SSID "  + sResult.SSID);
						/*
						 * Update signal level (keep maximum)
						 */
						for (WFConfig config : knownbysignal) {
							if (StringUtil.removeQuotes(config.wificonfig.SSID).equals(sResult.SSID)) {
								
								if(config.level <= sResult.level ) //note -ve less
									config.level = sResult.level; // set the one with higher signal level
								else
									Log.i(TAG, "Existing level - " + config.level + " is more than " + "new level " + sResult.level);
							}
						}
					}
				}
		}
		
		if (knownbysignal.isEmpty()) {
			Log.e(TAG, "No saved network available in scan");
			ShowToastFromThread(context,"No Saved Networks available in scan, again not usual ...",Toast.LENGTH_LONG);
			return NO_SAVED_NETWORK_IN_SCAN;
		}
		else
		{
			logKnownBySignal(context);
			//Now order by priority
			//then order by signal level - applicable if same priority
			
			/*
			 * Sort by ScanResult.level which is signal
			 */
			Collections.sort(knownbysignal, new SortByPriorityAndSignal());
			
			Log.i(TAG, "Saved and Available networks Sorted by priority");
			logKnownBySignal(context);
			
			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 
			
			for (WFConfig connectToNetwork : knownbysignal) 
			{
				if (isConnectedToWifi(context)) //connected to wifi
	    		{
					ShowToastFromThread(context,"Wifi Network connected",Toast.LENGTH_SHORT);
	    			Log.i(TAG, "Wifi network is connected");
	    			break;
	    		}
				
				wifiManager.disconnect();
		        wifiManager.enableNetwork(connectToNetwork.wificonfig.networkId, false); //disable others is false - so do not disable others
		        wifiManager.reconnect();
		        
		        Log.i(TAG, "Request to enable " + connectToNetwork.wificonfig.SSID + "  has been sent");
		        ShowToastFromThread(context,"Request to enable " + connectToNetwork.wificonfig.SSID + "  has been sent",Toast.LENGTH_SHORT);
		         
	         	try
				{
					Thread.sleep(MILLISEC_BETWEEN_WIFI_RETRIES); 
				}
				catch(Exception e)
				{
					if((e != null) && (e.getMessage() !=null))
						Log.e(TAG,e.getMessage());	
				}	         		         									
			}						
		}
		
		return DATA_TO_CONNECT_IS_AVAILABLE;
		
	}
	
	private void ConnectToUnsavedOpenNetworks(final Context context)
	{
		/*
		 * Comparator class for sorting results for ScanResult
		 */
		class SortBySignal implements Comparator<ScanResult> {
			@Override
			public int compare(ScanResult o2, ScanResult o1) {
				/*
				 * Sort by signal
				 */				
				if(o1.level < o2.level)
					return -1;
				else if (o1.level > o2.level)
					return 1;
				else
					return 0;
								
			}
		}
			try
			{
					
				List<ScanResult> scanResults = getWifiManager(context).getScanResults();
				
				if (scanResults == null) {
					Log.e(TAG, "No wifi network Available, scan returned null");
					ShowToastFromThread(context,"No wifi network Available, scan returned null, contact administrator",Toast.LENGTH_LONG);
					return;
				}
				else
				{
					
					/*
					 * Sort by ScanResult.level which is signal
					 */
					Collections.sort(scanResults, new SortBySignal());
					
					Log.i(TAG, "Scanned networks sorted by signal levels");
					logScanResultAllScanned(context,scanResults);		
					ShowToastFromThread(context,"Will attempt to connect to available open networks ...",Toast.LENGTH_SHORT);
					
					for (ScanResult aScannedNetwork : scanResults) 
					{
						if (isConnectedToWifi(context)) //connected to wifi
			    		{
							ShowToastFromThread(context,"Wifi Network connected",Toast.LENGTH_SHORT);
			    			Log.i(TAG, "Wifi network is connected");
			    			break;
			    		}
						
						//If the network is not open - we cannot "automatically" connect to it
						//Capabilities Describes the authentication, key management, and encryption schemes supported by the access point.
						//Authentication - security can have the following values:
							//	OPEN - ** not carried in return result
							//	WEP - Wired Equivalent Privacy (WEP) - deprecated
							//	WPA - Wi-Fi Protected Access (WPA)
							//	WPA2 - Wi-Fi Protected Access II (WPA2)   
						//Key management
							//WPA-Personal - Also referred to as WPA-PSK (Pre-shared key) mode
							//WPA-Enterprise - Also referred to as WPA-802.1X mode, and sometimes just WPA (as opposed to WPA-PSK).
						//Encryption protocol
							//		TKIP (Temporal Key Integrity Protocol) - The RC4 stream cipher is used with a 128-bit per-packet key, meaning that it dynamically generates a new key for each packet. Used by WPA.
							//		CCMP -	An AES-based encryption mechanism that is stronger than TKIP. Used by WPA2.
						
						if(!getScanResultSecurity(aScannedNetwork).equals(OPEN))
						{
							Log.i(TAG, aScannedNetwork.SSID + " is a secured network. Cannot connect automatically." );
							ShowToastFromThread(context,"Skipping secured network " + aScannedNetwork.SSID,Toast.LENGTH_SHORT);
							continue;
						}										
						
						//if this SSID is already in the set of saved/configured networks from supplicant then skip that
						//implies we have tried to connect to this SSID
						List<WifiConfiguration> wifiConfigsUpdated =  getWifiManager(context).getConfiguredNetworks();
						
						if (wifiConfigsUpdated == null) {
							//do nothing
						}
						else
						{
							//if SSID present continue with the next network
							if (containsSSID_return_int(aScannedNetwork.SSID, wifiConfigsUpdated) != -1)
							{
								Log.i(TAG, aScannedNetwork.SSID + " is present in the saved networks list. Skipping the step to add this SSID.");
								continue;
							}					
						}
						
						
						//This OPEN SSID is not present in the configured list - add it
						//Create WifiConfiguration object
						WifiConfiguration conf = new WifiConfiguration();
						conf.SSID = "\"" + aScannedNetwork.SSID + "\"";   // Please note the quotes. String should contain ssid in quotes
						conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
						conf.status=WifiConfiguration.Status.ENABLED;	
						conf.priority=1; //all at priority 1, one with best signal gets connected
						getWifiManager(context).addNetwork(conf);
						getWifiManager(context).saveConfiguration(); //persist the current list of configured networks
						
						//GO through the updated list again
						List<WifiConfiguration> wifiConfigsAdded =  getWifiManager(context).getConfiguredNetworks();
						
						if (wifiConfigsAdded == null) {
							//do nothing
						}
						else
						{
							//if SSID present enable it
							if (containsSSID_return_int(aScannedNetwork.SSID, wifiConfigsAdded) != -1)
							{
								Log.i(TAG, "Added the OPEN network " + aScannedNetwork.SSID + " to the saved list.");
								for( WifiConfiguration i : wifiConfigsAdded ) 
								{
								    if(i.SSID != null && i.SSID.equals("\"" + aScannedNetwork.SSID + "\"")) 
								    {
								    	getWifiManager(context).disconnect();
								    	getWifiManager(context).enableNetwork(i.networkId, false);
								    	getWifiManager(context).reconnect(); 
								    	
								    	Log.i(TAG, "Request to enable " + aScannedNetwork.SSID + "  has been sent");
								       try
										{
											Thread.sleep(MILLISEC_BETWEEN_WIFI_RETRIES); 
										}
										catch(Exception e)
										{
											if((e != null) && (e.getMessage() !=null))
												Log.e(TAG,e.getMessage());	
										}
								       ShowToastFromThread(context,"Request to enable " + aScannedNetwork.SSID + "  has been sent",Toast.LENGTH_SHORT);					         				         	
								        break;
								    }
								    continue; // for the other scanned networks
								}
	
							}
							else
							{
								Log.e(TAG, "Could not add the OPEN network " + aScannedNetwork.SSID + " to the saved network list");
							}
						}
						         
					 }
				}
			}
			catch(Exception e)
			{
				if ((e != null) && (e.getMessage()!= null))
					Log.e(TAG, e.getMessage());
			}
	}
	
	private void EnableWifiAlgorithm(Context context)
	{
		if (isConnectedToWifi(context)) //connected to wifi
		{
			ShowToast(context,"Wifi Network connected",Toast.LENGTH_SHORT);
			Log.i(TAG, "Wifi Network connected");
			return;
		}
		
		Log.i(TAG, "Will now call the connect to Access Points function in a seperate thread");
		final Context thread_Context = context;
		
		Thread t = new Thread() {
        public void run() {		        	
        			ConnectToKnownAPs(thread_Context);						       
		        	};
		};
		
    	try
    	{
    		t.start();
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception in starting the thread ConnectToKnownAPs.");		
    		if ((e != null) && (e.getMessage()!= null))
				Log.e(TAG, e.getMessage());			        		
    	}    	
		
		return;					
	}

	/**
	 * Gets the state of Wi-Fi
	 * 
	 * @param context
	 * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
	 */
	private static int getWifiState(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		int wifiState = wifiManager.getWifiState();
		if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
			return STATE_DISABLED;
		} else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
			return STATE_ENABLED;
		} else {
			return STATE_INTERMEDIATE;
		}
	}

	/**
	 * Updates the widget when something changes, or when a button is pushed.
	 * 
	 * @param context
	 */
	public static void updateWidget(Context context) {
		RemoteViews views = buildUpdate(context, -1);
		// Update specific list of appWidgetIds if given,
		// otherwise default to all
		final AppWidgetManager gm = AppWidgetManager.getInstance(context);
		gm.updateAppWidget(THIS_APPWIDGET, views);
	}

	/**
	 * Load image for given widget and build {@link RemoteViews} for it.
	 */
	static RemoteViews buildUpdate(Context context, int appWidgetId) {
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.main);
		views.setOnClickPendingIntent(R.id.btn_wifi,
				getLaunchPendingIntent(context, appWidgetId, BUTTON_WIFI));
		views.setOnClickPendingIntent(R.id.btn_ssid,
				getLaunchPendingIntent(context, appWidgetId, BUTTON_WIFI));
		//Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
		//PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
		//views.setOnClickPendingIntent(R.id.btn_ssid, pi);
		updateButtons(views, context);
		return views;
	}

	/**
	 * Creates PendingIntent to notify the widget of a button click.
	 * 
	 * @param context
	 * @param appWidgetId
	 * @return
	 */
	private static PendingIntent getLaunchPendingIntent(Context context,
			int appWidgetId, int buttonId) {
		Intent launchIntent = new Intent();
		launchIntent.setClass(context, WifiAppWidgetProvider.class);
		launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		launchIntent.setData(Uri.parse("custom:" + buttonId));
		PendingIntent pi = PendingIntent.getBroadcast(context, 0 /*
																 * no
																 * requestCode
																 */,
				launchIntent, 0 /* no flags */);
		return pi;
	}

	/**
	 * Updates the buttons based on the underlying states of Wi-Fi.
	 * 
	 * @param views
	 *            The RemoteViews to update.
	 * @param context
	 *            the Context to get connectivity info from.
	 */
	private static void updateButtons(RemoteViews views, Context context) {
		switch (getWifiState(context)) {
		case STATE_DISABLED:
			changeWidgetToDisabled(views);
			break;
		case STATE_ENABLED:
			if(isConnectedToWifi(context)) //to wifi
			{
				changeWidgetToEnabled(views, context);
			}
			else
				changeWidgetToDisabled(views);				
			break;
		case STATE_INTERMEDIATE:
			changeWidgetToIntermediate(views);
			break;
		}
	}

	/**
	 * SSID of the Wi-Fi network we're connected to.
	 * 
	 * pre-condition: isConnected(context)
	 * 
	 * @param context
	 *            the Context to get connection info from.
	 * @return the SSID of the Wi-Fi network we're connected to
	 */
	private static String getSSID(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		return wifiManager.getConnectionInfo().getSSID();
	}
	
	/**
	 * Display network SSID
	 * 
	 * @param views
	 *            the RemoteViews to update
	 * @param ssid
	 *            the network SSID
	 */
	private static void setSSID(RemoteViews views, String ssid) {
		views.setTextViewText(R.id.ssid, ssid);		
	}

	/**
	 * Change widget to disable state.
	 * 
	 * @param views
	 *            the RemoteViews to update.
	 */
	private static void changeWidgetToDisabled(RemoteViews views) {
		
		//views.setImageViewResource(R.id.img_wifi, R.drawable.wifi_off);
		//views.setViewVisibility(R.id.divider, View.GONE);
		//views.setViewVisibility(R.id.btn_ssid, View.GONE);
		
		views.setViewVisibility(R.id.img_wifi, View.GONE);
		views.setViewVisibility(R.id.divider, View.GONE);
		views.setViewVisibility(R.id.btn_wifi, View.GONE);
		views.setViewVisibility(R.id.btn_ssid, View.VISIBLE);
		views.setTextViewText(R.id.ssid, "Touch here to Enable Wifi Network");		
		WifiAppWidgetProvider.previousState = STATE_DISABLED;
		
	}

	/**
	 * Change widget to enabled (connected or disconnected) state.
	 * 
	 * @param views
	 *            the RemoteViews to update.
	 * @param context
	 *            the Context to get connectivity info.
	 */
	private static void changeWidgetToEnabled(RemoteViews views, Context context) {
		views.setImageViewResource(R.id.img_wifi, R.drawable.wifi_on);
		if (isConnectedToWifi(context)) {
			setSSID(views, getSSID(context));
		} else {
			views.setTextViewText(R.id.ssid, context.getText(R.string.settings));
		}
		views.setViewVisibility(R.id.btn_wifi, View.VISIBLE);
		views.setViewVisibility(R.id.img_wifi, View.VISIBLE);
		views.setViewVisibility(R.id.divider, View.VISIBLE);
		views.setViewVisibility(R.id.btn_ssid, View.VISIBLE);
		WifiAppWidgetProvider.previousState = STATE_ENABLED;
	}

	/**
	 * Change widget to intermediate state.
	 * 
	 * @param views
	 *            the RemoteViews to update.
	 */
	private static void changeWidgetToIntermediate(RemoteViews views) {
		if (WifiAppWidgetProvider.previousState == STATE_DISABLED) {
			views.setImageViewResource(R.id.img_wifi, R.drawable.wifi_on);
		} else {
			views.setImageViewResource(R.id.img_wifi, R.drawable.wifi_off);
		}
	}
}
