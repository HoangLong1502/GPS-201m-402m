/**
 * AdMob App ID Android (ca-app-pub-xxxxxxxx~yyyyyyyyyy) — AdMob → App settings.
 * .env: EXPO_PUBLIC_ADMOB_ANDROID_APP_ID
 * Mặc định: ID test Google (dev).
 * Chỉ build Android .apk — không cấu hình iOS.
 */
module.exports = ({ config }) => {
  const androidAppId =
    process.env.EXPO_PUBLIC_ADMOB_ANDROID_APP_ID || 'ca-app-pub-3940256099942544~3347511713';

  const plugins = (config.plugins || []).filter(
    (p) =>
      p !== 'react-native-google-mobile-ads' &&
      !(Array.isArray(p) && p[0] === 'react-native-google-mobile-ads'),
  );

  return {
    ...config,
    plugins: [...plugins, ['react-native-google-mobile-ads', { androidAppId }]],
  };
};
