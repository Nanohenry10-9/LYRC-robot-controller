package LYRC.TEAM01.robot_controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

public class BLEUtils {
    public BluetoothAdapter btAdapter;
    public BluetoothLeScanner btScanner;

    public BluetoothGatt deviceGatt;

    public String deviceMAC = "00:1B:10:FB:78:8B";
    private BluetoothDevice device;

    private boolean dataReceived = false, connected = false;

    Context context;

    BLEUtils(BluetoothManager btm, Context c) {
        btAdapter = btm.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        context = c;
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
        if (result.getDevice().getAddress().equals(deviceMAC) && !connected) {
            CanvasController.log("Device found");
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    btScanner.stopScan(leScanCallback);
                }
            });
            device = result.getDevice();
            device.connectGatt(context, true, gattCallback);
            stopScanning();
        }
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == STATE_CONNECTED && !connected){
                deviceGatt = gatt;
                connected = true;
                CanvasController.log("Device connected");
                gatt.discoverServices();
            } else if (newState == STATE_DISCONNECTED && connected) {
                connected = false;
                CanvasController.log("Connection dropped, retrying...");
                startScanning();
            }
        }

        /*@Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
            byte r[] = ch.getValue();
            CanvasController.log("Received: " + r);
        }*/

        /*@Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //gatt.setCharacteristicNotification(characteristicForProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE),true);
            }
        }*/
    };

    public void sendData(byte[] b) {
        if (deviceGatt == null) {
            return;
        }
        BluetoothGattCharacteristic ch = characteristicForProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
        if (ch != null) {
            ch.setValue(b);
            deviceGatt.writeCharacteristic(ch);
        }
    }

    private BluetoothGattCharacteristic characteristicForProperty(int property) {
        List<BluetoothGattService> list = deviceGatt.getServices();
        if (list == null) {
            return null;
        }
        for (BluetoothGattService gattService : list) {
            String uuid = gattService.getUuid().toString();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                int properties = gattCharacteristic.getProperties();
                int p = (property & properties);
                int np = property;
                if (np == p) {
                    if (np == (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ)) {
                        if (uuid.indexOf("ffe4") > 0) {
                            return gattCharacteristic;
                        }
                    } else {
                        return gattCharacteristic;
                    }
                }
            }
        }
        return null;
    }

    public void startScanning() {
        CanvasController.log("BLE scan started");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        CanvasController.log("BLE scan stopped");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }
}
