import * as Location from 'expo-location';
import { useCallback, useRef, useState } from 'react';
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
      timerRef.current = setInterval(() => {
        setElapsed((Date.now() - startTimeRef.current) / 1000);
      }, 100);

      watchRef.current = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.BestForNavigation,
          distanceInterval: 0,
          timeInterval: 100,
        },
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
          // Smooth only UI speed (EMA), while keeping tracking logic realtime.
          const alpha = 0.35;
          const nextSmoothed =
            smoothedSpeedRef.current <= 0 ? speedKmh : smoothedSpeedRef.current + alpha * (speedKmh - smoothedSpeedRef.current);
          smoothedSpeedRef.current = speedKmh < 1 ? 0 : nextSmoothed;
          setCurrentSpeed(smoothedSpeedRef.current);

          maxSpeedRef.current = Math.max(maxSpeedRef.current, speedKmh);
          setMaxSpeed(maxSpeedRef.current);
          if ((location.coords.accuracy ?? 100) > 20) {
            setAccuracyWarning('Độ chính xác GPS thấp, hãy di chuyển ra khu vực thoáng.');
          } else {
            setAccuracyWarning(undefined);
          }

          const currentPoint = {
            latitude: location.coords.latitude,
            longitude: location.coords.longitude,
          };

          // Thêm điểm đường đi (downsample nhẹ để giảm lag UI)
          if (lastPathPointRef.current) {
            const moved = haversineMeters(lastPathPointRef.current as any, currentPoint as any);
            // Ngưỡng thấp để đảm bảo STOPWATCH/GPS có đủ điểm để vẽ map
            if (moved >= 1) {
              const nextPath = [...pathRef.current, currentPoint];
              pathRef.current = nextPath;
              lastPathPointRef.current = currentPoint;
              setPath(nextPath);
            }
          } else {
            pathRef.current = [currentPoint];
            lastPathPointRef.current = currentPoint;
            setPath(pathRef.current);
          }

          if (prevPointRef.current) {
            const delta = haversineMeters(prevPointRef.current, location.coords);
            distanceRef.current += delta;
            setDistance(distanceRef.current);
            if (distanceRef.current >= targetDistanceByMode(modeRef.current)) {
              finish().catch(() => null);
            }
          }
          prevPointRef.current = location.coords;
          lastSampleRef.current = {
            latitude: location.coords.latitude,
            longitude: location.coords.longitude,
            timestamp: now,
          };
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
