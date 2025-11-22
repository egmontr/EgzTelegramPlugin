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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.drinkless.tdlib.TdApi;

import java.util.Hashtable;

import de.egi.geofence.geozone.plugin.tgm.preferences.PreferenceKeys;
import de.egi.geofence.geozone.plugin.tgm.utils.NotificationUtil;
import de.egi.geofence.geozone.plugin.tgm.utils.Utils;

/**
 * This {@code WakefulBroadcastReceiver} takes care of creating and managing a
 * partial wake lock for your app. It passes off the work of processing the plugin
 * events to an {@code IntentService}, while ensuring that the device does not
 * go back to sleep in the transition. The {@code IntentService} calls
 * {@code TgmBroadcastReceiverPlugin.completeWakefulIntent()} when it is ready to
 * release the wake lock.
 */

public class TgmBroadcastReceiverPlugin extends BroadcastReceiver {
    private static final String ACTION_EGIGEOZONE_EVENT = "de.egi.geofence.geozone.plugin.EVENT";
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
    public void onReceive(Context context, Intent intent) {
        // Explicitly specify that TgmPluginIntentService will handle the intent.
//        ComponentName comp = new ComponentName(context.getPackageName(), TgmPluginIntentService.class.getName());
//        // Start the service, keeping the device awake while it is launching.
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(intent.setComponent(comp));
//        } else {
//            context.startService(intent.setComponent(comp));
//        }

//        startWakefulService(context, (intent.setComponent(comp)));

        tpa = ((TgmPluginApplication)context.getApplicationContext());
        tpa.info("TgmBroadcastReceiverPlugin: onReceive");
        String action = intent.getAction();
        if (ACTION_EGIGEOZONE_EVENT.equals(action)) {
            // Call Method to perform EgiGeoZone events
            doEvent(intent, context);
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
//        TgmBroadcastReceiverPlugin.completeWakefulIntent(intent);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void doEvent(Intent intent, Context context){
        tpa.info("TgmBroadcastReceiverPlugin: doEvent");
        SharedPreferences mPrefs = context.getSharedPreferences(TgmPluginMain.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        chatBotId = mPrefs.getLong(PreferenceKeys.BOT_ID, 0);

        if (chatBotId == 0){
            int notifyId = 201;
            Intent openIntent = new Intent(context, TgmPluginMain.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(context, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            }else{
                pendingIntent = PendingIntent.getActivity(context, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            NotificationUtil.notify(context,
                    notifyId,
                    pendingIntent,
                    "Error: EgzTgmPlugin",
                    context.getString(R.string.wrong_bot), "",
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

        String zoneName = intent.getStringExtra("zone_name");

        // Do not send message, if not the right zone
        if(!ht.containsKey(zoneName)) {
            tpa.info("TgmBroadcastReceiverPlugin: Do not handle this zone: " + zoneName);
            return;
        }

        tpa.info("TgmBroadcastReceiverPlugin: Send message for zone: " + zoneName);

        String transition = intent.getStringExtra("transition");
        String transText;
        if (transition.equals("1")){
            transText = context.getString(R.string.geofence_transition_entered);
        }else{
            transText = context.getString(R.string.geofence_transition_exited);
        }
        String latitude = intent.getStringExtra("latitude");
        String longitude = intent.getStringExtra("longitude");
        String deviceId = intent.getStringExtra("device_id");
        String date = intent.getStringExtra("date_device");

        String realLatitude = intent.getStringExtra("realLatitude");
        String realLongitude = intent.getStringExtra("realLongitude");
        String location_accuracy = intent.getStringExtra("location_accuracy");

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

        tpa.info("TgmBroadcastReceiverPlugin: chatBotId and command: " + chatBotId + " :: " + command);

        // Save command to globals
        GlobalSingleton.getInstance().setCommand(command);
        // Get AuthState and then Broadcast to send message
//        tpa.sendFunction(new TdApi.GetAuthorizationState(), tpa);

        TdApi.FormattedText formattedText = new TdApi.FormattedText();
        formattedText.text = command;
        tpa.sendMessage(chatBotId, new TdApi.InputMessageText( formattedText, null, true), tpa);
        GlobalSingleton.getInstance().setCommand("");
    }
}

