import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../AppNavigator';
import { useAppContext } from '../context/AppContext';
import { formatSpeed, formatTime } from '../utils';
import { ResultMap } from '../components/ResultMap';

type Props = NativeStackScreenProps<RootStackParamList, 'RouteMap'>;

export const RouteMapScreen = ({ navigation }: Props) => {
  const { latestResult } = useAppContext();

  if (!latestResult) {
    return (
      <View style={styles.container}>
        <Text style={styles.title}>Chưa có hành trình</Text>
        <Pressable style={styles.button} onPress={() => navigation.navigate('Home')}>
          <Text style={styles.buttonLabel}>Về trang chính</Text>
        </Pressable>
      </View>
    );
  }

  const path = latestResult.path ?? [];
  const isStopwatchMode = latestResult.mode === 'STOPWATCH';
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
      <Text style={styles.title}>Bản đồ hành trình</Text>

      {path.length >= 2 && region ? (
        <ResultMap path={path} region={region} />
      ) : (
        <View style={styles.mapFallback}>
          <Text style={styles.mapFallbackText}>Không đủ dữ liệu GPS để vẽ bản đồ.</Text>
        </View>
      )}

      <Text style={styles.item}>
        Quãng đường: {latestResult.distance.toFixed(1)} m
      </Text>
      <Text style={styles.item}>Thời gian: {formatTime(latestResult.time)}</Text>
      {isStopwatchMode && (
        <Text style={styles.item}>
          Tốc độ trung bình: {formatSpeed(latestResult.avgSpeed ?? 0)}
        </Text>
      )}
      <Text style={styles.item}>Tốc độ tối đa: {formatSpeed(latestResult.maxSpeed)}</Text>

      <Pressable style={styles.button} onPress={() => navigation.navigate('Home')}>
        <Text style={styles.buttonLabel}>Hoàn tất</Text>
      </Pressable>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#090909', padding: 18, alignItems: 'center' },
  title: { color: '#ff4d4f', fontSize: 26, fontWeight: '900', marginTop: 6, marginBottom: 10 },
  item: { color: 'white', fontSize: 18, marginTop: 6 },
  mapFallback: {
    width: '100%',
    height: 240,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#2f2f2f',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 10,
  },
  mapFallbackText: { color: '#ffd7d7', fontWeight: '700', textAlign: 'center', paddingHorizontal: 12 },
  button: { marginTop: 16, backgroundColor: '#b82020', paddingHorizontal: 24, paddingVertical: 12, borderRadius: 10, width: '100%' },
  buttonLabel: { color: 'white', fontWeight: '700', textAlign: 'center' },
});

