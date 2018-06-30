/*
* Copyright 2014 - 2015 Egmont R. (egmontr@gmail.com)
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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import de.egi.geofence.geozone.plugin.tgm.preferences.PreferenceKeys;

public class Properties extends Activity   {
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.properties);

        SharedPreferences mPrefs = getSharedPreferences(TgmPluginMain.SHARED_PREFERENCE_NAME, MODE_PRIVATE);
        ((EditText) this.findViewById(R.id.editFirstName)).setText(mPrefs.getString(PreferenceKeys.USER_FIRST_NAME, ""));
        ((EditText) this.findViewById(R.id.editLastName)).setText(mPrefs.getString(PreferenceKeys.USER_LAST_NAME, ""));
        ((EditText) this.findViewById(R.id.editPhone)).setText(mPrefs.getString(PreferenceKeys.PHONE_NUMBER, ""));
        ((EditText) this.findViewById(R.id.editBotName)).setText(mPrefs.getString(PreferenceKeys.BOT_NAME, ""));
        ((EditText) this.findViewById(R.id.editBotID)).setText(Long.toString(mPrefs.getLong(PreferenceKeys.BOT_ID, 0)));
        ((EditText) this.findViewById(R.id.editCode)).setText(mPrefs.getString(PreferenceKeys.CODE, ""));
    }
}