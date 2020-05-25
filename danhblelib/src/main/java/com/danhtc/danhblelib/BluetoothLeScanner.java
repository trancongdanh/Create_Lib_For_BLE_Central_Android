package com.danhtc.danhblelib;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public class BluetoothLeScanner {

    public interface BluetoohLeScannerListener {
        void onScanCallback(final BluetoothDevice device, int rssi, byte[] scanRecord);
    }

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 12;
    private static final int LOCATION_REQUEST_CODE = 13;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private Activity parentActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoohLeScannerListener listener;

    public void initScanner(Activity parent, BluetoohLeScannerListener listener) {
        mHandler = new Handler();
        parentActivity = parent;
        this.listener = listener;

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!parentActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(parentActivity, "BLE is not supported", Toast.LENGTH_SHORT).show();
            parentActivity.finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) parentActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(parentActivity, "Bluetooth not supported.", Toast.LENGTH_SHORT).show();
            parentActivity.finish();
        }
    }

    public void startScanner() {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                parentActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        scanLeDevice(true);
    }

    public void stopScanner() {
        scanLeDevice(false);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (ContextCompat.checkSelfPermission(parentActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                tryStartScan();
            } else {

                ActivityCompat.requestPermissions(parentActivity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_CODE);
            }

        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    private void tryStartScan() {
        if (isLocationServicesAvailable(parentActivity)) {
            startScan();
        } else {
            Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            parentActivity.startActivityForResult(enableLocationIntent, LOCATION_REQUEST_CODE);
        }
    }

    private void startScan() {
        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, SCAN_PERIOD);

        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    parentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (BluetoothLeScanner.this.listener != null) {
                                listener.onScanCallback(device, rssi, scanRecord);
                            }
                        }
                    });
                }
            };

    boolean isLocationServicesAvailable(Context context) {
        int locationMode = 0;
        boolean isAvailable;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        isAvailable = (locationMode != Settings.Secure.LOCATION_MODE_OFF);

        boolean coarsePermissionCheck = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        boolean finePermissionCheck = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);

        return isAvailable && (coarsePermissionCheck || finePermissionCheck);
    }
}
