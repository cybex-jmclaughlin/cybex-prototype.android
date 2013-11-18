package com.terriblelabs.cyble;

import android.annotation.TargetApi;
import android.app.Activity;
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
  private boolean mIsConnected = false;
  private Menu headerMenu;
  public TextView elapsedSecondsOutput;
  public TextView currentMetsOutput;
  public TextView heartRateOutput;
  public TextView caloriesBurnedOutput;
  private boolean mIsBound = false;


  // Code to manage Service lifecycle.
  private final ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
      BluetoothLeService.LocalBinder binder = (BluetoothLeService.LocalBinder) service;
      mBluetoothLeService = binder.getService();
      mIsBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      mBluetoothLeService = null;
      mIsBound = false;
    }
  };



  public class GattReceiver extends BroadcastReceiver {
    public GattReceiver() {
      // Android needs the empty constructor.
    }

    @Override
    public void onReceive(Context context, Intent intent){
      final String action = intent.getAction();
      if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
        invalidateOptionsMenu();
        hideConnectAndShowDisconnect();
        mIsConnected = true;
      } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
        invalidateOptionsMenu();
        hideDisconnectAndShowConnect();
        mIsConnected = false;
      } else if (BluetoothLeService.
          ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
      } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
        String value =  intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
        String uuid = intent.getStringExtra(BluetoothLeService.CHARACTERISTIC_UPDATE);
        updateViews(uuid, value);
      }
    }
  };

  void doUnbindService() {
    if (mIsBound) {
      // Detach our existing connection.
      unbindService(mServiceConnection);
      mIsBound = false;
    }
  }
    private void registerReceiver() {
      final IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
      intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
      intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
      intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
      GattReceiver receiver = new GattReceiver();
      registerReceiver(receiver, intentFilter);
    }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onStop() {
    super.onStop();

    if (mIsBound) {
      doUnbindService();
      mIsBound = false;
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.gatt_services_characteristics);
    final Intent intent = getIntent();
    mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
    mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
    // Always need to register our receiver.
    registerReceiver();
    bindViews();
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
    Log.i("JARVIS ABOUT TO CONNECT", "onCreate");
    if (mBluetoothLeService == null){
      mBluetoothLeService = new BluetoothLeService();
    }
    mBluetoothLeService.connect(mDeviceAddress);
    Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    headerMenu = menu;
    getMenuInflater().inflate(R.menu.gatt_services, menu);
    if (mIsConnected) {
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
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  // Connect to the loca
  private void updateViews(String uuid, String value){
    Log.i("JARVIS - UPDATING VIEW", value);
    if (mBluetoothLeService.ELAPSED_SECONDS_ATTR_UUID.equals(uuid.toUpperCase())){
      elapsedSecondsOutput.setText(value);
    }else if (mBluetoothLeService.CALORIES_BURNED_ATTR_UUID.equals(uuid.toUpperCase())){
      caloriesBurnedOutput.setText(value);
    }else if (mBluetoothLeService.CURRENT_METS_ATTR_UUID.equals(uuid.toUpperCase())){
      currentMetsOutput.setText(value);
    }else if (mBluetoothLeService.CURRENT_HEART_RATE_ATTR_UUID.equals(uuid.toUpperCase())){
      heartRateOutput.setText(value);
    }
  }

}
