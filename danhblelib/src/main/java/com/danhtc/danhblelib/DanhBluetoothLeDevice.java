package com.danhtc.danhblelib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class DanhBluetoothLeDevice {

    public interface DanhBluetoothLeDeviceListener {
        void onGattConnected();
        void onGattDisconnect();
        void onGattDisconnected();
        void onGattServicesDiscovered();
        void onGattWriteCharacteristicSuccessfully();
        void onGattWriteCharacteristicRed();
        void onGattWriteCharacteristicGreen();
        void onGattWriteCharacteristicSleep();
        void onGattError(String errorMessage);
    }

    public static final String RED_STATUS = "RED";
    public static final String GREEN_STATUS = "GREEN";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private DanhBluetoothLeDeviceListener mListener;
    private Activity mActivity;

    private final String SERVICE_UUID_STR = "AAB7643F-BD0C-4BFD-8547-4027364BD723";
    private final String CHARAC_UUID_STR = "60FF3470-DAB6-4890-910D-CAC5911ED642";

    final Handler handler = new Handler();

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            ;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mListener.onGattConnected();
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mListener.onGattDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mListener.onGattServicesDiscovered();
            } else {
                mListener.onGattError("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {

            mListener.onGattWriteCharacteristicSuccessfully();

            byte[] bytes = characteristic.getValue();
            try {
                String str = new String(bytes, "UTF-8");

                if (RED_STATUS.equals(str)) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Do something after 1s = 1000ms
                            writeCustomCharacteristic(GREEN_STATUS);
                            mListener.onGattWriteCharacteristicGreen();
                        }
                    }, 1000);

                } else if (GREEN_STATUS.equals(str)) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Do something after 1s = 1000ms
                            disconnect();
                            mListener.onGattDisconnect();
                        }
                    }, 1000);

                }

                mListener.onGattWriteCharacteristicSleep();

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    };

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(Activity activity, DanhBluetoothLeDeviceListener listener) {
        mActivity = activity;
        mListener = listener;

        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                mListener.onGattError("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            mListener.onGattError("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            mListener.onGattError("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = STATE_CONNECTING;
//                return true;
//            } else {
//                return false;
//            }
//        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            mListener.onGattError("Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mActivity, false, mGattCallback);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            mListener.onGattError("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */

    public void writeCustomCharacteristic(String value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            mListener.onGattError("BluetoothAdapter not initialized");
            return;
        }

        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(SERVICE_UUID_STR));
        if(mCustomService == null){
            mListener.onGattError("Custom BLE Service not found");
            return;
        }

        /*get the write characteristic from the service*/
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(CHARAC_UUID_STR));

        byte[] bytes = new byte[0];
        try {
            bytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        mWriteCharacteristic.setValue(bytes);
        if(mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
            mListener.onGattError("Failed to write characteristic");
        }
    }
}
