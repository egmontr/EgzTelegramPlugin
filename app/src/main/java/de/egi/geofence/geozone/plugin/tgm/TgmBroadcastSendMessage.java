package de.egi.geofence.geozone.plugin.tgm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import org.drinkless.td.libcore.telegram.TdApi;

public class TgmBroadcastSendMessage extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (GlobalSingleton.getInstance() != null) {
        String c = GlobalSingleton.getInstance().getCommand();
        long cb = GlobalSingleton.getInstance().getChatBotId();

        if (c != null && !c.equals("")) {
            if (cb == 0) {
                // nothing
            } else {
                TgmPluginApplication tpa = ((TgmPluginApplication) context.getApplicationContext());
                tpa.sendMessage(cb, new TdApi.InputMessageText(c, true, true, null, null), tpa);
                    GlobalSingleton.getInstance().setCommand("");
                }
            }
        }
        completeWakefulIntent(intent);
    }
}