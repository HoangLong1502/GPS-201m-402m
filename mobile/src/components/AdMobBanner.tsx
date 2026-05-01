import React from 'react';
import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { bannerAdUnitId } from '../config/admob';
import { isGoogleMobileAdsNativeAvailable } from '../utils/googleMobileAdsNative';

export function AdMobBanner() {
  const insets = useSafeAreaInsets();

  if (!isGoogleMobileAdsNativeAvailable()) {
    return null;
  }

  let BannerAd: typeof import('react-native-google-mobile-ads').BannerAd;
  let BannerAdSize: typeof import('react-native-google-mobile-ads').BannerAdSize;
  try {
    const ads = require('react-native-google-mobile-ads') as typeof import('react-native-google-mobile-ads');
    BannerAd = ads.BannerAd;
    BannerAdSize = ads.BannerAdSize;
  } catch {
    return null;
  }

  return (
    <View style={[styles.bar, { paddingBottom: Math.max(insets.bottom, 6) }]}>
      <BannerAd
        unitId={bannerAdUnitId}
        size={BannerAdSize.LARGE_ANCHORED_ADAPTIVE_BANNER}
        requestOptions={{ requestNonPersonalizedAdsOnly: false }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    width: '100%',
    alignItems: 'center',
    backgroundColor: '#0a0a0a',
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#2a2a2a',
  },
});
