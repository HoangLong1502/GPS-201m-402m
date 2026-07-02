import React from 'react';
import { NavigationContainer, DarkTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { HomeScreen } from './screens/HomeScreen';
import { LeaderboardScreen } from './screens/LeaderboardScreen';
import { LoginScreen } from './screens/LoginScreen';
import { ProfileScreen } from './screens/ProfileScreen';
import { ResultScreen } from './screens/ResultScreen';
import { TrackingScreen } from './screens/TrackingScreen';
import { RouteMapScreen } from './screens/RouteMapScreen';
import { Mode } from './types';
import { useAppContext } from './context/AppContext';

export type RootStackParamList = {
  Login: undefined;
  Home: undefined;
  Profile: undefined;
  Tracking: { mode: Mode };
  Result: undefined;
  RouteMap: undefined;
  Leaderboard: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export const AppNavigator = () => {
  const { user } = useAppContext();

  return (
    <NavigationContainer theme={DarkTheme}>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: '#101010' },
          headerTintColor: '#fff',
          headerTitleStyle: { fontWeight: '800' },
        }}
      >
        {!user ? (
          <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
        ) : (
          <>
            <Stack.Screen name="Home" component={HomeScreen} options={{ title: 'Đường Đua' }} />
            <Stack.Screen name="Profile" component={ProfileScreen} options={{ title: 'Hồ Sơ Tay Đua' }} />
            <Stack.Screen name="Tracking" component={TrackingScreen} options={{ title: 'Theo Dõi Tốc Độ' }} />
            <Stack.Screen name="Result" component={ResultScreen} options={{ title: 'Kết Quả' }} />
            <Stack.Screen name="RouteMap" component={RouteMapScreen} options={{ title: 'Bản đồ hành trình' }} />
            <Stack.Screen name="Leaderboard" component={LeaderboardScreen} options={{ title: 'Bảng Xếp Hạng' }} />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
};
