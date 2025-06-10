package g.jom.showmyip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IpAddressNotificationService extends Service {
    private static final String TAG = "IpAddressService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ip_address_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String ipAddress = getLocalIpAddress();
        showOrUpdateNotification(ipAddress);

        Toast.makeText(this, "IP : " + ipAddress, Toast.LENGTH_LONG).show();

        // This makes the service a foreground service, which is required for persistent notifications.
        // It also makes the service less likely to be killed by the system.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding, so return null
    }

    /**
     * Creates and displays or updates the persistent notification.
     * @param ipAddress The IP address to display.
     */
    private void showOrUpdateNotification(String ipAddress) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Local IP Address")
                .setContentText("IP: " + (ipAddress != null ? ipAddress : "Not Connected"))
                .setSmallIcon(R.drawable.ic_stat_network) // IMPORTANT: Create this drawable
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Makes the notification persistent
                .setOnlyAlertOnce(true) // Prevents sound/vibration on update
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * Retrieves the local IPv4 address of the device.
     * @return The IP address string or "Not Connected" if no address is found.
     */
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    // Check if it's not a loopback address and is an IPv4 address
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "Error getting IP address", ex);
        }
        return "Not Connected";
    }

    /**
     * Creates a NotificationChannel, required for Android Oreo (API 26) and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "IP Address Service Channel",
                    NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound on each update
            );
            serviceChannel.setDescription("Channel for the persistent IP address notification");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
