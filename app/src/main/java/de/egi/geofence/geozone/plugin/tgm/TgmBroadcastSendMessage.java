package de.egi.geofence.geozone.plugin.tgm;

import android.content.Context;
import android.content.Intent;
import androidx.legacy.content.WakefulBroadcastReceiver;

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
                    TdApi.FormattedText formattedText = new TdApi.FormattedText();
                    formattedText.text = c;
                    tpa.sendMessage(cb, new TdApi.InputMessageText( formattedText, true, true), tpa);
                        GlobalSingleton.getInstance().setCommand("");
                }
            }
        }
        completeWakefulIntent(intent);
    }
}