package org.androidappdev.wifiwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class ShowDialog extends Activity {

    private static final int OK_CANCEL_DIALOG = 0;
    private static final int OK_DIALOG = 1;    
    
    static final String TAG = "WifiAppWidgetProvider";
    
    private String message = "";
    private String title = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int param_messageId = 0;
       Bundle extras = getIntent().getExtras();
		if(extras !=null) {
			param_messageId = extras.getInt("MESSAGEID");		    
		}
		
		
		switch(param_messageId)
		{
			case WifiAppWidgetProvider.NO_NETWORK_IN_SCAN: 
				//A network could not be detected at this place. Please contact your administrator."
				title = "A wifi network could not be detected at this place.";
				message = "This may be a temporary problem. If the problem persists, please contact your administrator.";
				showDialog(OK_DIALOG);
	        		break;
			case WifiAppWidgetProvider.NO_SAVED_NETWORK:
				//There are no saved networks on this device. Please contact your administrator."
				title = "There are no saved wifi networks on this device.";
				message = "Please contact your administrator.";
				showDialog(OK_DIALOG);
			        break;
			case WifiAppWidgetProvider.NO_SAVED_NETWORK_IN_SCAN:
				//There are networks available but they not saved on this device. Please contact your administrator."
				title = "There are wifi networks available but those are not saved on this device.";
				message = "This may be a temporary problem. If the problem persists, please contact your administrator.";
				showDialog(OK_DIALOG);
		        	break;		
			case WifiAppWidgetProvider.DATA_TO_CONNECT_IS_AVAILABLE:
			default:
				//everything expected is OK and we could be stuck				
				// display the dialog
				title = "There seems to be a problem connecting to a saved wifi network.";
				message = "A reboot of the device may help. Do you want to reboot now?";
		        showDialog(OK_CANCEL_DIALOG);
		        break;				
		}       
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case OK_DIALOG:
        	
        	return new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // finish the activity
                    finish();
                }
            })
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                	finish();                   
                }
            })
            .create(); 
        	
        case OK_CANCEL_DIALOG:
            return new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // finish the activity
                    finish();
                }
            })
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                	RebootDevice();                   
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // finish the activity
                    finish();
                }
            }).create();
            
        }

        return null;
    }
    
	private void RebootDevice()
	{
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		pm.reboot(null);		
	}

   
}