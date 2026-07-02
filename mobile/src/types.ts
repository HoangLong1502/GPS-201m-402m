export type BackendMode = 'GPS' | 'RACE_201' | 'RACE_402';
export type Mode = BackendMode | 'STOPWATCH';
export type LeaderboardMode = BackendMode;

export interface UserProfile {
  id: string;
  phoneNumber?: string | null;
  displayName?: string | null;
  avatar?: string | null;
  vehicleName?: string | null;
  engineType?: string | null;
  createdAt: string;
}

export interface TrackingResult {
  mode: Mode;
  maxSpeed: number;
  time: number;
  distance: number;
  avgSpeed?: number;
  path?: Array<{ latitude: number; longitude: number }>;
}

export interface LeaderboardItem {
  id: string;
  mode: BackendMode;
  maxSpeed: number;
  time: number;
  distance: number;
  createdAt: string;
  user: UserProfile;
}

export interface AuthResponse {
  accessToken: string;
  user: UserProfile;
}
