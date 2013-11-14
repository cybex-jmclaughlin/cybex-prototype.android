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
  public static String ELAPSED_SECONDS_ATTR_UUID = "1799649b-7c99-48b1-98cf-0b7dcda597a7";
  public static String METERS_TRAVELED_ATTR_UUID = "45186dd6-06e7-44a2-a5ea-bc9c45b7e2b5";
  public static String METERS_PER_HOUR_ATTR_UUID = "b7cf5c63-9c07-40c7-a6ad-6aa6d8ed031d";
  public static String CALORIES_BURNED_ATTR_UUID = "3d00bef9-375d-40de-88db-f220631bd8a4";
  public static String CALORIES_PER_HOUR_ATTR_UUID = "ac869a9f-9754-44ab-a280-c61b7a6d15be";
  public static String SERIAL_ATTR_UUID = "6e12ade7-11b0-44f7-921a-0c11fb9b2bd1";
  public static String MODEL_ATTR_UUID = "74371ef2-4c10-4494-be1a-0503fc844cc9";
  public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
  public static ArrayList<String> integerServices = new ArrayList<String>();
  static {
    // Sample Services.
    attributes.put(WORKOUT_SERVICE_UUID, "Workout");
    attributes.put(EQUIPMENT_SERVICE_UUID, "Equipment Info");
    // Sample Characteristics.
    attributes.put(ELAPSED_SECONDS_ATTR_UUID, "Elapsed Seconds");
    attributes.put(METERS_PER_HOUR_ATTR_UUID, "Meters per hour");
    attributes.put(METERS_TRAVELED_ATTR_UUID, "Meters Traveled");
    attributes.put(CALORIES_BURNED_ATTR_UUID, "Calories Burned");
    attributes.put(CALORIES_PER_HOUR_ATTR_UUID, "Calories per Hour");
    attributes.put(SERIAL_ATTR_UUID, "Serial");
    attributes.put(MODEL_ATTR_UUID, "Model");
  }
  static{
    integerServices.add(ELAPSED_SECONDS_ATTR_UUID);
    integerServices.add(METERS_TRAVELED_ATTR_UUID);
    integerServices.add(METERS_PER_HOUR_ATTR_UUID);
    integerServices.add(CALORIES_BURNED_ATTR_UUID);
    integerServices.add(CALORIES_PER_HOUR_ATTR_UUID);
  }
  public static String lookup(String uuid, String defaultName) {
    String name = attributes.get(uuid);
    return name == null ? defaultName : name;
  }
}
