import React from 'react';
import MapView, { Polyline } from 'react-native-maps';
import { StyleSheet, View } from 'react-native';

type LatLng = { latitude: number; longitude: number };

export function ResultMap({
  path,
  region,
}: {
  path: LatLng[];
  region: any;
}) {
  if (path.length < 2 || !region) return null;

  return (
    <View style={styles.wrap}>
      <MapView style={styles.map} initialRegion={region}>
        <Polyline coordinates={path} strokeWidth={4} strokeColor="#ffd166" />
      </MapView>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { width: '100%', height: 240, marginTop: 6, borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#2f2f2f' },
  map: { flex: 1, backgroundColor: '#060606' },
});

