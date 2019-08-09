package com.tab.demo.nearby;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.collection.SimpleArrayMap;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.File;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NearbyService extends Service {
    private static final String TAG = "NearbyService";
    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;
    private static final String SERVICE_ID = "com.tab.demo.nearby";
    private static final String LOCAL_ENDPOINT_NAME = Build.DEVICE;
    private static final String CHANNEL_ID = "channel";
    private static final int NOTIFICATION_ID = 101;
    public static final String BROADCAST_ACTION = "com.tab.demo.nearby.reports";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_STATUS = "extra_status";

    private String remoteEndpointId;
    private String remoteEndpointName;
    private final IBinder binder = new LocalBinder();
    ConnectionsClient connectionsClient;

    @Override
    public void onCreate() {
        super.onCreate();
        connectionsClient = Nearby.getConnectionsClient(this);
        startForeground(NOTIFICATION_ID, getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //searchEndpoint();
        startAdvertising();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class LocalBinder extends Binder {
        NearbyService getService() {
            return NearbyService.this;
        }
    }

    private void searchEndpoint() {
        startAdvertising();
        startDiscovery();
    }

    private void startAdvertising() {
        Log.d(TAG, "startAdvertising()");
        connectionsClient.startAdvertising(
                LOCAL_ENDPOINT_NAME, SERVICE_ID, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    public void startDiscovery() {
        Log.d(TAG, "startDiscovery()");
        connectionsClient.startDiscovery(
                SERVICE_ID, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i(TAG, "onEndpointFound: endpointId =" + endpointId);
                    Log.i(TAG, "onEndpointFound: info.getEndpointName() =" + info.getEndpointName());
                    connectionsClient.stopDiscovery();
                    connectionsClient.requestConnection(LOCAL_ENDPOINT_NAME, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    Log.i(TAG, "onEndpointLost: endpointId =" + endpointId);
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.i(TAG, "onConnectionInitiated: endpointId =" + endpointId);
                    Log.i(TAG, "onConnectionInitiated: connectionInfo.getEndpointName() =" + connectionInfo.getEndpointName());
                    Log.i(TAG, "onConnectionInitiated:connectionInfo.isIncomingConnection() =" + connectionInfo.isIncomingConnection());
                    Log.i(TAG, " acceptConnection");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    remoteEndpointName = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.i(TAG, "onConnectionResult: connection successful");
                        Log.i(TAG, "onConnectionResult: endpointId =" + endpointId);
                        remoteEndpointId = endpointId;
                        reportConnectStatus(remoteEndpointName,"Connected");
                    } else {
                        Log.i(TAG, "onConnectionResult: connection failed");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.i(TAG, "onDisconnected: endpointId =" + endpointId);
                    reportConnectStatus(remoteEndpointName,"Disconnected");
                }
            };

    public void reportConnectStatus(String name, String status) {
        Intent localIntent = new Intent(BROADCAST_ACTION)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_STATUS, status);
        LocalBroadcastManager.getInstance(NearbyService.this).sendBroadcast(localIntent);
    }

    public void sendStringPayload(String str) {
        connectionsClient.sendPayload(remoteEndpointId, Payload.fromBytes(str.getBytes(UTF_8)));
    }

    public void sendFilePayload(Payload filePayload) {
        connectionsClient.sendPayload(remoteEndpointId, filePayload);
    }

    public void disconnect() {
        connectionsClient.disconnectFromEndpoint(remoteEndpointId);
    }

    public void connect() {
        connectionsClient.requestConnection(LOCAL_ENDPOINT_NAME, remoteEndpointId, connectionLifecycleCallback);
    }

    private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            Log.d(TAG, "onPayloadReceived, payload.getType() = " + payload.getType());
            if (payload.getType() == Payload.Type.BYTES) {
                String str = new String(payload.asBytes(), UTF_8);
                //txtBytes.setText(msg);
            } else if (payload.getType() == Payload.Type.FILE) {
                incomingFilePayloads.put(payload.getId(), payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                Log.d(TAG, "PayloadTransferUpdate.Status.SUCCESS");
                long payloadId = update.getPayloadId();
                Payload payload = incomingFilePayloads.remove(payloadId);
                if (payload != null) {
                    Log.d(TAG, "payload.getType() " + payload.getType());
                    completedFilePayloads.put(payloadId, payload);
                    if (payload.getType() == Payload.Type.FILE) {
                        processFilePayload(payloadId);
                    }
                }
            }
        }
    };


    private void processFilePayload(long payloadId) {
        Log.d(TAG, "processFilePayload ");
        Payload filePayload = completedFilePayloads.get(payloadId);
        if (filePayload != null) {
            completedFilePayloads.remove(payloadId);
            File file = getFile(filePayload);
            showFile(file);
        }
    }

    private File getFile(Payload filePayload) {
        File payloadFile = filePayload.asFile().asJavaFile();
        String randomName = "nearby_shared-" + System.currentTimeMillis() + ".jpg";
        File targetFileName = new File(payloadFile.getParentFile(), randomName);
        boolean result = payloadFile.renameTo(targetFileName);
        if (!result) Log.d(TAG, "renameTo failed  ");
        Log.d(TAG, "targetFileName =  " + targetFileName.toString());
        return targetFileName;
    }

    private void showFile(File targetFileName) {
        Uri uri = FileProvider.getUriForFile(this, "com.tab.demo.nearby", targetFileName);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }


    private Notification getNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nearby Service")
                .setContentText("Wi-Fi Direct")
                .setSmallIcon(R.drawable.icon)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        stopDiscovery();
        connectionsClient.stopAllEndpoints();
    }

    public void stopDiscovery() {
        connectionsClient.stopDiscovery();
    }

    public void stopAdvertising() {
        connectionsClient.stopAdvertising();
    }


}
