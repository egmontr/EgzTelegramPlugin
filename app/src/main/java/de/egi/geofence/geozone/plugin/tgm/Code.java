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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import org.drinkless.td.libcore.telegram.TdApi;

import de.egi.geofence.geozone.plugin.tgm.preferences.PreferenceKeys;

public class Code extends AppCompatActivity implements TextWatcher, View.OnClickListener{
    // The name of the resulting SharedPreferences
    private static final String SHARED_PREFERENCE_NAME = TgmPluginMain.class.getSimpleName();
    private SharedPreferences.Editor editor;

    private EditText code;
    private EditText botname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.code);
        SharedPreferences mPrefs = getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE);
        editor = mPrefs.edit();

        code = this.findViewById(R.id.editCode);
        botname = this.findViewById(R.id.editBotName);
        Button button = this.findViewById(R.id.button);

        code.addTextChangedListener(this);
        botname.addTextChangedListener(this);
        button.setOnClickListener(this);

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
        if (botname.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.BOT_NAME, botname.getText().toString().trim());
        }else if (code.getText().hashCode() == s.hashCode()) {
            editor.putString(PreferenceKeys.CODE, code.getText().toString().trim());
        }
        editor.commit();
    }

    @Override
    public void onClick(View view) {
        TgmPluginApplication tpa = ((TgmPluginApplication)getApplication());
        tpa.sendFunction(new TdApi.CheckAuthenticationCode(code.getText().toString()), tpa);
        finish();
    }
}
