import { LocationObjectCoords } from 'expo-location';

export const targetDistanceByMode = (mode: 'GPS' | 'RACE_201' | 'RACE_402' | 'STOPWATCH') => {
  if (mode === 'RACE_201') {
    return 201;
  }
  if (mode === 'RACE_402') {
    return 402;
  }
  if (mode === 'STOPWATCH') {
    return Infinity;
  }
  return Infinity;
};

export const formatTime = (seconds: number) => `${seconds.toFixed(2)}s`;

export const formatSpeed = (value: number) => `${value.toFixed(1)} km/h`;

export const haversineMeters = (
  a: Pick<LocationObjectCoords, 'latitude' | 'longitude'>,
  b: Pick<LocationObjectCoords, 'latitude' | 'longitude'>,
) => {
  const R = 6371000;
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(b.latitude - a.latitude);
  const dLon = toRad(b.longitude - a.longitude);
  const lat1 = toRad(a.latitude);
  const lat2 = toRad(b.latitude);
  const x =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
  return R * (2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x)));
};
