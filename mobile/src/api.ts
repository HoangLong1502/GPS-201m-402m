import axios from 'axios';
import { Platform } from 'react-native';
import { AuthResponse, BackendMode, LeaderboardItem, LeaderboardMode, Mode, TrackingResult, UserProfile } from './types';

export const API_BASE_URL =
  Platform.OS === 'android'
    ? 'http://10.0.2.2:3000'
    : 'http://localhost:3000';

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 12000,
});

export const setAuthToken = (token?: string) => {
  if (token) {
    api.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete api.defaults.headers.common.Authorization;
  }
};

export const upsertUser = async (
  payload: FormData | Record<string, string>,
  userId?: string,
): Promise<UserProfile> => {
  const isFormData = payload instanceof FormData;
  const config = isFormData
    ? { headers: { 'Content-Type': 'multipart/form-data' } }
    : undefined;
  if (userId) {
    const { data } = await api.put<UserProfile>(`/users/${userId}`, payload, config);
    return data;
  }
  const { data } = await api.post<UserProfile>('/users', payload, config);
  return data;
};

export const registerByPhone = async (
  phoneNumber: string,
  password: string,
  displayName: string,
): Promise<AuthResponse> => {
  const { data } = await api.post<AuthResponse>('/auth/register-phone', {
    phoneNumber,
    password,
    displayName,
  });
  return data;
};

export const loginByPhone = async (
  phoneNumber: string,
  password: string,
): Promise<AuthResponse> => {
  const { data } = await api.post<AuthResponse>('/auth/login-phone', {
    phoneNumber,
    password,
  });
  return data;
};

export const submitResult = async (result: TrackingResult): Promise<void> => {
  const backendMode: BackendMode =
    result.mode === 'STOPWATCH' ? 'GPS' : (result.mode as BackendMode);
  await api.post('/results', {
    mode: backendMode,
    maxSpeed: result.maxSpeed,
    time: result.time,
    distance: result.distance,
  });
};

export const fetchLeaderboard = async (
  mode: LeaderboardMode,
  vehicleName?: string,
): Promise<LeaderboardItem[]> => {
  const path = vehicleName ? '/results/vehicle' : '/results/global';
  const { data } = await api.get<LeaderboardItem[]>(path, {
    params: { mode, vehicleName },
  });
  return data;
};
