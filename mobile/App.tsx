import { StatusBar } from 'expo-status-bar';
import React, { useEffect } from 'react';
import { Platform } from 'react-native';
import { AppNavigator } from './src/AppNavigator';
import { AppProvider } from './src/context/AppContext';

export default function App() {
  useEffect(() => {
    if (Platform.OS === 'web') return;
    const { default: mobileAds } = require('react-native-google-mobile-ads');
    mobileAds().initialize().catch(() => undefined);
  }, []);

  return (
    <AppProvider>
      <AppNavigator />
      <StatusBar style="light" />
    </AppProvider>
  );
}
