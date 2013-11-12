package com.terriblelabs.cyble;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)

public class MainActivity extends ListActivity {
  private final static int REQUEST_ENABLE_BT = 1;
  private BluetoothAdapter mBluetoothAdapter;
  private boolean mScanning;
  private Handler mHandler;
  private LeDeviceListAdapter mLeDeviceAdapter;

  // Stops scanning after 10 seconds.
  private static final long SCAN_PERIOD = 10000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    checkBTLE();
    mLeDeviceAdapter = new LeDeviceListAdapter();
    setupAdapter();
    setupBluetooth();
    setupHandler();
    setContentView(R.layout.activity_main);
    bindViews();
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.device_scan, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    switch (item.getItemId()){
      case R.id.scan_button:
        scanLeDevice(true);
        return true;
      default:
        return true;
    }
  }
  private void bindViews(){
  }

  private void checkBTLE(){
    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
      Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
      finish();
    }
  }
  private void setupAdapter(){
    setListAdapter(mLeDeviceAdapter);

  }
  private void setupHandler(){
    mHandler = new Handler();
  }

  private void setupBluetooth(){
    // Initializes Bluetooth adapter.
    final BluetoothManager bluetoothManager =
        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();
    // Ensures Bluetooth is available on the device and it is enabled. If not,
// displays a dialog requesting user permission to enable Bluetooth.
    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }



  @Override
  public void onListItemClick(ListView l, View v, int position, long thisID){
    final BluetoothDevice device = mLeDeviceAdapter.getDevice(position);
    if (device == null) return;
    final Intent intent = new Intent(this, DeviceControlActivity.class);
    intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
    intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
    if (mScanning) {
      mBluetoothAdapter.stopLeScan(mLeScanCallback);
      mScanning = false;
    }
    startActivity(intent);
  }


  private void scanLeDevice(final boolean enable) {
    if (enable) {
      // Stops scanning after a pre-defined scan period.
      mHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          mScanning = false;
          mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
      }, SCAN_PERIOD);

      mScanning = true;
      mBluetoothAdapter.startLeScan(mLeScanCallback);
    } else {
      mScanning = false;
      mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }
  }

  private BluetoothAdapter.LeScanCallback mLeScanCallback =
      new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Log.i("Device Found, I guess!", "RUN");
              mLeDeviceAdapter.addDevice(device);
              mLeDeviceAdapter.notifyDataSetChanged();
            }
          });
        }
      };

  @Override
  public void onResume(){
    super.onResume();
  }

  // Adapter for holding devices found through scanning.
  private class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflator;

    public LeDeviceListAdapter() {
      super();
      mLeDevices = new ArrayList<BluetoothDevice>();
      mInflator = MainActivity.this.getLayoutInflater();
    }

    public void addDevice(BluetoothDevice device) {
      if(!mLeDevices.contains(device)) {
        mLeDevices.add(device);
      }
    }

    public BluetoothDevice getDevice(int position) {
      return mLeDevices.get(position);
    }

    public void clear() {
      mLeDevices.clear();
    }

    @Override
    public int getCount() {
      return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
      return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
      return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      ViewHolder viewHolder;
      // General ListView optimization code.
      if (view == null) {
        view = mInflator.inflate(R.layout.ble_item_row, null);
        viewHolder = new ViewHolder();
        viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
        viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
        view.setTag(viewHolder);
      } else {
        viewHolder = (ViewHolder) view.getTag();
      }

      BluetoothDevice device = mLeDevices.get(i);
      final String deviceName = device.getName();
      if (deviceName != null && deviceName.length() > 0)
        viewHolder.deviceName.setText(deviceName);
      else
        viewHolder.deviceName.setText(R.string.unknown_device);
      viewHolder.deviceAddress.setText(device.getAddress());

      return view;
    }
  }

  private class ViewHolder{
    TextView deviceAddress;
    TextView deviceName;
  }


}
