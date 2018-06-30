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

package de.egi.geofence.geozone.plugin.tgm.utils;

public class Utils {

	/**
	 * Replace text
	 */
	public static String replaceVar(String source, String search, String replace) {
		if (search.equals(replace)) {
			return source; // nothing to do
		}

		StringBuilder result = new StringBuilder();
		int len = search.length();
		if (len == 0) {
			return source; // v
		}

		int pos = 0; // position
		int nPos; // next position
		do {
			nPos = source.indexOf(search, pos);
			if (nPos != -1) { // found
				result.append(source.substring(pos, nPos));
				result.append(replace);
				pos = nPos + len;
			} else { // not found
				result.append(source.substring(pos)); // last
			}
		} while (nPos != -1);

		return result.toString();
	}
}









