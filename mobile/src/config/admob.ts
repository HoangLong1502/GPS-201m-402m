const bannerAndroid = process.env.EXPO_PUBLIC_ADMOB_BANNER_ANDROID_ID;

/** ID banner adaptive test chính thức của Google (Android). Không import thư viện ads để tránh crash trên Expo Go. */
export const ANDROID_ADMOB_TEST_ADAPTIVE_BANNER = 'ca-app-pub-3940256099942544/9214589741';

export const bannerAdUnitId = bannerAndroid || ANDROID_ADMOB_TEST_ADAPTIVE_BANNER;
