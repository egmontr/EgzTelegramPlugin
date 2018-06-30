/*
* Copyright 2017 Egmont R. (egmontr@gmail.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package de.egi.geofence.geozone.plugin.tgm;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;

import org.drinkless.td.libcore.telegram.TdApi;

import java.util.Hashtable;

import de.egi.geofence.geozone.plugin.tgm.preferences.PreferenceKeys;
import de.egi.geofence.geozone.plugin.tgm.utils.NotificationUtil;
import de.egi.geofence.geozone.plugin.tgm.utils.Utils;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code TgmBroadcastReceiverPlugin} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
@SuppressWarnings("unused")
public class TgmPluginIntentService extends IntentService {

	private static final String ACTION_EGIGEOZONE_EVENT = "de.egi.geofence.geozone.plugin.EVENT";
    private String transition;
    private String transText;
    private String zoneName;
    private String latitude;
    private String longitude;
    private String realLatitude;
    private String realLongitude;
    private String location_accuracy;
    private String deviceId;
    private String date;
    private String notificationText = null;
    public TgmPluginIntentService() {
        super("TgmPluginIntentService");
    }
    private TgmPluginApplication tpa;
    public static long chatBotId;

    public static String command = "";

    public static final String ZONE 		= "${zone}";
    public static final String TRANSITION 	= "${transition}";
    public static final String TRANSITIONTYPE = "${transitionType}";
    public static final String LAT 			= "${latitude}";
    public static final String LNG          = "${longitude}";
    public static final String REALLAT 		= "${realLatitude}";
    public static final String REALLNG      = "${realLongitude}";
    public static final String RADIUS		= "${radius}";
    public static final String DEVICEID	 	= "${deviceId}";
    public static final String ANDROIDID	= "${androidId}";
    public static final String ACCURACY 	= "${accuracy}";

    public static final String DATE 		= "${date}"; // Date as ISO
    public static final String LOCALDATE    = "${localDate}"; // local device date
    public static final String LOCATIONDATE = "${locationDate}"; // location date as ISO
    public static final String LOCALLOCATIONDATE = "${localLocationDate}"; // local location date from device

    @Override
    protected void onHandleIntent(Intent intent) {
        tpa = ((TgmPluginApplication)getApplication());
        tpa.info("TgmPluginIntentService: onHandleIntent");
        String action = intent.getAction();
		if (ACTION_EGIGEOZONE_EVENT.equals(action)) {
			// Call Method to perform EgiGeoZone events
			doEvent(intent);
		}
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        TgmBroadcastReceiverPlugin.completeWakefulIntent(intent);
    }

	private void doEvent(Intent intent){
        tpa.info("TgmPluginIntentService: doEvent");
        SharedPreferences mPrefs = getSharedPreferences(TgmPluginMain.SHARED_PREFERENCE_NAME, MODE_PRIVATE);
        chatBotId = mPrefs.getLong(PreferenceKeys.BOT_ID, 0);

        if (chatBotId == 0){
            int notifyId = 201;
            Intent openIntent = new Intent(this, TgmPluginMain.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationUtil.notify(this,
                    notifyId,
                    pendingIntent,
                    "Error: EgzTgmPlugin",
                    getString(R.string.wrong_bot), "",
                    false, false, R.drawable.ic_dove_white_1);

            return;
        }

        GlobalSingleton.getInstance().setChatBotId(chatBotId);

        // Transmit event only if activated
        boolean sw = mPrefs.getBoolean(PreferenceKeys.SWITCH, false);
        if (!sw) return;

        Hashtable<String, String> ht = new Hashtable<>();

        ht.put(mPrefs.getString(PreferenceKeys.Z1, ""), mPrefs.getString(PreferenceKeys.C1, ""));
        ht.put(mPrefs.getString(PreferenceKeys.Z2, ""), mPrefs.getString(PreferenceKeys.C2, ""));
        ht.put(mPrefs.getString(PreferenceKeys.Z3, ""), mPrefs.getString(PreferenceKeys.C3, ""));
        ht.put(mPrefs.getString(PreferenceKeys.Z4, ""), mPrefs.getString(PreferenceKeys.C4, ""));
        ht.put(mPrefs.getString(PreferenceKeys.Z5, ""), mPrefs.getString(PreferenceKeys.C5, ""));
        ht.put(mPrefs.getString(PreferenceKeys.Z6, ""), mPrefs.getString(PreferenceKeys.C6, ""));

        zoneName = intent.getStringExtra("zone_name");

        // Do not send message, if not the right zone
        if(!ht.containsKey(zoneName)) {
            tpa.info("TgmPluginIntentService: Do not handle this zone: " + zoneName);
            return;
        }

        tpa.info("TgmPluginIntentService: Send message for zone: " + zoneName);

        transition = intent.getStringExtra("transition");
        transText = "";
        if (transition.equals("1")){
            transText = getString(R.string.geofence_transition_entered);
        }else{
            transText = getString(R.string.geofence_transition_exited);
        }
		latitude = intent.getStringExtra("latitude");
		longitude = intent.getStringExtra("longitude");
		deviceId = intent.getStringExtra("device_id");
		date = intent.getStringExtra("date_device");

        realLatitude = intent.getStringExtra("realLatitude");
        realLongitude = intent.getStringExtra("realLongitude");
        location_accuracy = intent.getStringExtra("location_accuracy");

        command = ht.get(zoneName);
        command = Utils.replaceVar(command, ZONE, zoneName);
        command = Utils.replaceVar(command, TRANSITION, transText);
        command = Utils.replaceVar(command, TRANSITIONTYPE, transition);
        command = Utils.replaceVar(command, LAT, latitude);
        command = Utils.replaceVar(command, LNG, longitude);
        command = Utils.replaceVar(command, REALLAT, realLatitude);
        command = Utils.replaceVar(command, REALLNG, realLongitude);
        command = Utils.replaceVar(command, ACCURACY, location_accuracy);
        command = Utils.replaceVar(command, DEVICEID, deviceId);
        command = Utils.replaceVar(command, DATE, date);

        tpa.info("TgmPluginIntentService: chatBotId and command: " + chatBotId + " :: " + command);
        tpa.info("TgmPluginIntentService: GetAuthState" + chatBotId);

        // Save command to globals
        GlobalSingleton.getInstance().setCommand(command);
        // Get AuthState and den Broadcast to send message
        tpa.sendFunction(new TdApi.GetAuthState(), tpa);
	}
}















