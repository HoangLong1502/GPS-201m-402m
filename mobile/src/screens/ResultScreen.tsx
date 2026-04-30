import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../AppNavigator';
import { useAppContext } from '../context/AppContext';
import { formatSpeed, formatTime } from '../utils';
import { ResultMap } from '../components/ResultMap';

type Props = NativeStackScreenProps<RootStackParamList, 'Result'>;

export const ResultScreen = ({ navigation }: Props) => {
  const { latestResult } = useAppContext();

  if (!latestResult) {
    return (
      <View style={styles.container}>
        <Text style={styles.title}>Chưa có kết quả</Text>
        <Pressable style={styles.button} onPress={() => navigation.navigate('Home')}>
          <Text style={styles.buttonLabel}>Về trang chính</Text>
        </Pressable>
      </View>
    );
  }

  const isRaceMode = latestResult.mode === 'RACE_201' || latestResult.mode === 'RACE_402';
  const isGpsMode = latestResult.mode === 'GPS';
  const isStopwatchMode = latestResult.mode === 'STOPWATCH';
  const path = latestResult.path ?? [];

  const region =
    path.length >= 2
      ? (() => {
          const lats = path.map((p) => p.latitude);
          const lons = path.map((p) => p.longitude);
          const minLat = Math.min(...lats);
          const maxLat = Math.max(...lats);
          const minLon = Math.min(...lons);
          const maxLon = Math.max(...lons);
          const lat = (minLat + maxLat) / 2;
          const lon = (minLon + maxLon) / 2;
          const latDelta = Math.max(0.002, (maxLat - minLat) * 1.2);
          const lonDelta = Math.max(0.002, (maxLon - minLon) * 1.2);
          return { latitude: lat, longitude: lon, latitudeDelta: latDelta, longitudeDelta: lonDelta };
        })()
      : null;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Kết quả ({latestResult.mode})</Text>
      <View style={styles.primaryTopCard}>
        <Text style={styles.primaryTopLabel}>{isRaceMode ? 'RACE TIME' : 'MAX SPEED'}</Text>
        <Text style={styles.primaryTopValue}>{isRaceMode ? formatTime(latestResult.time) : formatSpeed(latestResult.maxSpeed)}</Text>
      </View>

      <View style={styles.summaryDash}>
        <View style={styles.summaryLedRow}>
          {Array.from({ length: 12 }, (_, idx) => (
            <View
              key={`result-led-${idx}`}
              style={[
                styles.summaryLedDot,
                idx < 9 ? (isRaceMode ? styles.summaryLedRace : styles.summaryLedGps) : styles.summaryLedOff,
              ]}
            />
          ))}
        </View>
        <View style={styles.summaryTopRow}>
          <View style={styles.summaryTopCell}>
            <Text style={styles.summaryTopLabel}>MODE</Text>
            <Text style={styles.summaryTopValue}>{latestResult.mode}</Text>
          </View>
          <View style={styles.summaryTopCell}>
            <Text style={styles.summaryTopLabel}>TIME</Text>
            <Text style={styles.summaryTopValue}>{formatTime(latestResult.time)}</Text>
          </View>
          <View style={styles.summaryTopCell}>
            <Text style={styles.summaryTopLabel}>DIST</Text>
            <Text style={styles.summaryTopValue}>{latestResult.distance.toFixed(1)}m</Text>
          </View>
        </View>
        <View style={styles.summaryMetricRow}>
          <View style={styles.summaryMetricCell}>
            <Text style={styles.summaryMetricLabel}>MAX SPEED</Text>
            <Text style={styles.summaryMetricValue}>{formatSpeed(latestResult.maxSpeed)}</Text>
          </View>
          <View style={styles.summaryMetricCell}>
            <Text style={styles.summaryMetricLabel}>RESULT</Text>
            <Text style={styles.summaryMetricValue}>{isRaceMode ? 'DRAG DONE' : isGpsMode ? 'GPS RUN' : 'STOPWATCH'}</Text>
          </View>
        </View>
        {isStopwatchMode && (
          <View style={styles.summaryMetricRow}>
            <View style={styles.summaryMetricCellWide}>
              <Text style={styles.summaryMetricLabel}>AVG SPEED</Text>
              <Text style={styles.summaryMetricValue}>{formatSpeed(latestResult.avgSpeed ?? 0)}</Text>
            </View>
          </View>
        )}
      </View>

      {isStopwatchMode && path.length >= 2 && region && <ResultMap path={path} region={region} />}
      <Pressable style={styles.button} onPress={() => navigation.navigate('Home')}>
        <Text style={styles.buttonLabel}>Hoàn tất</Text>
      </Pressable>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#090909', justifyContent: 'center', alignItems: 'center', gap: 10, padding: 20 },
  title: { color: '#ff4d4f', fontSize: 28, fontWeight: '900' },
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
  primaryTopValue: { color: '#f8ef52', fontSize: 48, fontWeight: '900', lineHeight: 52, marginTop: 2 },
  summaryDash: {
    width: '100%',
    borderRadius: 14,
    borderWidth: 1.5,
    borderColor: '#2a2a2a',
    backgroundColor: '#0d0d0d',
    padding: 10,
    gap: 8,
  },
  summaryLedRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 2 },
  summaryLedDot: { width: 12, height: 12, borderRadius: 6 },
  summaryLedRace: { backgroundColor: '#ff5a5a' },
  summaryLedGps: { backgroundColor: '#5aa9ff' },
  summaryLedOff: { backgroundColor: '#1b2a3f' },
  summaryTopRow: { flexDirection: 'row', gap: 8 },
  summaryTopCell: {
    flex: 1,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#252525',
    backgroundColor: '#141414',
    paddingVertical: 8,
    paddingHorizontal: 10,
  },
  summaryTopLabel: { color: '#7ed1ff', fontSize: 10, fontWeight: '700', letterSpacing: 1 },
  summaryTopValue: { color: '#f2f2f2', fontSize: 14, fontWeight: '900', marginTop: 2 },
  summaryMetricRow: { flexDirection: 'row', gap: 8 },
  summaryMetricCell: {
    flex: 1,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#2b2b2b',
    backgroundColor: '#151515',
    paddingVertical: 10,
    paddingHorizontal: 10,
  },
  summaryMetricCellWide: {
    flex: 1,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#393118',
    backgroundColor: '#1a160e',
    paddingVertical: 10,
    paddingHorizontal: 10,
  },
  summaryMetricLabel: { color: '#89d8ff', fontSize: 11, fontWeight: '700', marginBottom: 4 },
  summaryMetricValue: { color: '#fff', fontSize: 22, fontWeight: '900' },
  mapWrap: { width: '100%', height: 240, marginTop: 6, borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#2f2f2f' },
  map: { flex: 1, backgroundColor: '#060606' },
  button: { marginTop: 16, backgroundColor: '#b82020', paddingHorizontal: 24, paddingVertical: 12, borderRadius: 10, width: '100%' },
  buttonLabel: { color: 'white', fontWeight: '700' },
});
