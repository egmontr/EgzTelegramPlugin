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
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.drinkless.td.libcore.telegram.Client;
import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;

import de.egi.geofence.geozone.plugin.tgm.preferences.PreferenceKeys;
import de.egi.geofence.geozone.plugin.tgm.utils.NotificationUtil;
import de.mindpipe.android.logging.log4j.LogConfigurator;

// https://vk.com/topic-55882680_31509731

public class TgmPluginApplication extends Application implements Client.ResultHandler{

    private static Client client;
    private final static String LOG_TAG = "TgmPluginApplication";
    private boolean boolGetAuthState = true;
    private boolean boolSendNow = false;
    final static LogConfigurator logConfigurator = new LogConfigurator();
    private Logger log;
    private static final String SHARED_PREFERENCE_NAME = TgmPluginMain.class.getSimpleName();
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor editor;

    // #####
    private static final String newLine = System.getProperty("line.separator");

    private static TdApi.AuthorizationState authorizationState = null;

    private static void print(String str) {
        System.out.println("");
        System.out.println(str);
    }

    public static void setClient(Client client) {
        TgmPluginApplication.client = client;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCreate() {
        super.onCreate();

        mPrefs = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE);
        editor = mPrefs.edit();
        editor.apply();

        String level = mPrefs.getString(PreferenceKeys.LOG_LEVEL, "ERROR");
        if (level.equalsIgnoreCase("")){
            level = Level.ERROR.toString();
        }
        log = Logger.getLogger(TgmPluginApplication.class);
        if (logConfigurator.getFileName().equalsIgnoreCase("android-log4j.log")){
            logConfigurator.setFileName(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + File.separator + "tgmplugin" + File.separator + "tgmplugin.log");
            logConfigurator.setUseFileAppender(true);
            logConfigurator.setRootLevel(Level.toLevel(level));
            // Set log level of a specific logger
            logConfigurator.setLevel("de.egi.geofence.geozone.plugin.tgm", Level.toLevel(level));
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

        File f;
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
        if (client == null) {
            client = Client.create(this, null, null);
        }
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    public void onResult(TdApi.Object object) {
        info("onResult :" + object.toString());

        if (object instanceof TdApi.UpdateNewMessage) {
            TdApi.UpdateNewMessage newMessage = (TdApi.UpdateNewMessage) object;
            long chatBotId = mPrefs.getLong(PreferenceKeys.BOT_ID, 0);
            if (newMessage.message.chatId == chatBotId) {
                TdApi.MessageText mt = (TdApi.MessageText) newMessage.message.content;
                TdApi.FormattedText msgResp = mt.text;
                int notifyId = 200;
                Intent openIntent = new Intent(this, TgmPluginMain.class);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                }else{
                    pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }

                // Show notification only if requested
                boolean nonotify = mPrefs.getBoolean(PreferenceKeys.NONOTIFYRESPONSE, false);
                if (!nonotify) {
                    NotificationUtil.notify(this,
                            notifyId,
                            pendingIntent,
                            "EgzTgmPlugin",
                            msgResp.text, "",
                            false, false, R.drawable.ic_dove_2);
                }
            }
        // New
        } else if (object instanceof TdApi.AuthorizationStateReady) {
            // Broadcast AuthState OK to Main for green semaphore
            // 1.
            final Intent intent = new Intent("de.egi.geofence.geozone.plugin.tgm.AUTHSTATEOK");
            sendBroadcast(intent);
            sendFunction(new TdApi.GetChats(null,2000), this);
            // Logged in
            info("auth state OK :: login state: true");
            editor.putBoolean(PreferenceKeys.LOGGEDSTATE, true);
            editor.apply();

            // Search for Bot
//            sendFunction(new TdApi.SearchPublicChat(mPrefs.getString(PreferenceKeys.BOT_NAME, "")), this);

            info("AuthStateOk");

        } else if (object instanceof TdApi.UpdateAuthorizationState) {
            onAuthorizationStateUpdated(((TdApi.UpdateAuthorizationState) object).authorizationState);
        } else if (object instanceof TdApi.User){
            TdApi.User user = (TdApi.User) object;
            if (mPrefs.getString(PreferenceKeys.PHONE_NUMBER, "").contains(user.phoneNumber)){
                info("save my_id (user.id): " + user.id);
                editor.putInt(PreferenceKeys.MY_ID, (int) user.id);
                editor.apply();
            }
        } else if (object instanceof TdApi.UpdateUser) {
            TdApi.UpdateUser updateUser = (TdApi.UpdateUser) object;
            if (updateUser.user.username.equals(mPrefs.getString(PreferenceKeys.BOT_NAME, ""))){
                info("updateUser :: set BotChatId to : " + updateUser.user.id);
                editor.putLong(PreferenceKeys.BOT_ID, updateUser.user.id);
                editor.apply();
            }
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
                TdApi.OptionValue ov = uo.value;

                info("save my_id (update option): " + ov.toString()); // @new
                editor.putInt(PreferenceKeys.MY_ID, Integer.parseInt(ov.toString())); // @new
                editor.apply();
            }
        }else if (object instanceof TdApi.Chat) {
            TdApi.Chat chat = (TdApi.Chat) object;
            // @new
            if (chat.type != null){
                TdApi.ChatTypePrivate ci = (TdApi.ChatTypePrivate) chat.type;
//                if (ci.userId.equals(mPrefs.getString(PreferenceKeys.BOT_NAME, ""))) {
//                    info("chat :: set BotChatId to : " + chat.id);
//                    editor.putLong(PreferenceKeys.BOT_ID, chat.id);
//                    editor.apply();
//
////                    sendFunction(new TdApi.SendBotStartMessage(mPrefs.getInt(PreferenceKeys.BOT_ID, 0), mPrefs.getInt(PreferenceKeys.MY_ID, 0), "" + System.currentTimeMillis()), this);
//
//                }
            }
        }else if (object instanceof TdApi.Chats) {
            // 2.
            TdApi.Chats chats = (TdApi.Chats) object;
            sendFunction(new TdApi.OpenChat(mPrefs.getLong(PreferenceKeys.BOT_ID, 0)), this);
            boolSendNow = true;
        }else if (object instanceof TdApi.Ok) {
            // 3.
            TdApi.Ok ok = (TdApi.Ok) object;
            // Broadcast send message
//            Intent intent = new Intent();
//            intent.setAction("de.egi.geofence.geozone.plugin.tgm.SENDMESSAGE");
//            getApplicationContext().sendBroadcast(intent);

            if (boolSendNow && GlobalSingleton.getInstance() != null && !GlobalSingleton.getInstance().getCommand().isEmpty()){
                boolSendNow = false;
                String c = GlobalSingleton.getInstance().getCommand();
                long cb = GlobalSingleton.getInstance().getChatBotId();

                if (c != null && !c.equals("")) {
                    if (cb == 0) {
                        // nothing
                    } else {
                        TgmPluginApplication tpa = ((TgmPluginApplication) getApplicationContext());
                        TdApi.FormattedText formattedText = new TdApi.FormattedText();
                        formattedText.text = c;
                        tpa.sendMessage(cb, new TdApi.InputMessageText( formattedText, true, true), tpa);
                        GlobalSingleton.getInstance().setCommand("");
                    }
                }
            }


        }else if (object instanceof TdApi.Error) {
            TdApi.Error error = (TdApi.Error) object;
            if (error.message.equals("Unauthorized")) {
                error("Do Error: Unauthorized");
                int notifyId = 200;
                Intent openIntent = new Intent(this, TgmPluginMain.class);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                }else{
                    pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
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
                PendingIntent pendingIntent;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                }else{
                    pendingIntent = PendingIntent.getActivity(this, notifyId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
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
                sendFunction(new TdApi.GetAuthorizationState(), this);
                boolGetAuthState = false;
                info("do GetAuthState");
            }
        }
    }

//    private Client getClient() {
//        if (client == null) {
//            try {
//                File f = new File(getPackageManager()
//                        .getPackageInfo(getPackageName(), 0)
//                        .applicationInfo.dataDir + "/egztgm/");
//                if (f.exists()) {
//                    final String absolutePath = f.getAbsolutePath();
////                    TG.setDir(absolutePath);
//                }
//            } catch (PackageManager.NameNotFoundException e) {
//                e.printStackTrace();
//            }
//
////            client = TG.getClientInstance();
//        }
//        return client;
//    }

    public void sendFunction(TdApi.Function func, Client.ResultHandler handler) {
        info("sendFunction: " + func.toString());
        client.send(func, handler);
    }

    public void sendMessage(long chatId, TdApi.InputMessageContent inputMessageContent, Client.ResultHandler handler) {
        TdApi.MessageSendOptions sendMessageOptions = new TdApi.MessageSendOptions(true, true, null);
        final TdApi.SendMessage function = new TdApi.SendMessage(chatId, 0, 0, sendMessageOptions, null, inputMessageContent);
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
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
        if (authorizationState != null) {
            TgmPluginApplication.authorizationState = authorizationState;
        }
        switch (TgmPluginApplication.authorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
                TdApi.TdlibParameters parameters = new TdApi.TdlibParameters();
                parameters.databaseDirectory = getApplicationContext().getFilesDir().getAbsolutePath() + File.separator + "tdlib";
                parameters.useMessageDatabase = true;
                parameters.useSecretChats = true;
                parameters.apiId = 93054;
                parameters.apiHash = "16758ee9e2f0411a4efc5d1eb1edea07";
                parameters.systemLanguageCode = "en";
                parameters.deviceModel = "Desktop";
                parameters.systemVersion = "Unknown";
                parameters.applicationVersion = "1.0";
                parameters.enableStorageOptimizer = true;
//                parameters.useTestDc = true;

                client.send(new TdApi.SetTdlibParameters(parameters), new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR:
                client.send(new TdApi.CheckDatabaseEncryptionKey(), new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
                Intent intent = new Intent(this, PhoneNumber.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            }
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                Intent intent = new Intent(this, Code.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            }
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR: {
                // @new
//                String password = promptString("Please enter password: ");
//                client.send(new TdApi.CheckAuthenticationPassword(password), new AuthorizationRequestHandler());
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR:
                // Broadcast AuthState OK to Main for green semaphore
                // 1.
                final Intent intent = new Intent("de.egi.geofence.geozone.plugin.tgm.AUTHSTATEOK");
                sendBroadcast(intent);
                sendFunction(new TdApi.GetChats(null, 2000), this);
                // Logged in
                info("auth state OK :: login state: true");
                editor.putBoolean(PreferenceKeys.LOGGEDSTATE, true);
                editor.apply();

                // Search for Bot
//            sendFunction(new TdApi.SearchPublicChat(mPrefs.getString(PreferenceKeys.BOT_NAME, "")), this);

                info("AuthStateOk");

                break;
            case TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR:
                print("Logging out");
                // Broadcast AuthState OK to Main for red semaphore
                final Intent intentOK = new Intent("de.egi.geofence.geozone.plugin.tgm.AUTHSTATENOK");
                sendBroadcast(intentOK);

                info("auth state OK :: login state: false");
                editor.putBoolean(PreferenceKeys.LOGGEDSTATE, false);
                editor.apply();
                info("AuthStateLoggingOut");

                break;
            case TdApi.AuthorizationStateClosing.CONSTRUCTOR:
                print("Closing");
                break;
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR:
                print("Closed");
                client.close();
                break;
            default:
                System.err.println("Unsupported authorization state:" + newLine + authorizationState);
            case TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR:
                break;
            case TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR:
                break;
        }
    }

    private class AuthorizationRequestHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            int constructor = object.getConstructor();
            if (constructor == TdApi.Error.CONSTRUCTOR) {
                System.err.println("Receive an error:" + newLine + object);
                onAuthorizationStateUpdated(null); // repeat last action
            } else if (constructor == TdApi.Ok.CONSTRUCTOR) {// result is already received through UpdateAuthorizationState, nothing to do
            } else {
                System.err.println("Receive wrong response from TDLib:" + newLine + object);
            }
        }
    }

}
