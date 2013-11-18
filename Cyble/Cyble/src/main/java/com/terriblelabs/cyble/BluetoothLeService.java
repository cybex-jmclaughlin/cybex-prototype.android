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

  private static final int STATE_DISCONNECTED = 0;
  private static final int STATE_CONNECTING = 1;
  private static final int STATE_CONNECTED = 2;

  public static final byte[] SERVICE_ENABLED = {0x01};
  public static final byte[] SERVICE_DISABLED = {0x00};


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

  public static UUID WORKOUT_SERVICE_UUID = UUID.fromString("1ca931a8-6a77-4e4d-aad8-5ca168163ba6");
  public static UUID EQUIPMENT_SERVICE_UUID = UUID.fromString("5748216d-3c4a-491e-9138-467824e8f270");
  public static UUID ELAPSED_SECONDS_ATTR_UUID = UUID.fromString("1799649B-7C99-48B1-98CF-0B7DCDA597A7");
  public static UUID METERS_TRAVELED_ATTR_UUID = UUID.fromString("45186DD6-06E7-44A2-A5EA-BC9C45B7E2B5");
  public static UUID METERS_PER_HOUR_ATTR_UUID = UUID.fromString("B7CF5C63-9C07-40C7-A6AD-6AA6D8ED031D");
  public static UUID CALORIES_BURNED_ATTR_UUID = UUID.fromString("3D00BEF9-375D-40DE-88DB-F220631BD8A4");
  public static UUID CALORIES_PER_HOUR_ATTR_UUID = UUID.fromString("AC869A9F-9754-44AB-A280-C61B7A6D15BE");
  public static UUID CURRENT_POWER_ATTR_UUID  = UUID.fromString("6E1EA3E8-CF5E-45C5-A61C-2F338220A77F");
  public static UUID CURRENT_HEART_RATE_ATTR_UUID = UUID.fromString("C9F0DCBF-DD99-4282-B74B-AC44BB5C013E");
  public static UUID STRIDES_PER_MINUTE_ATTR_UUID = UUID.fromString("065806B9-7AC6-4DCC-B42C-96BB712E0CEB");
  public static UUID CURRENT_METS_ATTR_UUID = UUID.fromString("E4A234EA-DC68-4B07-B435-485B9B3406FD");
  public static UUID SERIAL_ATTR_UUID = UUID.fromString("6E12ADE7-11B0-44F7-921A-0C11FB9B2BD1");
  public static UUID MODEL_ATTR_UUID = UUID.fromString("74371EF2-4C10-4494-BE1A-0503FC844CC9");
  public static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
  private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();

  ArrayBlockingQueue<BluetoothGattService> mServiceToSubscribe = new ArrayBlockingQueue<BluetoothGattService>(12);

  //public final static UUID UUID_HEART_RATE_MEASUREMENT =
      //UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

  // Implements callback methods for GATT events that the app cares about.  For example,
  // connection change and services discovered.

  private void processService(){
    if (!mServiceToSubscribe.isEmpty()){
      BluetoothGattService service = null;
      try {
        service = mServiceToSubscribe.take();
        UUID uuid = service.getUuid();
        if (uuid.equals(WORKOUT_SERVICE_UUID)){
          BluetoothGattCharacteristic elapsedSecondsCharacteristic = service.getCharacteristic(ELAPSED_SECONDS_ATTR_UUID);
          BluetoothGattCharacteristic caloriesBurnedCharacteristic = service.getCharacteristic(CALORIES_BURNED_ATTR_UUID);
          BluetoothGattCharacteristic currentMetsCharacteristc = service.getCharacteristic(CURRENT_METS_ATTR_UUID);
          BluetoothGattCharacteristic currentHeartRateCharacteristic = service.getCharacteristic(CURRENT_HEART_RATE_ATTR_UUID);
          enableNotificationForService(true, mBluetoothGatt, currentHeartRateCharacteristic);
          enableNotificationForService(true, mBluetoothGatt, elapsedSecondsCharacteristic);
          enableNotificationForService(true, mBluetoothGatt, currentMetsCharacteristc);
          enableNotificationForService(true, mBluetoothGatt, caloriesBurnedCharacteristic);
        }

      } catch (InterruptedException e){
        e.printStackTrace();
      }
    }
  }

    private void enableNotificationForService(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic dataCharacteristic) {
      gatt.setCharacteristicNotification(dataCharacteristic, enable);
      BluetoothGattDescriptor descriptor = dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
      descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
      writeDescriptor(descriptor);
    }

    private void writeDescriptor(BluetoothGattDescriptor d){
      //put the descriptor into the write queue
      descriptorWriteQueue.add(d);
      //if there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
      if(descriptorWriteQueue.size() == 1){
        mBluetoothGatt.writeDescriptor(d);
      }
    }

  public void readCharacteristic(String characteristicName) {
    if (mBluetoothAdapter == null || mBluetoothGatt == null) {
      Log.w(TAG, "BluetoothAdapter not initialized");
      return;
    }
    BluetoothGattService s = mBluetoothGatt.getService(WORKOUT_SERVICE_UUID);
    BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(characteristicName));
    //put the characteristic into the read queue
    characteristicReadQueue.add(c);
    //if there is only 1 item in the queue, then read it.  If more than 1, we handle asynchronously in the callback above
    //GIVE PRECEDENCE to descriptor writes.  They must all finish first.
    if((characteristicReadQueue.size() == 1) && (descriptorWriteQueue.size() == 0))
      mBluetoothGatt.readCharacteristic(c);
  }
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      String intentAction;
      Log.i("JARVIS - STATE CHANGED", "HOPEFULLY WORKING");
      if (newState == BluetoothProfile.STATE_CONNECTED) {
        intentAction = ACTION_GATT_CONNECTED;
        mConnectionState = STATE_CONNECTED;
        if (mBluetoothGatt.discoverServices()){
          broadcastUpdate(intentAction);
        }


      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        intentAction = ACTION_GATT_DISCONNECTED;
        mConnectionState = STATE_DISCONNECTED;
        Log.i(TAG, "Disconnected from GATT server.");
        broadcastUpdate(intentAction);
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        // Set notifications on this service.
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
          UUID uuid = service.getUuid();

          try {
            if (uuid.equals(WORKOUT_SERVICE_UUID)) {
              mServiceToSubscribe.put(service);
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

        }
        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
        processService();
      } else {
        Log.w(TAG, "onServicesDiscovered received: " + status);
      }
    }


    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.d(TAG, "Callback: Wrote GATT Descriptor successfully.");
      }
      else{
        Log.d(TAG, "Callback: Error writing GATT Descriptor: "+ status);
      }
      descriptorWriteQueue.remove();  //pop the item that we just finishing writing
      //if there is more to write, do it!
      if(descriptorWriteQueue.size() > 0)
        mBluetoothGatt.writeDescriptor(descriptorWriteQueue.element());
      else if(characteristicReadQueue.size() > 0)
        mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
    };

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

      if(characteristicReadQueue.size() > 0)
        mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
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
    UUID uuid = characteristic.getUuid();
    if (uuid.equals(CURRENT_HEART_RATE_ATTR_UUID)
    || uuid.equals(CALORIES_BURNED_ATTR_UUID)
    || uuid.equals(ELAPSED_SECONDS_ATTR_UUID)
    || uuid.equals(CURRENT_METS_ATTR_UUID)
     ) {
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
    initialize();
    if (mBluetoothAdapter == null || address == null) {
      Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
      return false;
    }

    // Previously connected device.  Try to reconnect.
    if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
        && mBluetoothGatt != null) {
      Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
      if (mBluetoothGatt.connect()) {
        mConnectionState = STATE_CONNECTING;
        return true;
      } else {
        return false;
      }
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
