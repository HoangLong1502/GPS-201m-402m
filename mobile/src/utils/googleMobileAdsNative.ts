import { NativeModules, Platform } from 'react-native';

/** Expo Go không có native module → false. Dev build / APK có link ads → true. */
export function isGoogleMobileAdsNativeAvailable(): boolean {
  if (Platform.OS === 'web') return false;
  return NativeModules.RNGoogleMobileAdsModule != null;
}
