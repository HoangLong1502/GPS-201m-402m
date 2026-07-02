import React from 'react';
import { Text, View } from 'react-native';

export function ResultMap(_props: { path: Array<{ latitude: number; longitude: number }>; region: any }) {
  return (
    <View style={{ width: '100%', height: 240, marginTop: 6, borderRadius: 16, borderWidth: 1, borderColor: '#2f2f2f', alignItems: 'center', justifyContent: 'center' }}>
      <Text style={{ color: '#ffd7d7', fontWeight: '700' }}>Bản đồ không hiển thị trên web</Text>
    </View>
  );
}

