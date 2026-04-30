import React, { useEffect, useState } from 'react';
import { FlatList, Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { API_BASE_URL, fetchLeaderboard } from '../api';
import { useAppContext } from '../context/AppContext';
import { LeaderboardItem, LeaderboardMode } from '../types';
import { formatSpeed, formatTime } from '../utils';

const modes: LeaderboardMode[] = ['GPS', 'RACE_201', 'RACE_402'];

export const LeaderboardScreen = () => {
  const { user } = useAppContext();
  const [mode, setMode] = useState<LeaderboardMode>('GPS');
  const [globalData, setGlobalData] = useState<LeaderboardItem[]>([]);
  const [vehicleData, setVehicleData] = useState<LeaderboardItem[]>([]);
  const [showVehicleBoard, setShowVehicleBoard] = useState(false);

  useEffect(() => {
    fetchLeaderboard(mode).then(setGlobalData).catch(() => setGlobalData([]));
    if (showVehicleBoard && user?.vehicleName) {
      fetchLeaderboard(mode, user.vehicleName).then(setVehicleData).catch(() => setVehicleData([]));
    } else {
      setVehicleData([]);
    }
  }, [mode, user?.vehicleName, showVehicleBoard]);

  const renderRow = ({ item, index }: { item: LeaderboardItem; index: number }) => (
    <View style={styles.row}>
      <Text style={styles.rank}>#{index + 1}</Text>
      {item.user.avatar ? (
        <Image
          source={{
            uri: item.user.avatar.startsWith('http')
              ? item.user.avatar
              : `${API_BASE_URL}${item.user.avatar}`,
          }}
          style={styles.avatar}
        />
      ) : (
        <View style={styles.avatarFallback}>
          <Text style={styles.avatarFallbackText}>
            {(item.user.displayName || item.user.vehicleName || 'U').charAt(0).toUpperCase()}
          </Text>
        </View>
      )}
      <View style={{ flex: 1 }}>
        <Text style={styles.vehicle}>{item.user.vehicleName}</Text>
        <Text style={styles.engine}>{item.user.engineType}</Text>
      </View>
      <Text style={styles.value}>{mode === 'GPS' ? formatSpeed(item.maxSpeed) : formatTime(item.time)}</Text>
    </View>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>BẢNG XẾP HẠNG</Text>
      <View style={styles.tabs}>
        {modes.map((item) => (
          <Pressable
            key={item}
            style={[styles.tab, mode === item && styles.tabActive]}
            onPress={() => setMode(item)}
          >
            <Text style={styles.tabLabel}>{item}</Text>
          </Pressable>
        ))}
      </View>
      <Text style={styles.section}>Toàn cầu</Text>
      <FlatList
        data={globalData}
        keyExtractor={(item) => item.id}
        renderItem={renderRow}
        style={styles.list}
      />
      {user?.vehicleName && (
        <>
          <Pressable style={styles.vehicleToggleBtn} onPress={() => setShowVehicleBoard((prev) => !prev)}>
            <Text style={styles.vehicleToggleLabel}>
              {showVehicleBoard ? 'Ẩn xếp hạng theo xe' : `Xem xếp hạng theo xe: ${user.vehicleName}`}
            </Text>
          </Pressable>
          {showVehicleBoard && (
            <>
              <Text style={styles.section}>Theo xe: {user.vehicleName}</Text>
              <FlatList
                data={vehicleData}
                keyExtractor={(item) => `${item.id}-vehicle`}
                renderItem={renderRow}
                style={styles.list}
              />
            </>
          )}
        </>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#090909', padding: 16 },
  title: { color: '#ff4d4f', fontSize: 28, fontWeight: '900' },
  tabs: { flexDirection: 'row', gap: 8, marginTop: 12, marginBottom: 12 },
  tab: { backgroundColor: '#252525', borderRadius: 10, paddingVertical: 10, paddingHorizontal: 12, borderWidth: 1, borderColor: '#333' },
  tabActive: { backgroundColor: '#b82020', borderColor: '#ff6767' },
  tabLabel: { color: 'white', fontWeight: '700' },
  section: { color: '#fff', fontSize: 18, fontWeight: '700', marginTop: 8, marginBottom: 6 },
  vehicleToggleBtn: {
    backgroundColor: '#1a1a1a',
    borderColor: '#b82020',
    borderWidth: 1,
    borderRadius: 10,
    paddingVertical: 10,
    paddingHorizontal: 12,
    marginTop: 4,
    marginBottom: 4,
  },
  vehicleToggleLabel: { color: '#ffb3b3', fontWeight: '700', textAlign: 'center' },
  list: { maxHeight: 180, marginBottom: 8 },
  row: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#161616', padding: 10, borderRadius: 10, marginBottom: 8, gap: 10, borderWidth: 1, borderColor: '#2f2f2f' },
  rank: { color: '#ff7676', width: 34, fontWeight: '800' },
  avatar: { width: 38, height: 38, borderRadius: 19 },
  avatarFallback: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: '#343434',
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarFallbackText: { color: '#fff', fontWeight: '800' },
  vehicle: { color: 'white', fontWeight: '700' },
  engine: { color: '#aaa' },
  value: { color: '#ffd166', fontWeight: '900' },
});
