package com.terriblelabs.cyble;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLeService extends Service {
  private final static String TAG = BluetoothLeService.class.getSimpleName();

  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter;
  private String mBluetoothDeviceAddress;
  private BluetoothGatt mBluetoothGatt;
  private int mConnectionState = STATE_DISCONNECTED;
  ArrayBlockingQueue<BluetoothGattService> mServiceToSubscribe = new ArrayBlockingQueue<BluetoothGattService>(12);
  private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
  private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();


  private static final int STATE_DISCONNECTED = 0;
  private static final int STATE_CONNECTING = 1;
  private static final int STATE_CONNECTED = 2;

  public final static String ACTION_GATT_CONNECTED =
      "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
  public final static String ACTION_GATT_DISCONNECTED =
      "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
  public final static String ACTION_GATT_SERVICES_DISCOVERED =
      "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
  public final static String ACTION_DATA_AVAILABLE =
      "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
  public final static String EXTRA_DATA =
      "com.example.bluetooth.le.EXTRA_DATA";
  public final static String CHARACTERISTIC_UPDATE =
      "UpdateForCharacteristicFound";

  private void processService() {
    if (!mServiceToSubscribe.isEmpty()) {
      BluetoothGattService service = null;
      try {
        service = mServiceToSubscribe.take();
        UUID uuid = service.getUuid();
        if (uuid.equals(GattAttributes.WORKOUT_SERVICE_UUID)) {
          BluetoothGattCharacteristic elapsedSecondsCharacteristic = service.getCharacteristic(GattAttributes.ELAPSED_SECONDS_ATTR_UUID);
          enableNotificationForService(true, mBluetoothGatt, elapsedSecondsCharacteristic);
          BluetoothGattCharacteristic caloriesBurnedCharacteristic = service.getCharacteristic(GattAttributes.CALORIES_BURNED_ATTR_UUID);
          enableNotificationForService(true, mBluetoothGatt, caloriesBurnedCharacteristic);
          BluetoothGattCharacteristic currentHeartRateCharacteristic = service.getCharacteristic(GattAttributes.CURRENT_HEART_RATE_ATTR_UUID);
          enableNotificationForService(true, mBluetoothGatt, currentHeartRateCharacteristic);
          BluetoothGattCharacteristic currentMetsCharacteristic = service.getCharacteristic(GattAttributes.CURRENT_METS_ATTR_UUID);
          enableNotificationForService(true, mBluetoothGatt, currentMetsCharacteristic);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  private void enableNotificationForService(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic dataCharacteristic) {
    gatt.setCharacteristicNotification(dataCharacteristic, enable);
    BluetoothGattDescriptor descriptor = dataCharacteristic.getDescriptor(GattAttributes.CLIENT_CONFIG_UUID);

    descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    writeGattDescriptor(descriptor);
  }

  public void writeGattDescriptor(BluetoothGattDescriptor d){
    descriptorWriteQueue.add(d);
        if(descriptorWriteQueue.size() == 1){
          mBluetoothGatt.writeDescriptor(d);
        }

  }

  // Implements callback methods for GATT events that the app cares about.  For example,
  // connection change and services discovered.
  private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      String intentAction;
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        intentAction = ACTION_GATT_CONNECTED;
        Log.i("JARVIS", "CONNECTED");
        mConnectionState = STATE_CONNECTED;
        if (mBluetoothGatt.discoverServices()){
          broadcastUpdate(intentAction);
        }


      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.i("JARVIS", "DISCONNECTED");
        intentAction = ACTION_GATT_DISCONNECTED;
        mConnectionState = STATE_DISCONNECTED;
        Log.i(TAG, "Disconnected from GATT server.");
        broadcastUpdate(intentAction);
      }
    }
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.d("JARVIS", "Callback: Wrote GATT Descriptor successfully.");
      }
      else{
        Log.d("JARVIS", "Callback: Error writing GATT Descriptor: "+ status);
      }
      descriptorWriteQueue.remove();  //pop the item that we just finishing writing
      //if there is more to write, do it!
      if(descriptorWriteQueue.size() > 0)
        mBluetoothGatt.writeDescriptor(descriptorWriteQueue.element());
      else if(characteristicReadQueue.size() > 0)
        mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
    };

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
          UUID uuid = service.getUuid();

          try {
            if (uuid.equals(GattAttributes.WORKOUT_SERVICE_UUID)) {
              mServiceToSubscribe.put(service);
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

        }
        processService();
        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

      } else {
        Log.w(TAG, "onServicesDiscovered received: " + status);
      }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
      characteristicReadQueue.remove();
      if (status == BluetoothGatt.GATT_SUCCESS) {
        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
      }
      else{
        Log.d(TAG, "onCharacteristicRead error: " + status);
      }

      if(characteristicReadQueue.size() > 0){
        mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
      }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
      Log.i("JARVIS - CHARACTERISTIC CHANGED", characteristic.getUuid().toString());
      broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
    }
  };

  private void broadcastUpdate(final String action) {
    final Intent intent = new Intent(action);
    sendBroadcast(intent);
  }

  private void broadcastUpdate(final String action,
                               final BluetoothGattCharacteristic characteristic) {
    final Intent intent = new Intent(action);
    final UUID uuid = characteristic.getUuid();
    if (uuid.equals(GattAttributes.ELAPSED_SECONDS_ATTR_UUID)
      || uuid.equals(GattAttributes.CALORIES_BURNED_ATTR_UUID)
      || uuid.equals(GattAttributes.CURRENT_HEART_RATE_ATTR_UUID)
      || uuid.equals(GattAttributes.CURRENT_METS_ATTR_UUID)){
      int format = BluetoothGattCharacteristic.FORMAT_UINT32;
      int value = characteristic.getIntValue(format, 0);
      intent.putExtra(CHARACTERISTIC_UPDATE, characteristic.getUuid().toString());
      intent.putExtra(EXTRA_DATA, String.valueOf(value));
    } else {
      // For all other profiles, writes the data formatted in HEX.
      final byte[] data = characteristic.getValue();
      if (data != null && data.length > 0) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for(byte byteChar : data)
          stringBuilder.append(String.format("%02X ", byteChar));
        intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
      }
    }
    sendBroadcast(intent);
  }

  public class LocalBinder extends Binder {
    BluetoothLeService getService() {
      return BluetoothLeService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    // After using a given device, you should make sure that BluetoothGatt.close() is called
    // such that resources are cleaned up properly.  In this particular example, close() is
    // invoked when the UI is disconnected from the Service.
    close();
    return super.onUnbind(intent);
  }

  private final IBinder mBinder = new LocalBinder();

  /**
   * Initializes a reference to the local Bluetooth adapter.
   *
   * @return Return true if the initialization is successful.
   */
  public boolean initialize() {
    // For API level 18 and above, get a reference to BluetoothAdapter through
    // BluetoothManager.
    if (mBluetoothManager == null) {
      mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
      if (mBluetoothManager == null) {
        Log.e(TAG, "Unable to initialize BluetoothManager.");
        return false;
      }
    }

    mBluetoothAdapter = mBluetoothManager.getAdapter();
    if (mBluetoothAdapter == null) {
      Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
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
      Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
      return false;
    }

    final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    if (device == null) {
      Log.w(TAG, "Device not found.  Unable to connect.");
      return false;
    }
    // We want to directly connect to the device, so we are setting the autoConnect
    // parameter to false.
    mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    Log.d(TAG, "Trying to create a new connection.");
    mBluetoothDeviceAddress = address;
    mConnectionState = STATE_CONNECTING;
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
      Log.w(TAG, "BluetoothAdapter not initialized");
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
   * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
   * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
   * callback.
   *
   * @param characteristic The characteristic to read from.
   */
  public void readCharacteristic(BluetoothGattCharacteristic characteristic) {

    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized");
      return;
    }
    characteristicReadQueue.add(characteristic);
    //if there is only 1 item in the queue, then read it.  If more than 1, we handle asynchronously in the callback above
    //GIVE PRECEDENCE to descriptor writes.  They must all finish first.
    if((characteristicReadQueue.size() == 1) && (descriptorWriteQueue.size() == 0))
      mBluetoothGatt.readCharacteristic(characteristic);

  }

  /**
   * Retrieves a list of supported GATT services on the connected device. This should be
   * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
   *
   * @return A {@code List} of supported services.
   */
  public List<BluetoothGattService> getSupportedGattServices() {
    if (mBluetoothGatt == null) return null;
    if (mBluetoothGatt == null) return null;

    return mBluetoothGatt.getServices();
  }
}
