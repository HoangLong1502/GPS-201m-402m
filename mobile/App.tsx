import { StatusBar } from 'expo-status-bar';
import React, { useEffect } from 'react';
import { Platform } from 'react-native';
import { AppNavigator } from './src/AppNavigator';
import { AppProvider } from './src/context/AppContext';
import { isGoogleMobileAdsNativeAvailable } from './src/utils/googleMobileAdsNative';

export default function App() {
  useEffect(() => {
    if (Platform.OS === 'web' || !isGoogleMobileAdsNativeAvailable()) return;
    try {
      const { default: mobileAds } = require('react-native-google-mobile-ads');
      mobileAds().initialize().catch(() => undefined);
    } catch {
      /* Expo Go hoặc bản build chưa link ads */
    }
  }, []);

  return (
    <AppProvider>
      <AppNavigator />
      <StatusBar style="light" />
    </AppProvider>
  );
}
