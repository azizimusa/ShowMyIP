package g.jom.showmyip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class IpAddressNotificationService extends Service {
    private static final String TAG = "IpAddressNotificationService";
    private static final String CHANNEL_ID = "ip_address_channel";
    private static final int NOTIFICATION_ID = 1;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        setupNetworkCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create the notification channel (required for Android Oreo and above)
        createNotificationChannel();

        // Create the initial notification
        Notification notification = createNotification("Checking IP address...");

        // Start the service in the foreground
        startForeground(NOTIFICATION_ID, notification);

        // Register the network callback to start listening for changes
        registerNetworkCallback();

        // Perform an initial update
        updateIpAddress();

        // If the service is killed, it will be automatically restarted
        return START_STICKY;
    }

    private void setupNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Network is available. Updating IP.");
                updateIpAddress();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.d(TAG, "Network is lost. Updating IP.");
                updateIpAddress();
            }
        };
    }

    private void registerNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    private void updateIpAddress() {
        String ipAddress = getDeviceIpAddress();
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = "No network connection";
        }
        Log.d(TAG, "Current IP: " + ipAddress);
        updateNotification(ipAddress);
        Toast.makeText(this, "IP Address : " + ipAddress, Toast.LENGTH_LONG).show();
    }

    private void updateNotification(String ipAddress) {
        Notification notification = createNotification(ipAddress);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Your IP Address")
                .setContentText(text)
                // Make sure you have an icon named 'ic_notification' in your drawable folder
                .setSmallIcon(R.drawable.ic_stat_network)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "IP Address Notification",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private String getDeviceIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':')<0;
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the callback when the service is destroyed to prevent memory leaks
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }
}
