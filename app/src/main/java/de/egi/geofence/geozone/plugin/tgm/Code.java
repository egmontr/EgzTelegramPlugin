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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.drinkless.td.libcore.telegram.TdApi;

import de.egi.geofence.geozone.plugin.tgm.preferences.PreferenceKeys;

public class Code extends AppCompatActivity implements TextWatcher, View.OnClickListener{
    // The name of the resulting SharedPreferences
    private static final String SHARED_PREFERENCE_NAME = TgmPluginMain.class.getSimpleName();
    private SharedPreferences.Editor editor;

    private EditText code;
    private EditText fname;
    private EditText lname;
    private EditText botname;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.code);
        SharedPreferences mPrefs = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE);
        editor = mPrefs.edit();

        code = (EditText) this.findViewById(R.id.editCode);
        fname = (EditText) this.findViewById(R.id.editFirstName);
        lname = (EditText) this.findViewById(R.id.editLastName);
        botname = (EditText) this.findViewById(R.id.editBotName);
        Button button = (Button) this.findViewById(R.id.button);

        code.addTextChangedListener(this);
        fname.addTextChangedListener(this);
        lname.addTextChangedListener(this);
        botname.addTextChangedListener(this);
        button.setOnClickListener(this);

        lname.setText(mPrefs.getString(PreferenceKeys.USER_LAST_NAME, ""));
        fname.setText(mPrefs.getString(PreferenceKeys.USER_FIRST_NAME, ""));
        botname.setText(mPrefs.getString(PreferenceKeys.BOT_NAME, ""));
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (lname.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.USER_LAST_NAME, lname.getText().toString().trim());
        } else if (fname.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.USER_FIRST_NAME, fname.getText().toString().trim());
        }else if (botname.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.BOT_NAME, botname.getText().toString().trim());
        }else if (code.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.CODE, code.getText().toString().trim());
        }
        editor.commit();
    }

    @Override
    public void onClick(View view) {
        TgmPluginApplication tpa = ((TgmPluginApplication)getApplication());
        tpa.sendFunction(new TdApi.CheckAuthCode(code.getText().toString(), fname.getText().toString(), lname.getText().toString()), tpa);
        finish();
    }
}
