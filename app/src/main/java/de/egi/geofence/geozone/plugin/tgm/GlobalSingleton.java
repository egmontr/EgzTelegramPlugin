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

public class GlobalSingleton {
	private static GlobalSingleton _instance;
	private String command = "";
	private long chatBotId = 0;

	private GlobalSingleton() {
	}

	public static GlobalSingleton getInstance() {
		if (_instance == null) {
			_instance = new GlobalSingleton();
		}
		return _instance;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public long getChatBotId() {
		return chatBotId;
	}

	public void setChatBotId(long chatBotId) {
		this.chatBotId = chatBotId;
	}

}










