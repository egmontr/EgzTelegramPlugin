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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.log4j.Level;
import org.drinkless.td.libcore.telegram.TdApi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import de.egi.geofence.geozone.plugin.tgm.preferences.PreferenceKeys;
import de.egi.geofence.geozone.plugin.tgm.utils.NotificationUtil;
import de.egi.geofence.geozone.plugin.tgm.utils.Utils;

public class TgmPluginMain extends AppCompatActivity implements TextWatcher, CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    // The name of the resulting SharedPreferences
    public static final String SHARED_PREFERENCE_NAME = TgmPluginMain.class.getSimpleName();
    private SharedPreferences.Editor editor;
    private TgmPluginApplication tpa;

    private long chatBotId;

    private EditText z1;
    private EditText c1;
    private EditText z2;
    private EditText c2;
    private EditText z3;
    private EditText c3;
    private EditText z4;
    private EditText c4;
    private EditText z5;
    private EditText c5;
    private EditText z6;
    private EditText c6;
    private ImageView ampel = null;
    private SwitchCompat notifyResponse = null;
    private ToggleButton toggle = null;

    //The BroadcastReceiver
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("de.egi.geofence.geozone.plugin.tgm.AUTHSTATEOK".equals(action)) {
                // Set semaphore to green
                ampel.setImageResource(R.drawable.ic_lens_green_24dp);
            }
            if ("de.egi.geofence.geozone.plugin.tgm.AUTHSTATENOK".equals(action)) {
                ampel.setImageResource(R.drawable.ic_lens_red_24dp);
            }
        }
    };

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);

        if (!checkAllNeededPermissions()){
            // Display UI and wait for user interaction
            android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(getString(R.string.alertPermissions));
            alertDialogBuilder.setTitle(getString(R.string.titleAlertPermissions));

            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    startActivity(intent);
                    finish();
                }
            });
            android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return;
        }

        IntentFilter filter1 = new IntentFilter("de.egi.geofence.geozone.plugin.tgm.AUTHSTATEOK");
        this.registerReceiver(mReceiver, filter1);
        IntentFilter filter2 = new IntentFilter("de.egi.geofence.geozone.plugin.tgm.AUTHSTATENOK");
        this.registerReceiver(mReceiver, filter2);

        ampel = ((ImageView) this.findViewById(R.id.tracking_service_state));

        SharedPreferences mPrefs = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE);
        editor = mPrefs.edit();
        editor.apply();

        tpa = ((TgmPluginApplication) getApplication());

        toggle = (ToggleButton) this.findViewById(R.id.toggleButton);

        notifyResponse = (SwitchCompat) this.findViewById(R.id.notifyResponse);

        z1 = (EditText) this.findViewById(R.id.editZone1);
        z2 = (EditText) this.findViewById(R.id.editZone2);
        z3 = (EditText) this.findViewById(R.id.editZone3);
        z4 = (EditText) this.findViewById(R.id.editZone4);
        z5 = (EditText) this.findViewById(R.id.editZone5);
        z6 = (EditText) this.findViewById(R.id.editZone6);

        c1 = (EditText) this.findViewById(R.id.editCommand1);
        c2 = (EditText) this.findViewById(R.id.editCommand2);
        c3 = (EditText) this.findViewById(R.id.editCommand3);
        c4 = (EditText) this.findViewById(R.id.editCommand4);
        c5 = (EditText) this.findViewById(R.id.editCommand5);
        c6 = (EditText) this.findViewById(R.id.editCommand6);

        Button send = (Button) this.findViewById(R.id.sendTestMessage);
        Button logoff = (Button) this.findViewById(R.id.logoff);
        Button login = (Button) this.findViewById(R.id.login);

        z1.addTextChangedListener(this);
        z2.addTextChangedListener(this);
        z3.addTextChangedListener(this);
        z4.addTextChangedListener(this);
        z5.addTextChangedListener(this);
        z6.addTextChangedListener(this);

        c1.addTextChangedListener(this);
        c2.addTextChangedListener(this);
        c3.addTextChangedListener(this);
        c4.addTextChangedListener(this);
        c5.addTextChangedListener(this);
        c6.addTextChangedListener(this);

        toggle.setOnCheckedChangeListener(this);
        notifyResponse.setOnCheckedChangeListener(this);

        send.setOnClickListener(this);
        logoff.setOnClickListener(this);
        login.setOnClickListener(this);

        boolean sw = mPrefs.getBoolean(PreferenceKeys.SWITCH, false);
        boolean nr = mPrefs.getBoolean(PreferenceKeys.NONOTIFYRESPONSE, false);
        String sz1 = mPrefs.getString(PreferenceKeys.Z1, "");
        String sz2 = mPrefs.getString(PreferenceKeys.Z2, "");
        String sz3 = mPrefs.getString(PreferenceKeys.Z3, "");
        String sz4 = mPrefs.getString(PreferenceKeys.Z4, "");
        String sz5 = mPrefs.getString(PreferenceKeys.Z5, "");
        String sz6 = mPrefs.getString(PreferenceKeys.Z6, "");

        String sc1 = mPrefs.getString(PreferenceKeys.C1, "");
        String sc2 = mPrefs.getString(PreferenceKeys.C2, "");
        String sc3 = mPrefs.getString(PreferenceKeys.C3, "");
        String sc4 = mPrefs.getString(PreferenceKeys.C4, "");
        String sc5 = mPrefs.getString(PreferenceKeys.C5, "");
        String sc6 = mPrefs.getString(PreferenceKeys.C6, "");

        chatBotId = mPrefs.getLong(PreferenceKeys.BOT_ID, 0);

        toggle.setChecked(sw);
        notifyResponse.setChecked(nr);
        z1.setText(sz1);
        z2.setText(sz2);
        z3.setText(sz3);
        z4.setText(sz4);
        z5.setText(sz5);
        z6.setText(sz6);
        c1.setText(sc1);
        c2.setText(sc2);
        c3.setText(sc3);
        c4.setText(sc4);
        c5.setText(sc5);
        c6.setText(sc6);

        if (mPrefs.getBoolean(PreferenceKeys.LOGGEDSTATE, false)){
            // Set semaphore to green
            ampel.setImageResource(R.drawable.ic_lens_green_24dp);
        }else{
            // Set semaphore to red
            ampel.setImageResource(R.drawable.ic_lens_red_24dp);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_plugin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_info) {
            Intent info = new Intent(this, Info.class);
            startActivity(info);
            return true;
        }
        if (id == R.id.action_debug) {
            Intent i4 = new Intent(this, Debug.class);
            startActivityForResult(i4, 4712);
            return true;
        }
        if (id == R.id.action_props) {
            Intent i5 = new Intent(this, Properties.class);
            startActivity(i5);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {
            // Debug Level setzen
            case 4712 :
                if (resultCode == RESULT_OK) {
                    String level = intent.getStringExtra("level");
                    TgmPluginApplication.logConfigurator.setRootLevel(Level.toLevel(level));
                    TgmPluginApplication.logConfigurator.setLevel("de.egi.geofence.geozone", Level.toLevel(level));
                    try{
                        TgmPluginApplication.logConfigurator.configure();
                    } catch (Exception e) {
                        // Do nothing
                    }
                    editor.putString(PreferenceKeys.LOG_LEVEL, level);
                    editor.apply();
                    // Show Alert
                    Toast.makeText(this, " Level : " + level , Toast.LENGTH_LONG).show();
                }
                break;
            // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                break;
        }
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (z1.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.Z1, z1.getText().toString());
        }
        if (c1.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.C1, c1.getText().toString());
        }
        if (z2.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.Z2, z2.getText().toString());
        }
        if (c2.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.C2, c2.getText().toString());
        }
        if (z3.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.Z3, z3.getText().toString());
        }
        if (c3.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.C3, c3.getText().toString());
        }
        if (z4.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.Z4, z4.getText().toString());
        }
        if (c4.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.C4, c4.getText().toString());
        }
        if (z5.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.Z5, z5.getText().toString());
        }
        if (c5.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.C5, c5.getText().toString());
        }
        if (z6.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.Z6, z6.getText().toString());
        }
        if (c6.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.C6, c6.getText().toString());
        }
        editor.apply();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == toggle) {
            if (isChecked) {
                editor.putBoolean(PreferenceKeys.SWITCH, true);
            } else {
                editor.putBoolean(PreferenceKeys.SWITCH, false);
            }
        }
        if (buttonView == notifyResponse) {
            if (isChecked) {
                editor.putBoolean(PreferenceKeys.NONOTIFYRESPONSE, true);
            } else {
                editor.putBoolean(PreferenceKeys.NONOTIFYRESPONSE, false);
            }
        }
        editor.apply();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.logoff:
                tpa.sendFunction(new TdApi.ResetAuth(), tpa);

                editor.putLong(PreferenceKeys.BOT_ID, 0);
                editor.putString(PreferenceKeys.CODE, "");
                editor.apply();

                break;
            case R.id.login:
                tpa.sendFunction(new TdApi.GetAuthState(), tpa);
                break;
            case R.id.sendTestMessage:
                String command = c1.getText().toString();
                command = Utils.replaceVar(command, TgmPluginIntentService.ZONE, z1.getText().toString());
                command = Utils.replaceVar(command, TgmPluginIntentService.TRANSITION, getString(R.string.geofence_transition_entered));
                command = Utils.replaceVar(command, TgmPluginIntentService.TRANSITIONTYPE, "1");
                command = Utils.replaceVar(command, TgmPluginIntentService.LAT, "48");
                command = Utils.replaceVar(command, TgmPluginIntentService.LNG, "11");
                command = Utils.replaceVar(command, TgmPluginIntentService.DEVICEID, "FFAA");

                TimeZone tz1 = TimeZone.getDefault();
                DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                df.setTimeZone(tz1);
                String nowAsLocal = df1.format(new Date());

                command = Utils.replaceVar(command, TgmPluginIntentService.DATE, nowAsLocal);

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

                    // Show Alert
                    Toast.makeText(this, getString(R.string.wrong_bot), Toast.LENGTH_LONG).show();

                    break;
                }

                GlobalSingleton.getInstance().setChatBotId(chatBotId);

                // Save command to globals
                GlobalSingleton.getInstance().setCommand(command);

//                tpa.sendMessage(chatBotId, new TdApi.InputMessageText(command, true, true, null, null), tpa);

                // Get AuthState and den Broadcast to send message
                tpa.sendFunction(new TdApi.GetAuthState(), tpa);

                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            this.unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter1 = new IntentFilter("de.egi.geofence.geozone.plugin.tgm.AUTHSTATEOK");
        this.registerReceiver(mReceiver, filter1);
        IntentFilter filter2 = new IntentFilter("de.egi.geofence.geozone.plugin.tgm.AUTHSTATENOK");
        this.registerReceiver(mReceiver, filter2);
    }

    // Check for all needed permissions
    public boolean checkAllNeededPermissions(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result != PackageManager.PERMISSION_GRANTED){
            return false;
        }
        return true;
    }
}