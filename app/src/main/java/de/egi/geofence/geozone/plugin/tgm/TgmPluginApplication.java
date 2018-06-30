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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.drinkless.td.libcore.telegram.Client;
import org.drinkless.td.libcore.telegram.TG;
import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;

import de.egi.geofence.geozone.plugin.tgm.preferences.PreferenceKeys;
import de.egi.geofence.geozone.plugin.tgm.utils.NotificationUtil;
import de.mindpipe.android.logging.log4j.LogConfigurator;

// https://vk.com/topic-55882680_31509731

public class TgmPluginApplication extends Application implements Client.ResultHandler{
    private Client client;
    private final static String LOG_TAG = "TgmPluginApplication";
    private boolean boolGetAuthState = true;
    final static LogConfigurator logConfigurator = new LogConfigurator();
    private Logger log;
//    private final static String level = "ERROR";
    private static final String SHARED_PREFERENCE_NAME = TgmPluginMain.class.getSimpleName();
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCreate() {
        super.onCreate();

        mPrefs = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE);
        editor = mPrefs.edit();
        editor.apply();

        String level = mPrefs.getString(PreferenceKeys.LOG_LEVEL, "ERROR");
        if (level == null || level.equalsIgnoreCase("")){
            level = Level.ERROR.toString();
        }
        log = Logger.getLogger(TgmPluginApplication.class);
        if (logConfigurator.getFileName().equalsIgnoreCase("android-log4j.log")){
            logConfigurator.setFileName(Environment.getExternalStorageDirectory() + File.separator + "tgmplugin" + File.separator + "tgmplugin.log");
            logConfigurator.setUseFileAppender(true);
            logConfigurator.setRootLevel(Level.toLevel(level));
            // Set log level of a specific logger
            logConfigurator.setLevel("de.egi.geofence.geozone", Level.toLevel(level));
            try {
                logConfigurator.configure();
                info("Logger set!");
                Log.i("", "Logger set!");
            } catch (Exception e) {
                // Nothing to do!
            }
        }
        startTelegramApi();
    }

    private void startTelegramApi() {
        info("startTelegramApi");

        File f = null;
        String path;
        try {
            final PackageManager packageManager = getPackageManager();
            f = new File(packageManager
                    .getPackageInfo(getPackageName(), 0)
                    .applicationInfo.dataDir + "/egztgm/");
            if (!f.exists()) {
                f.mkdir();
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

//        boolean tgInit= mPrefs.getBoolean(PreferenceKeys.TG_INIT, false);
//        if (!tgInit) {
            path = f.getAbsolutePath();
            TG.setUpdatesHandler(this);
            TG.setDir(path);

//            editor.putBoolean(PreferenceKeys.TG_INIT, true);
//            editor.apply();
//        }

        if (client == null) {
            client = TG.getClientInstance();
        }
    }


    @Override
    public void onResult(TdApi.TLObject object) {
        info("onResult :" + object.toString());

        if (object instanceof TdApi.UpdateNewMessage) {
            TdApi.UpdateNewMessage newMessage = (TdApi.UpdateNewMessage) object;
            long chatBotId = mPrefs.getLong(PreferenceKeys.BOT_ID, 0);
            if (newMessage.message.senderUserId == chatBotId) {
                TdApi.MessageText mt = (TdApi.MessageText) newMessage.message.content;
                String msgResp = mt.text;
                int notifyId = 200;
                Intent openIntent = new Intent(this, TgmPluginMain.class);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                // Show notification only if requested
                boolean nonotify = mPrefs.getBoolean(PreferenceKeys.NONOTIFYRESPONSE, false);
                if (!nonotify) {
                    NotificationUtil.notify(this,
                            notifyId,
                            pendingIntent,
                            "EgzTgmPlugin",
                            msgResp, "",
                            false, false, R.drawable.ic_dove_2);
                }
            }
        } else if (object instanceof TdApi.User){
            TdApi.User user = (TdApi.User) object;
            if (mPrefs.getString(PreferenceKeys.PHONE_NUMBER, "").contains(user.phoneNumber)){
                info("save my_id (user.id): " + user.id);
                editor.putInt(PreferenceKeys.MY_ID, user.id);
                editor.apply();
            }
        } else if (object instanceof TdApi.UpdateUser) {
            TdApi.UpdateUser updateUser = (TdApi.UpdateUser) object;
            if (updateUser.user.username.equals(mPrefs.getString(PreferenceKeys.BOT_NAME, ""))){
                info("updateUser :: set BotChatId to : " + updateUser.user.id);
                editor.putLong(PreferenceKeys.BOT_ID, updateUser.user.id);
                editor.apply();
            }
        } else if (object instanceof TdApi.AuthStateWaitPhoneNumber){
            Intent intent = new Intent(this, PhoneNumber.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }else if (object instanceof TdApi.AuthStateWaitCode) {
            Intent intent = new Intent(this, Code.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }else if (object instanceof TdApi.UpdateOption) {
            TdApi.UpdateOption uo = (TdApi.UpdateOption) object;
            if (uo.name.equals("connection_state") && uo.value.equals("Ready")){
                // Broadcast AuthState OK to Main for green semaphore
                final Intent intent = new Intent("de.egi.geofence.geozone.plugin.tgm.AUTHSTATEOK");
                sendBroadcast(intent);

                info("update option :: login state: true");
                editor.putBoolean(PreferenceKeys.LOGGEDSTATE, true);
                editor.apply();
            }
            if (uo.name.equals("my_id")){
                // save my_id
                TdApi.OptionInteger ov = (TdApi.OptionInteger) uo.value;

                info("save my_id (update option): " + ov.value);
                editor.putInt(PreferenceKeys.MY_ID, ov.value);
                editor.apply();
            }
        }else if (object instanceof TdApi.Chat) {
            TdApi.Chat chat = (TdApi.Chat) object;
            if (chat.type instanceof TdApi.ChatInfo){
                TdApi.PrivateChatInfo ci = (TdApi.PrivateChatInfo) chat.type;
                if (ci.user.username.equals(mPrefs.getString(PreferenceKeys.BOT_NAME, ""))) {
                    info("chat :: set BotChatId to : " + chat.id);
                    editor.putLong(PreferenceKeys.BOT_ID, chat.id);
                    editor.apply();

//                    sendFunction(new TdApi.SendBotStartMessage(mPrefs.getInt(PreferenceKeys.BOT_ID, 0), mPrefs.getInt(PreferenceKeys.MY_ID, 0), "" + System.currentTimeMillis()), this);

                }
            }
        }else if (object instanceof TdApi.UpdateChat) {
            info("UpdateChat");
            TdApi.UpdateChat updateChat = (TdApi.UpdateChat) object;
            if (updateChat.chat.type instanceof TdApi.ChatInfo){
                TdApi.PrivateChatInfo ci = (TdApi.PrivateChatInfo) updateChat.chat.type;
                if (ci.user.username.equals(mPrefs.getString(PreferenceKeys.BOT_NAME, ""))) {
                    info("update chat :: set BotChatId to : " + updateChat.chat.id);
                    editor.putLong(PreferenceKeys.BOT_ID, updateChat.chat.id);
                    editor.apply();
                }
            }
        }else if (object instanceof TdApi.AuthStateOk) {
            // Broadcast AuthState OK to Main for green semaphore
            // 1.
            final Intent intent = new Intent("de.egi.geofence.geozone.plugin.tgm.AUTHSTATEOK");
            sendBroadcast(intent);
            if (mPrefs.getLong(PreferenceKeys.BOT_ID, 0) == 0) {
                sendFunction(new TdApi.GetChats(0, 0, 2000), this);
            }else{
                sendFunction(new TdApi.GetChats(0, mPrefs.getLong(PreferenceKeys.BOT_ID, 0) , 2000), this);
            }
            // Logged in
            info("auth state OK :: login state: true");
            editor.putBoolean(PreferenceKeys.LOGGEDSTATE, true);
            editor.apply();

            // Search for Bot
//            sendFunction(new TdApi.SearchPublicChat(mPrefs.getString(PreferenceKeys.BOT_NAME, "")), this);

            info("AuthStateOk");
        }else if (object instanceof TdApi.Chats) {
            // 2.
            TdApi.Chats chats = (TdApi.Chats) object;
            sendFunction(new TdApi.OpenChat(mPrefs.getLong(PreferenceKeys.BOT_ID, 0)), this);
        }else if (object instanceof TdApi.Ok) {
            // 3.
            TdApi.Ok ok = (TdApi.Ok) object;
            // Broadcast send message
            final Intent intent = new Intent("de.egi.geofence.geozone.plugin.tgm.SENDMESSAGE");
            sendBroadcast(intent);

        }else if (object instanceof TdApi.AuthStateLoggingOut) {
            // Broadcast AuthState OK to Main for red semaphore
            final Intent intent = new Intent("de.egi.geofence.geozone.plugin.tgm.AUTHSTATENOK");
            sendBroadcast(intent);

            info("auth state OK :: login state: false");
            editor.putBoolean(PreferenceKeys.LOGGEDSTATE, false);
            editor.apply();
            info("AuthStateLoggingOut");
        }else if (object instanceof TdApi.Error) {
            TdApi.Error error = (TdApi.Error) object;
            if (error.message.equals("Unauthorized")) {
                error("Do Error: Unauthorized");
                int notifyId = 200;
                Intent openIntent = new Intent(this, TgmPluginMain.class);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationUtil.notify(this,
                        notifyId,
                        pendingIntent,
                        "Login: EgzTgmPlugin",
                        getString(R.string.please_login), "",
                        false, false, R.drawable.ic_dove_2);
            }else{
                error("Do Error: " + error.message);
                int notifyId = 202;
                Intent openIntent = new Intent(this, TgmPluginMain.class);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationUtil.notify(this,
                        notifyId,
                        pendingIntent,
                        "Error: EgzTgmPlugin",
                        error.message, "",
                        false, false, R.drawable.ic_dove_2);

            }
        }
        else{
            if (boolGetAuthState) {
                sendFunction(new TdApi.GetAuthState(), this);
                boolGetAuthState = false;
                info("do GetAuthState");
            }
        }
    }

    private Client getClient() {
        if (client == null) {
            try {
                File f = new File(getPackageManager()
                        .getPackageInfo(getPackageName(), 0)
                        .applicationInfo.dataDir + "/egztgm/");
                if (f.exists()) {
                    final String absolutePath = f.getAbsolutePath();
                    TG.setDir(absolutePath);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            client = TG.getClientInstance();
        }
        return client;
    }

    public void sendFunction(TdApi.TLFunction func, Client.ResultHandler handler) {
        info("sendFunction: " + func.toString());
        client = getClient();
        client.send(func, handler);
    }

    public void sendMessage(long chatId, TdApi.InputMessageContent inputMessageContent, Client.ResultHandler handler) {
        client = getClient();
        final TdApi.SendMessage function = new TdApi.SendMessage(chatId, 0, true, true, null, inputMessageContent);
        client.send(function, handler);
    }

//    private void debug(String debug){
//        if (log == null || !checkWritePermission()) return;
//        log.debug(debug);
//    }

    public void info(String info){
        if (log == null || !checkWritePermission()) return;
        log.info(info);
    }

    private void error(String error){
        if (log == null || !checkWritePermission()) return;
        log.error(error);
    }

    // Check for all needed permissions
    public boolean checkWritePermission(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result != PackageManager.PERMISSION_GRANTED){
            return false;
        }
        return true;
    }
}
