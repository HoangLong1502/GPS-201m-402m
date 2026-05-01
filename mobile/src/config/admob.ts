import { TestIds } from 'react-native-google-mobile-ads';

const bannerAndroid = process.env.EXPO_PUBLIC_ADMOB_BANNER_ANDROID_ID;

/** Chỉ Android; web dùng AdMobBanner.web.tsx (không load ads). */
export const bannerAdUnitId = bannerAndroid || TestIds.ADAPTIVE_BANNER;
