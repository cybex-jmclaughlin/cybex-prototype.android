package com.terriblelabs.cyble;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
  private static HashMap<String, String> attributes = new HashMap();
  public static String WORKOUT_SERVICE_UUID = "1ca931a8-6a77-4e4d-aad8-5ca168163ba6";
  public static String EQUIPMENT_SERVICE_UUID = "5748216d-3c4a-491e-9138-467824e8f270";
  public static String ELAPSED_SECONDS_ATTR_UUID = "1799649B-7C99-48B1-98CF-0B7DCDA597A7";
  public static String METERS_TRAVELED_ATTR_UUID = "45186DD6-06E7-44A2-A5EA-BC9C45B7E2B5";
  public static String METERS_PER_HOUR_ATTR_UUID = "B7CF5C63-9C07-40C7-A6AD-6AA6D8ED031D";
  public static String CALORIES_BURNED_ATTR_UUID = "3D00BEF9-375D-40DE-88DB-F220631BD8A4";
  public static String CALORIES_PER_HOUR_ATTR_UUID = "AC869A9F-9754-44AB-A280-C61B7A6D15BE";
  public static String CURRENT_POWER_ATTR_UUID  = "6E1EA3E8-CF5E-45C5-A61C-2F338220A77F";
  public static String CURRENT_HEART_RATE_ATTR_UUID = "C9F0DCBF-DD99-4282-B74B-AC44BB5C013E";
  public static String STRIDES_PER_MINUTE_ATTR_UUID = "065806B9-7AC6-4DCC-B42C-96BB712E0CEB";
  public static String CURRENT_METS_ATTR_UUID = "E4A234EA-DC68-4B07-B435-485B9B3406FD";
  public static String SERIAL_ATTR_UUID = "6E12ADE7-11B0-44F7-921A-0C11FB9B2BD1";
  public static String MODEL_ATTR_UUID = "74371EF2-4C10-4494-BE1A-0503FC844CC9";
  public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
  public static ArrayList<String> integerServices = new ArrayList<String>();
  public static ArrayList<String> stringServices = new ArrayList<String>();
  public static ArrayList<String> notifiableServices = new ArrayList<String>();
  static {
    // Sample Services.
    attributes.put(WORKOUT_SERVICE_UUID, "Workout");
    attributes.put(EQUIPMENT_SERVICE_UUID, "Equipment Info");
    // Sample Characteristics.
    attributes.put(ELAPSED_SECONDS_ATTR_UUID, "Elapsed Seconds");
    attributes.put(METERS_PER_HOUR_ATTR_UUID, "Meters per hour");
    attributes.put(METERS_TRAVELED_ATTR_UUID, "Meters Traveled");
    attributes.put(CURRENT_HEART_RATE_ATTR_UUID, "Current Heart Rate");
    attributes.put(CURRENT_METS_ATTR_UUID, "Current Mets");
    attributes.put(CURRENT_POWER_ATTR_UUID, "Current Power Generated");
    attributes.put(CALORIES_BURNED_ATTR_UUID, "Calories Burned");
    attributes.put(CALORIES_PER_HOUR_ATTR_UUID, "Calories per Hour");
    attributes.put(STRIDES_PER_MINUTE_ATTR_UUID, "Strides Per Minute");
    attributes.put(SERIAL_ATTR_UUID, "Serial");
    attributes.put(MODEL_ATTR_UUID, "Model");

    integerServices.add(ELAPSED_SECONDS_ATTR_UUID);
    integerServices.add(METERS_TRAVELED_ATTR_UUID);
    integerServices.add(METERS_PER_HOUR_ATTR_UUID);
    integerServices.add(CALORIES_BURNED_ATTR_UUID);
    integerServices.add(CURRENT_METS_ATTR_UUID);
    integerServices.add(CURRENT_HEART_RATE_ATTR_UUID);
    integerServices.add(CALORIES_PER_HOUR_ATTR_UUID);
    integerServices.add(CURRENT_POWER_ATTR_UUID);


    stringServices.add(SERIAL_ATTR_UUID);
    stringServices.add(MODEL_ATTR_UUID);

    notifiableServices.add(ELAPSED_SECONDS_ATTR_UUID);
    notifiableServices.add(CURRENT_METS_ATTR_UUID);
    notifiableServices.add(CALORIES_BURNED_ATTR_UUID);
    notifiableServices.add(CURRENT_HEART_RATE_ATTR_UUID);
  }
  public static String lookup(String uuid, String defaultName) {
    String name = attributes.get(uuid);
    return name == null ? defaultName : name;
  }
}