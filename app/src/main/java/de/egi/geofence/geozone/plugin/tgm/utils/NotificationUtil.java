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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;

public class NotificationUtil {
//	private final static Logger log = Logger.getLogger(NotificationUtil.class);

	@SuppressWarnings("SameParameterValue")
	public static void notify(Context context, int notifyId, PendingIntent pendingIntent, String contentTitle,
							  String contentText, String tickerText, boolean vibrate, boolean playSound, int icon) {
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

		// Get an instance of the Notification manager
		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		String channelId = "channel-gcm";
		String channelName = "EgiTgmPlugin";

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			NotificationChannel mChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
			mChannel.setShowBadge(true);
			mChannel.shouldShowLights();
			if (vibrate) {
				mChannel.enableVibration(true);
				mChannel.setVibrationPattern(new long[] { 100, 400 });
			}

			notificationManager.createNotificationChannel(mChannel);
		}


		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
				.setContentTitle(contentTitle)
				.setContentText(contentText)
				.setTicker(tickerText)
				.setSmallIcon(icon)
				.setLights(0xff00960b, 1000, 5000)
				.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon))
				.setWhen(System.currentTimeMillis()).setAutoCancel(true)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
				.setContentIntent(pendingIntent);
		if (vibrate) {
			notificationBuilder.setVibrate(new long[] { 100, 400 });
		}
		if (playSound) {
			notificationBuilder.setSound(alarmSound);
		}
		Notification notification = notificationBuilder.build();
		notificationManager.notify(notifyId, notification);
	}
}