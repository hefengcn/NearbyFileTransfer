package com.tab.demo.nearby;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_GET_CONTENT = 20;

    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String SERVICE_ID = "com.tab.demo.nearby";
    private static final String NICK_NAME = Build.DEVICE;
    private ConnectionsClient connectionsClient;
    private String mEndpointId;
    private String peerName;
    private TextView txtRemoteName;
    private TextView txtStatus;
    private TextView txtBytes;


    /**
     * Broadcasts our presence using Nearby Connections so other players can find us.
     */
    public void startAdvertising(View view) {
        Log.d(TAG, "startAdvertising()");
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                NICK_NAME, SERVICE_ID, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    /**
     * Starts looking for other players using Nearby Connections.
     */
    public void startDiscovery(View view) {
        Log.d(TAG, "startDiscovery()");
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                SERVICE_ID, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    public void sendBytes(View view) {
        String str = String.valueOf(new Random().nextInt(100));
        connectionsClient.sendPayload(mEndpointId, Payload.fromBytes(str.getBytes(UTF_8)));

    }

    public void sendFile(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GET_CONTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_GET_CONTENT) {
            super.onActivityResult(requestCode, resultCode, data);
            Uri uri = data.getData();
            Payload filePayload;
            try {
                // Open the ParcelFileDescriptor for this URI with read access.
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                filePayload = Payload.fromFile(pfd);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found", e);
                return;
            }
            // Construct a simple message mapping the ID of the file payload to the desired filename.
            String filenameMessage = filePayload.getId() + ":" + uri.getLastPathSegment();
            // Send the filename message as a bytes payload.
            Payload filenameBytesPayload = Payload.fromBytes(filenameMessage.getBytes(StandardCharsets.UTF_8));
            connectionsClient.sendPayload(mEndpointId, filenameBytesPayload);
            // Finally, send the file payload.
            connectionsClient.sendPayload(mEndpointId, filePayload);
        }
    }

    // Callbacks for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    String command = new String(payload.asBytes(), UTF_8);
                    txtBytes.setText(command);
                }


                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == Status.SUCCESS) {
                    }
                }
            };

    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i(TAG, "onEndpointFound: endpoint found, connecting");
                    connectionsClient.requestConnection(NICK_NAME, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                }
            };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.i(TAG, "onConnectionInitiated: accepting connection");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    peerName = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.i(TAG, "onConnectionResult: connection successful");
                        mEndpointId = endpointId;
                        txtRemoteName.setText(peerName);
                        txtStatus.setText(getString(R.string.status_connected));
                    } else {
                        Log.i(TAG, "onConnectionResult: connection failed");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.i(TAG, "onDisconnected: disconnected from the opponent");
                    txtStatus.setText(getString(R.string.status_disconnected));
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        TextView txtLocalName = findViewById(R.id.local_nick_name);
        txtRemoteName = findViewById(R.id.remote_nick_name);
        txtStatus = findViewById(R.id.status);
        txtBytes = findViewById(R.id.bytes_received);
        txtLocalName.setText(getString(R.string.local_nick_name, NICK_NAME));

        connectionsClient = Nearby.getConnectionsClient(this);

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    @Override
    protected void onStop() {
//        connectionsClient.stopAllEndpoints();
//        resetView();
        super.onStop();
    }

    /**
     * Returns true if the app was granted all the permissions. Otherwise, returns false.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles user acceptance (or denial) of our permission request.
     */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    public void connect(View view) {
        Log.i(TAG, "connect clicked");
        if (mEndpointId != null)
            connectionsClient.requestConnection(NICK_NAME, mEndpointId, connectionLifecycleCallback);
    }

    public void disconnect(View view) {
        Log.i(TAG, "disconnect clicked");
        if (mEndpointId != null)
            connectionsClient.disconnectFromEndpoint(mEndpointId);
    }


    private void resetView() {
        txtRemoteName.setText(getString(R.string.no_peer_join));
        txtStatus.setText(getString(R.string.status_disconnected));
    }

}
