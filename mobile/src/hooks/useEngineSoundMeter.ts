import { Audio } from 'expo-av';
import { useCallback, useEffect, useRef, useState } from 'react';

const MIN_DB = -60;
const MAX_DB = 0;
const LED_COUNT = 12;

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));

export const useEngineSoundMeter = (enabled: boolean) => {
  const [activeLeds, setActiveLeds] = useState(0);
  const recordingRef = useRef<Audio.Recording | null>(null);
  const smoothLevelRef = useRef(0);

  const stopMeter = useCallback(async () => {
    const recording = recordingRef.current;
    if (recording) {
      try {
        await recording.stopAndUnloadAsync();
      } catch {
        // Ignore stop errors when recording already stopped.
      }
      recordingRef.current = null;
    }
    smoothLevelRef.current = 0;
    setActiveLeds(0);
  }, []);

  const startMeter = useCallback(async () => {
    const permission = await Audio.requestPermissionsAsync();
    if (!permission.granted) {
      setActiveLeds(0);
      return;
    }

    await Audio.setAudioModeAsync({
      allowsRecordingIOS: true,
      playsInSilentModeIOS: true,
    });

    const recording = new Audio.Recording();
    await recording.prepareToRecordAsync({
      android: {
        extension: '.m4a',
        outputFormat: Audio.AndroidOutputFormat.MPEG_4,
        audioEncoder: Audio.AndroidAudioEncoder.AAC,
        sampleRate: 44100,
        numberOfChannels: 1,
        bitRate: 128000,
      },
      ios: {
        extension: '.m4a',
        outputFormat: Audio.IOSOutputFormat.MPEG4AAC,
        audioQuality: Audio.IOSAudioQuality.HIGH,
        sampleRate: 44100,
        numberOfChannels: 1,
        bitRate: 128000,
        linearPCMBitDepth: 16,
        linearPCMIsBigEndian: false,
        linearPCMIsFloat: false,
      },
      web: {
        mimeType: 'audio/webm',
        bitsPerSecond: 128000,
      },
      isMeteringEnabled: true,
    });

    recording.setOnRecordingStatusUpdate((status) => {
      if (!status.isRecording) return;
      const metering = typeof status.metering === 'number' ? status.metering : MIN_DB;
      const normalized = clamp((metering - MIN_DB) / (MAX_DB - MIN_DB), 0, 1);

      // Smooth LED movement so it feels like a real dash.
      const alpha = 0.28;
      smoothLevelRef.current += alpha * (normalized - smoothLevelRef.current);
      const next = Math.round(smoothLevelRef.current * LED_COUNT);
      setActiveLeds(clamp(next, 0, LED_COUNT));
    });

    await recording.startAsync();
    recordingRef.current = recording;
  }, []);

  useEffect(() => {
    if (enabled) {
      startMeter().catch(() => setActiveLeds(0));
      return () => {
        stopMeter().catch(() => null);
      };
    }

    stopMeter().catch(() => null);
    return undefined;
  }, [enabled, startMeter, stopMeter]);

  return { activeLeds };
};

