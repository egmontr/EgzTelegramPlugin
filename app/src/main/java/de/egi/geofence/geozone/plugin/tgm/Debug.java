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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class Debug extends Activity   {
	private ListView listViewDebug = null;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug);
//		final ArrayAdapter adapter = new ArrayAdapter(this, R.id.listView_debug);
        listViewDebug = findViewById(R.id.listView_debug);
        listViewDebug.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
            	int position, long id) {
            	String  itemValue    = (String) listViewDebug.getItemAtPosition(position);
	     	    Intent data = new Intent();
	    	    data.putExtra("level", itemValue);
                setResult(4712, data);
                finish();
            }
       }); 
    }
}