package com.terriblelabs.cyble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
  private static HashMap<String, String> attributes = new HashMap();
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
  public static UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
  public static ArrayList<String> integerServices = new ArrayList<String>();
  public static ArrayList<String> stringServices = new ArrayList<String>();
  public static ArrayList<String> notifiableServices = new ArrayList<String>();
  static {
    // Sample Services.
;
  }
  public static String lookup(String uuid, String defaultName) {
    String name = attributes.get(uuid);
    return name == null ? defaultName : name;
  }
}