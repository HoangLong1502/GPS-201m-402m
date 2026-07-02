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
      {isRaceMode ? (
        <>
          <Text style={styles.recordTitle}>RECORD DRAG</Text>
          <Text style={styles.recordTime}>{formatTime(latestResult.time)}</Text>
          <Text style={styles.label}>Thời gian hoàn thành quãng chạy</Text>
        </>
      ) : (
        <>
          <Text style={styles.value}>{formatSpeed(latestResult.maxSpeed)}</Text>
          <Text style={styles.label}>
            {isStopwatchMode ? 'Tốc độ tối đa (Bấm giờ)' : 'Tốc độ tối đa (GPS)'}
          </Text>
        </>
      )}
      {isStopwatchMode && (
        <Text style={styles.item}>
          Tốc độ trung bình: {formatSpeed(latestResult.avgSpeed ?? 0)}
        </Text>
      )}
      <Text style={styles.item}>Thời gian: {formatTime(latestResult.time)}</Text>
      <Text style={styles.item}>Quãng đường: {latestResult.distance.toFixed(1)} m</Text>
      {isGpsMode && (
        <Text style={styles.item}>Tốc độ tối đa: {formatSpeed(latestResult.maxSpeed)}</Text>
      )}

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
  value: { color: '#ffd166', fontSize: 50, fontWeight: '900' },
  recordTitle: { color: '#ffb3b3', fontSize: 17, fontWeight: '700', letterSpacing: 2 },
  recordTime: { color: '#ffd166', fontSize: 62, fontWeight: '900', lineHeight: 68 },
  label: { color: '#bbb', marginBottom: 10 },
  item: { color: 'white', fontSize: 20 },
  mapWrap: { width: '100%', height: 240, marginTop: 6, borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#2f2f2f' },
  map: { flex: 1, backgroundColor: '#060606' },
  button: { marginTop: 20, backgroundColor: '#b82020', paddingHorizontal: 24, paddingVertical: 12, borderRadius: 10 },
  buttonLabel: { color: 'white', fontWeight: '700' },
});
