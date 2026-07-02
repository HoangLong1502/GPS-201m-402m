import AsyncStorage from '@react-native-async-storage/async-storage';
import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { setAuthToken } from '../api';
import { TrackingResult, UserProfile } from '../types';

interface AppContextValue {
  user?: UserProfile;
  token?: string;
  latestResult?: TrackingResult;
  setAuthSession: (user: UserProfile, token: string) => Promise<void>;
  clearAuthSession: () => Promise<void>;
  setUser: (user: UserProfile) => Promise<void>;
  setLatestResult: (result: TrackingResult | undefined) => void;
}

const AppContext = createContext<AppContextValue | undefined>(undefined);
const USER_KEY = 'speed_app_user';
const TOKEN_KEY = 'speed_app_token';

export const AppProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUserState] = useState<UserProfile>();
  const [token, setToken] = useState<string>();
  const [latestResult, setLatestResult] = useState<TrackingResult>();

  useEffect(() => {
    Promise.all([AsyncStorage.getItem(USER_KEY), AsyncStorage.getItem(TOKEN_KEY)]).then(
      ([rawUser, rawToken]) => {
        if (rawUser) {
          setUserState(JSON.parse(rawUser) as UserProfile);
        }
        if (rawToken) {
          setToken(rawToken);
          setAuthToken(rawToken);
        }
      },
    );
  }, []);

  const setAuthSession = async (nextUser: UserProfile, nextToken: string) => {
    setUserState(nextUser);
    setToken(nextToken);
    setAuthToken(nextToken);
    await Promise.all([
      AsyncStorage.setItem(USER_KEY, JSON.stringify(nextUser)),
      AsyncStorage.setItem(TOKEN_KEY, nextToken),
    ]);
  };

  const clearAuthSession = async () => {
    setUserState(undefined);
    setToken(undefined);
    setAuthToken(undefined);
    await Promise.all([AsyncStorage.removeItem(USER_KEY), AsyncStorage.removeItem(TOKEN_KEY)]);
  };

  const setUser = async (value: UserProfile) => {
    setUserState(value);
    await AsyncStorage.setItem(USER_KEY, JSON.stringify(value));
  };

  const value = useMemo(
    () => ({
      user,
      token,
      latestResult,
      setAuthSession,
      clearAuthSession,
      setUser,
      setLatestResult,
    }),
    [user, token, latestResult],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export const useAppContext = () => {
  const ctx = useContext(AppContext);
  if (!ctx) {
    throw new Error('useAppContext must be inside AppProvider');
  }
  return ctx;
};
