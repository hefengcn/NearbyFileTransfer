package com.tab.demo.nearby;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.nearby.connection.Payload;

import java.io.FileNotFoundException;
import java.util.Random;

import static com.tab.demo.nearby.NearbyService.BROADCAST_ACTION;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_OPEN_DOCUMENT = 20;

    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final String LOCAL_ENDPOINT_NAME = Build.DEVICE;
    private TextView tvRemoteEndpointID;
    private TextView tvRemoteEndpointName;
    private TextView tvConnectStatus;
    private TextView tvBytesReceived;

    private NearbyService mService;
    private boolean mBound = false;
    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        TextView tvLocalEndpointName = findViewById(R.id.local_endpoint_name);
        tvLocalEndpointName.setText(getString(R.string.local_endpoint_name, LOCAL_ENDPOINT_NAME));
        tvRemoteEndpointID = findViewById(R.id.remote_endpoint_id);
        tvRemoteEndpointName = findViewById(R.id.remote_endpoint_name);
        tvConnectStatus = findViewById(R.id.connect_status);
        tvBytesReceived = findViewById(R.id.bytes_received);
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(MainActivity.this, NearbyService.class));
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SimpleArrayMap<String, EndpointStatus> endpoints = mService.getStatus();
            if (!endpoints.isEmpty()) {
                tvRemoteEndpointID.setText("Remote endpoint id:" + endpoints.keyAt(0));
                tvRemoteEndpointName.setText("Remote endpoint name: " + endpoints.valueAt(0).getName());
                tvConnectStatus.setText("Connection status: " + endpoints.valueAt(0).getStatus());
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, NearbyService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            NearbyService.LocalBinder binder = (NearbyService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.reportConnectStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void sendBytes(View view) {
        String str = String.valueOf(new Random().nextInt(100));
        mService.sendStringPayload(str);
    }

    public void sendFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_OPEN_DOCUMENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_OPEN_DOCUMENT
                && resultCode == Activity.RESULT_OK
                && resultData != null) {
            Uri uri = resultData.getData();
            Log.d(TAG, "uri = " + uri);
            Payload filePayload;
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                filePayload = Payload.fromFile(pfd);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found", e);
                return;
            }
            mService.sendFilePayload(filePayload);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mService.stopDiscovery();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) unbindService(connection);
        mBound = false;
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

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
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
        mService.connect();
    }

    public void disconnect(View view) {
        Log.i(TAG, "disconnect clicked");
        mService.disconnect();
    }

    private void resetView() {
        tvRemoteEndpointName.setText(getString(R.string.no_remote_endpoint_join));
        tvConnectStatus.setText(getString(R.string.status_disconnected));
    }

    public void onStartButtonClick(View view) {
        mService.stopAdvertising();
        mService.startDiscovery();
    }

    public void onStopButtonClick(View view) {
        mService.onDestroy();
        System.exit(0);
    }
}
