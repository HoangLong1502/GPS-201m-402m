import React, { useLayoutEffect } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../AppNavigator';
import { useTracking } from '../hooks/useTracking';
import { useEngineSoundMeter } from '../hooks/useEngineSoundMeter';
import { formatSpeed, formatTime } from '../utils';
import { submitResult } from '../api';
import { useAppContext } from '../context/AppContext';

type Props = NativeStackScreenProps<RootStackParamList, 'Tracking'>;

export const TrackingScreen = ({ route, navigation }: Props) => {
  const { mode } = route.params;
  const tracking = useTracking();
  const { user, setLatestResult } = useAppContext();
  const isRaceMode = mode === 'RACE_201' || mode === 'RACE_402';
  const isGpsMode = mode === 'GPS';
  const isStopwatchMode = mode === 'STOPWATCH';
  const avgSpeedNow =
    tracking.elapsed > 0 ? (tracking.distance / tracking.elapsed) * 3.6 : 0;
  const { activeLeds } = useEngineSoundMeter(tracking.isRunning);

  useLayoutEffect(() => {
    const titleByMode: Record<string, string> = {
      GPS: 'bấm max GPS',
      STOPWATCH: 'Bấm giờ quãng đường',
      RACE_201: 'Drag 201m',
      RACE_402: 'Drag 402m',
    };
    navigation.setOptions({ title: titleByMode[mode] ?? 'Theo Dõi Tốc Độ' });
  }, [mode, navigation]);

  const onReady = async () => {
    if (!user) {
      Alert.alert('Thiếu hồ sơ', 'Vui lòng cập nhật hồ sơ trước khi chạy.');
      return;
    }
    try {
      await tracking.start(mode, async (result) => {
        setLatestResult(result);
        try {
          await submitResult(result);
        } catch {
          Alert.alert('Lưu thất bại', 'Kết quả chạy chưa được lưu vào cơ sở dữ liệu.');
        }
        if (result.mode === 'STOPWATCH') {
          navigation.replace('RouteMap');
        } else {
          navigation.replace('Result');
        }
      });
    } catch (error) {
      Alert.alert('Lỗi theo dõi GPS', (error as Error).message);
    }
  };

  const onFinish = async () => {
    await tracking.finish();
  };

  const primaryValue = isRaceMode ? formatTime(tracking.elapsed) : tracking.currentSpeed.toFixed(1);
  const primaryUnit = isRaceMode ? 'sec' : 'km/h';
  const ledColorStyle = isRaceMode ? styles.ledRed : isStopwatchMode ? styles.ledBlue : styles.ledGreen;

  return (
    <View style={styles.container}>
      <View style={styles.headerRow}>
        <Text style={styles.mode}>CHẾ ĐỘ: {mode}</Text>
        {isStopwatchMode && (
          <View style={styles.stopwatchPill}>
            <Text style={styles.stopwatchPillLabel}>BẤM GIỜ</Text>
            <Text style={styles.stopwatchPillValue}>{formatTime(tracking.elapsed)}</Text>
          </View>
        )}
      </View>

      <View style={styles.primaryTopCard}>
        <Text style={styles.primaryTopLabel}>{isRaceMode ? 'RACE TIME' : 'LIVE SPEED'}</Text>
        <View style={styles.primaryTopValueRow}>
          <Text style={styles.primaryTopValue} numberOfLines={1} adjustsFontSizeToFit>
            {primaryValue}
          </Text>
          <Text style={styles.primaryTopUnit}>{primaryUnit}</Text>
        </View>
      </View>

      <View style={styles.cockpitShell}>
        <View style={styles.ledRow}>
          {Array.from({ length: 12 }, (_, idx) => (
            <View
              key={`led-${idx}`}
              style={[
                styles.ledDot,
                idx < activeLeds ? ledColorStyle : styles.ledOff,
                idx < activeLeds && styles.ledGlow,
              ]}
            />
          ))}
        </View>
        <View style={styles.cockpitTopRow}>
          <View style={styles.smallPanel}>
            <Text style={styles.smallPanelLabel}>MODE</Text>
            <Text style={styles.smallPanelValue}>{mode}</Text>
          </View>
          <View style={styles.smallPanel}>
            <Text style={styles.smallPanelLabel}>TIME</Text>
            <Text style={styles.smallPanelValue}>{formatTime(tracking.elapsed)}</Text>
          </View>
          <View style={styles.smallPanel}>
            <Text style={styles.smallPanelLabel}>{isRaceMode ? 'TARGET' : 'MAX'}</Text>
            <Text style={styles.smallPanelValue}>{isRaceMode ? (mode === 'RACE_201' ? '201m' : '402m') : formatSpeed(tracking.maxSpeed)}</Text>
          </View>
        </View>
        <View style={styles.cockpitGrid}>
          <View style={styles.sideColumn}>
            <View style={styles.metricCard}>
              <Text style={styles.metricLabel}>{isRaceMode ? 'SPEED' : 'MAX SPEED'}</Text>
              <Text style={styles.metricValue}>{isRaceMode ? formatSpeed(tracking.currentSpeed) : formatSpeed(tracking.maxSpeed)}</Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricLabel}>DISTANCE</Text>
              <Text style={styles.metricValue}>{tracking.distance.toFixed(1)} m</Text>
            </View>
          </View>
          <View style={styles.sideColumn}>
            <View style={styles.metricCard}>
              <Text style={styles.metricLabel}>{isRaceMode ? 'MAX SPEED' : 'AVG SPEED'}</Text>
              <Text style={styles.metricValue}>{isRaceMode ? formatSpeed(tracking.maxSpeed) : formatSpeed(avgSpeedNow)}</Text>
            </View>
            <View style={styles.metricCard}>
              <Text style={styles.metricLabel}>{isRaceMode ? 'STATUS' : 'GPS STATE'}</Text>
              <Text style={styles.metricValue}>{tracking.accuracyWarning ? 'LOW ACC' : 'READY'}</Text>
            </View>
          </View>
        </View>
      </View>
      {tracking.countdown !== null && (
        <Text style={styles.countdown}>{tracking.countdown === 0 ? 'BẮT ĐẦU' : tracking.countdown}</Text>
      )}
      {tracking.accuracyWarning && <Text style={styles.warning}>{tracking.accuracyWarning}</Text>}

      {!tracking.isRunning ? (
        <Pressable style={styles.primaryBtn} onPress={onReady}>
          <Text style={styles.primaryLabel}>SẴN SÀNG</Text>
        </Pressable>
      ) : isRaceMode ? (
        <View style={styles.autoFinishBox}>
          <Text style={styles.autoFinishText}>
            DRAG tự kết thúc khi đủ {mode === 'RACE_201' ? '201m' : '402m'}
          </Text>
        </View>
      ) : (
        <Pressable style={styles.finishBtn} onPress={onFinish}>
          <Text style={styles.primaryLabel}>KẾT THÚC</Text>
        </Pressable>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#050505', alignItems: 'center', justifyContent: 'center', gap: 10, padding: 20 },
  headerRow: { width: '100%', flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 },
  mode: { color: '#ff9797', letterSpacing: 1.5, fontWeight: '700' },
  primaryTopCard: {
    width: '100%',
    borderRadius: 14,
    borderWidth: 1.5,
    borderColor: '#3a3218',
    backgroundColor: '#17140c',
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  primaryTopLabel: { color: '#f7d46a', fontSize: 11, fontWeight: '800', letterSpacing: 1.5 },
  primaryTopValueRow: { flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between', gap: 10, marginTop: 2 },
  primaryTopValue: { color: '#f8ef52', fontSize: 60, fontWeight: '900', lineHeight: 62, flexShrink: 1 },
  primaryTopUnit: { color: '#f7d46a', fontSize: 20, fontWeight: '800', marginBottom: 8 },
  cockpitShell: {
    width: '100%',
    borderRadius: 16,
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderWidth: 2,
    borderColor: '#232323',
    backgroundColor: '#0b0b0b',
  },
  ledRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 10, paddingHorizontal: 2 },
  ledDot: { width: 14, height: 14, borderRadius: 7 },
  ledGreen: { backgroundColor: '#34e56f' },
  ledBlue: { backgroundColor: '#5aa9ff' },
  ledRed: { backgroundColor: '#ff5a5a' },
  ledOff: { backgroundColor: '#1b2a3f' },
  ledGlow: {
    shadowColor: '#9fd2ff',
    shadowOpacity: 0.8,
    shadowRadius: 6,
  },
  cockpitTopRow: { flexDirection: 'row', gap: 8, marginBottom: 8 },
  smallPanel: {
    flex: 1,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#262626',
    backgroundColor: '#121212',
    alignItems: 'center',
    paddingVertical: 8,
  },
  smallPanelLabel: { color: '#7ed1ff', fontSize: 10, fontWeight: '700', letterSpacing: 1 },
  smallPanelValue: { color: '#f4f4f4', fontSize: 13, fontWeight: '800', marginTop: 2 },
  cockpitGrid: { flexDirection: 'row', gap: 8, minHeight: 210 },
  sideColumn: { flex: 1, gap: 8 },
  metricCard: {
    flex: 1,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#2b2b2b',
    backgroundColor: '#131313',
    paddingVertical: 12,
    paddingHorizontal: 10,
    justifyContent: 'center',
  },
  metricLabel: { color: '#89d8ff', fontSize: 11, fontWeight: '700', marginBottom: 4 },
  metricValue: { color: '#f8f8f8', fontSize: 20, fontWeight: '900' },
  stopwatchPill: { backgroundColor: '#121212', borderWidth: 1, borderColor: '#2f2f2f', borderRadius: 16, paddingHorizontal: 12, paddingVertical: 10, alignItems: 'center' },
  stopwatchPillLabel: { color: '#ffd166', fontWeight: '900', letterSpacing: 2, fontSize: 12 },
  stopwatchPillValue: { color: '#ff4d4f', fontWeight: '900', fontSize: 20, lineHeight: 22 },
  countdown: { color: '#ffd166', fontSize: 56, fontWeight: '900' },
  warning: { color: '#ffd166', textAlign: 'center', marginTop: 8 },
  primaryBtn: { backgroundColor: '#b82020', borderRadius: 14, paddingVertical: 14, paddingHorizontal: 44, marginTop: 12 },
  finishBtn: { backgroundColor: '#d63031', borderRadius: 14, paddingVertical: 14, paddingHorizontal: 44, marginTop: 12 },
  autoFinishBox: { backgroundColor: '#1f1f1f', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 10, marginTop: 12 },
  autoFinishText: { color: '#ffd7d7', fontWeight: '700' },
  primaryLabel: { color: 'white', fontSize: 20, fontWeight: '800' },
});
