package com.terriblelabs.cyble;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class DeviceControlActivity extends Activity {
  private final static String TAG = DeviceControlActivity.class.getSimpleName();

  public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
  public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

  private String mDeviceName;
  private String mDeviceAddress;
  private BluetoothLeService mBluetoothLeService;
  private boolean mConnected = false;
  private Menu headerMenu;
  public TextView elapsedSecondsOutput;
  public TextView currentMetsOutput;
  public TextView heartRateOutput;
  public TextView caloriesBurnedOutput;

  // Code to manage Service lifecycle.
  private final ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
      mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
      if (!mBluetoothLeService.initialize()) {
        Log.e(TAG, "Unable to initialize Bluetooth");
        finish();
      }
      mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      mBluetoothLeService = null;
    }
  };


  private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
        Log.i("JARVIS", "CONNECTED");
        mConnected = true;
        invalidateOptionsMenu();
        hideConnectAndShowDisconnect();
        gatherGattServices(mBluetoothLeService.getSupportedGattServices());

      } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
      } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
        mConnected = false;
        invalidateOptionsMenu();
        hideDisconnectAndShowConnect();
      } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
        String value =  intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
        String uuid = intent.getStringExtra(BluetoothLeService.CHARACTERISTIC_UPDATE);
        updateViews(uuid, value);
      }
    }
  };



  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.gatt_services_characteristics);

    final Intent intent = getIntent();
    mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
    mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
    bindViews();
    Log.i("JARVIS ABOUT TO CONNECT", "onCreate");
    connectToDevice();
  }

  public void bindViews(){
    ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
    elapsedSecondsOutput = (TextView) findViewById(R.id.elapsedSecondsValue);
    currentMetsOutput = (TextView) findViewById(R.id.currentMetsValue);
    heartRateOutput = (TextView) findViewById(R.id.heartRateValue);
    caloriesBurnedOutput = (TextView) findViewById(R.id.caloriesBurnedValue);

    getActionBar().setTitle(mDeviceName);
    getActionBar().setDisplayHomeAsUpEnabled(true);

  }

  public void connectToDevice(){
    if (mBluetoothLeService == null){
      mBluetoothLeService = new BluetoothLeService();
    }
    mBluetoothLeService.connect(mDeviceAddress);
    Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    if (mBluetoothLeService != null) {
      final boolean result = mBluetoothLeService.connect(mDeviceAddress);
      Log.d(TAG, "Connect request result=" + result);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(mGattUpdateReceiver);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbindService(mServiceConnection);
    mBluetoothLeService = null;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    headerMenu = menu;
    getMenuInflater().inflate(R.menu.gatt_services, menu);
    if (mConnected) {
      hideConnectAndShowDisconnect();
    } else {
      hideDisconnectAndShowConnect();
    }
    return true;
  }

  private void hideConnectAndShowDisconnect(){
    headerMenu.findItem(R.id.menu_connect).setVisible(false);
    headerMenu.findItem(R.id.menu_disconnect).setVisible(true);
    ((TextView) findViewById(R.id.device_state)).setText("Connected");

  }

  private void hideDisconnectAndShowConnect(){
    headerMenu.findItem(R.id.menu_connect).setVisible(true);
    headerMenu.findItem(R.id.menu_disconnect).setVisible(false);
    ((TextView) findViewById(R.id.device_state)).setText("Disconnected");

  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (mBluetoothLeService == null){
      mBluetoothLeService = new BluetoothLeService();
    }
    switch(item.getItemId()) {
      case R.id.menu_connect:
        connectToDevice();
        return true;
      case R.id.menu_disconnect:
        mBluetoothLeService.disconnect();
        return true;
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.subscribe:
        gatherGattServices(mBluetoothLeService.getSupportedGattServices());
        return true;
    }
    return super.onOptionsItemSelected(item);
  }


  private void gatherGattServices(List<BluetoothGattService> gattServices) {
    if (gattServices == null) return;
    // Loops through available GATT Services.
    Log.i("JARVIS - GO THROUGH SERVICES", Integer.toString(gattServices.size()));
    for (BluetoothGattService gattService : gattServices) {
      Log.i("JARVIS - GO THROUGH CHARACTERISTICS PER SERVICE", Integer.toString(gattService.getCharacteristics().size()));
      for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
        Log.i("JARVIS - Starting to subscribe", gattCharacteristic.getUuid().toString());
        BluetoothGattCharacteristic characteristic = gattCharacteristic;
        subscribeToNotifiable(characteristic);
      }
    }
  }


  private void subscribeToNotifiable(BluetoothGattCharacteristic characteristic){
    if (GattAttributes.notifiableServices.contains(characteristic.getUuid().toString().toUpperCase())){
      Log.i("JARVIS - THE RAW CHARACTERISTIC", characteristic.getUuid().toString().toUpperCase());
      mBluetoothLeService.setCharacteristicNotification(characteristic, true);
    } else {
      Log.i("JARVIS - NOT NOTIFIABLE", characteristic.getUuid().toString());
    }

  }


  private void updateViews(String uuid, String value){
    Log.i("JARVIS - UPDATING VIEW", value);
    if (GattAttributes.ELAPSED_SECONDS_ATTR_UUID.equals(uuid.toUpperCase())){
      elapsedSecondsOutput.setText(value);
    }else if (GattAttributes.CALORIES_BURNED_ATTR_UUID.equals(uuid.toUpperCase())){
      caloriesBurnedOutput.setText(value);
    }else if (GattAttributes.CURRENT_METS_ATTR_UUID.equals(uuid.toUpperCase())){
      currentMetsOutput.setText(value);
    }else if (GattAttributes.CURRENT_HEART_RATE_ATTR_UUID.equals(uuid.toUpperCase())){
      heartRateOutput.setText(value);
    }
  }

  private static IntentFilter makeGattUpdateIntentFilter() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
    intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
    intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
    return intentFilter;
  }
}
