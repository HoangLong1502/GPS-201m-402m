import * as Location from 'expo-location';
import { useCallback, useRef, useState } from 'react';
import { Platform, unstable_batchedUpdates } from 'react-native';
import { Mode, TrackingResult } from '../types';
import { haversineMeters, targetDistanceByMode } from '../utils';

export const useTracking = () => {
  const [currentSpeed, setCurrentSpeed] = useState(0);
  const [maxSpeed, setMaxSpeed] = useState(0);
  const [distance, setDistance] = useState(0);
  const [elapsed, setElapsed] = useState(0);
  const [path, setPath] = useState<Array<{ latitude: number; longitude: number }>>([]);
  const [accuracyWarning, setAccuracyWarning] = useState<string>();
  const [isRunning, setIsRunning] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  const startTimeRef = useRef<number>(0);
  const prevPointRef = useRef<Location.LocationObjectCoords | null>(null);
  const watchRef = useRef<Location.LocationSubscription | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const modeRef = useRef<Mode>('GPS');
  const onFinishRef = useRef<((result: TrackingResult) => void) | null>(null);
  const distanceRef = useRef<number>(0);
  const maxSpeedRef = useRef<number>(0);
  const pathRef = useRef<Array<{ latitude: number; longitude: number }>>([]);
  const lastPathPointRef = useRef<{ latitude: number; longitude: number } | null>(null);
  const lastSampleRef = useRef<{ latitude: number; longitude: number; timestamp: number } | null>(null);
  const smoothedSpeedRef = useRef<number>(0);
  const lastAccuracyWarnRef = useRef<boolean | undefined>(undefined);

  const stopInternal = useCallback(async () => {
    if (watchRef.current) {
      watchRef.current.remove();
      watchRef.current = null;
    }
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    setIsRunning(false);
  }, []);

  const finish = useCallback(async () => {
    const elapsedSec = (Date.now() - startTimeRef.current) / 1000;
    const safeElapsed = Math.max(0.001, elapsedSec);
    const avgSpeed = (distanceRef.current / safeElapsed) * 3.6; // km/h
    const result: TrackingResult = {
      mode: modeRef.current,
      maxSpeed: maxSpeedRef.current,
      time: elapsedSec,
      distance: distanceRef.current,
      avgSpeed,
      path: pathRef.current,
    };
    await stopInternal();
    onFinishRef.current?.(result);
  }, [stopInternal]);

  const start = useCallback(
    async (mode: Mode, onFinish: (result: TrackingResult) => void) => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        throw new Error('Bạn chưa cấp quyền GPS cho ứng dụng.');
      }
      modeRef.current = mode;
      onFinishRef.current = onFinish;
      setDistance(0);
      setCurrentSpeed(0);
      setMaxSpeed(0);
      setElapsed(0);
      setPath([]);
      setAccuracyWarning(undefined);
      prevPointRef.current = null;
      distanceRef.current = 0;
      maxSpeedRef.current = 0;
      pathRef.current = [];
      lastPathPointRef.current = null;
      lastSampleRef.current = null;
      smoothedSpeedRef.current = 0;

      for (let i = 3; i >= 1; i -= 1) {
        setCountdown(i);
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
      setCountdown(0);
      await new Promise((resolve) => setTimeout(resolve, 500));
      setCountdown(null);
      startTimeRef.current = Date.now();
      setIsRunning(true);
      const isGpsMode = mode === 'GPS';
      const tickMs = isGpsMode ? 16 : 100;
      timerRef.current = setInterval(() => {
        setElapsed((Date.now() - startTimeRef.current) / 1000);
      }, tickMs);

      lastAccuracyWarnRef.current = undefined;

      const watchOptions: Location.LocationOptions & {
        activityType?: Location.ActivityType;
        pausesUpdatesAutomatically?: boolean;
      } = {
        accuracy: Location.Accuracy.BestForNavigation,
        distanceInterval: 0,
        timeInterval: isGpsMode ? 1 : 100,
      };
      if (isGpsMode && Platform.OS === 'ios') {
        watchOptions.activityType = Location.ActivityType.AutomotiveNavigation;
        watchOptions.pausesUpdatesAutomatically = false;
      }

      watchRef.current = await Location.watchPositionAsync(
        watchOptions,
        (location) => {
          const now = location.timestamp || Date.now();
          const speedFromSensor = location.coords.speed;
          let speedKmh = Math.max(0, (speedFromSensor ?? 0) * 3.6);
          if ((speedFromSensor === null || speedFromSensor === undefined) && lastSampleRef.current) {
            const deltaMs = now - lastSampleRef.current.timestamp;
            if (deltaMs > 0) {
              const moved = haversineMeters(lastSampleRef.current, location.coords);
              speedKmh = (moved / (deltaMs / 1000)) * 3.6;
            }
          }

          const currentMode = modeRef.current;
          const useRawSpeed = currentMode === 'GPS' || currentMode === 'RACE_201' || currentMode === 'RACE_402';

          let displaySpeed: number;
          if (useRawSpeed) {
            displaySpeed = speedKmh < 0.5 ? 0 : speedKmh;
            smoothedSpeedRef.current = displaySpeed;
          } else {
            const alpha = 0.35;
            const nextSmoothed =
              smoothedSpeedRef.current <= 0 ? speedKmh : smoothedSpeedRef.current + alpha * (speedKmh - smoothedSpeedRef.current);
            smoothedSpeedRef.current = speedKmh < 1 ? 0 : nextSmoothed;
            displaySpeed = smoothedSpeedRef.current;
          }

          maxSpeedRef.current = Math.max(maxSpeedRef.current, speedKmh);

          const lowAccuracy = (location.coords.accuracy ?? 100) > 20;
          const nextWarn = lowAccuracy ? 'Độ chính xác GPS thấp, hãy di chuyển ra khu vực thoáng.' : undefined;
          const accuracyChanged = lastAccuracyWarnRef.current !== lowAccuracy;
          if (accuracyChanged) {
            lastAccuracyWarnRef.current = lowAccuracy;
          }

          const currentPoint = {
            latitude: location.coords.latitude,
            longitude: location.coords.longitude,
          };

          let pathUpdate: Array<{ latitude: number; longitude: number }> | null = null;
          if (lastPathPointRef.current) {
            const moved = haversineMeters(lastPathPointRef.current as any, currentPoint as any);
            const minMove = 1;
            if (moved >= minMove) {
              const nextPath = [...pathRef.current, currentPoint];
              pathRef.current = nextPath;
              lastPathPointRef.current = currentPoint;
              const shouldFlushPath = currentMode !== 'GPS' || nextPath.length % 2 === 0;
              if (shouldFlushPath) {
                pathUpdate = nextPath;
              }
            }
          } else {
            pathRef.current = [currentPoint];
            lastPathPointRef.current = currentPoint;
            pathUpdate = pathRef.current;
          }

          if (prevPointRef.current) {
            const delta = haversineMeters(prevPointRef.current, location.coords);
            distanceRef.current += delta;
            if (distanceRef.current >= targetDistanceByMode(currentMode)) {
              finish().catch(() => null);
            }
          }
          prevPointRef.current = location.coords;
          lastSampleRef.current = {
            latitude: location.coords.latitude,
            longitude: location.coords.longitude,
            timestamp: now,
          };

          if (currentMode === 'GPS') {
            unstable_batchedUpdates(() => {
              setCurrentSpeed(displaySpeed);
              setMaxSpeed(maxSpeedRef.current);
              setDistance(distanceRef.current);
              if (pathUpdate) {
                setPath(pathUpdate);
              }
              if (accuracyChanged) {
                setAccuracyWarning(nextWarn);
              }
            });
          } else {
            setCurrentSpeed(displaySpeed);
            setMaxSpeed(maxSpeedRef.current);
            if (accuracyChanged) {
              setAccuracyWarning(nextWarn);
            }
            setDistance(distanceRef.current);
            if (pathUpdate) {
              setPath(pathUpdate);
            }
          }
        },
      );
    },
    [finish],
  );

  return {
    currentSpeed,
    maxSpeed,
    distance,
    elapsed,
    path,
    countdown,
    isRunning,
    accuracyWarning,
    start,
    finish,
  };
};
