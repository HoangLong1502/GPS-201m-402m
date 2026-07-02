import * as ImagePicker from 'expo-image-picker';
import React, { useState } from 'react';
import axios from 'axios';
import { Alert, Image, Platform, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { API_BASE_URL, upsertUser } from '../api';
import { useAppContext } from '../context/AppContext';

export const ProfileScreen = () => {
  const { user, setUser } = useAppContext();
  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [vehicleName, setVehicleName] = useState(user?.vehicleName ?? '');
  const [engineType, setEngineType] = useState(user?.engineType ?? '');
  const [imageUri, setImageUri] = useState<string | undefined>(
    user?.avatar
      ? user.avatar.startsWith('http')
        ? user.avatar
        : `${API_BASE_URL}${user.avatar}`
      : undefined,
  );
  const [pickedNewAvatar, setPickedNewAvatar] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const pickAvatar = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.7,
    });
    if (!result.canceled) {
      setImageUri(result.assets[0].uri);
      setPickedNewAvatar(true);
    }
  };

  const saveProfile = async () => {
    if (!displayName.trim() || !vehicleName.trim() || !engineType.trim()) {
      Alert.alert('Thiếu thông tin', 'Vui lòng nhập tên, loại xe và loại động cơ.');
      return;
    }
    try {
      setIsSaving(true);
      let payload: FormData | Record<string, string>;
      if (pickedNewAvatar && imageUri) {
        const formData = new FormData();
        formData.append('displayName', displayName.trim());
        formData.append('vehicleName', vehicleName.trim());
        formData.append('engineType', engineType.trim());
        if (Platform.OS === 'web') {
          const blob = await fetch(imageUri).then((res) => res.blob());
          formData.append('avatar', blob, 'avatar.jpg');
        } else {
          formData.append('avatar', {
            uri: imageUri,
            type: 'image/jpeg',
            name: 'avatar.jpg',
          } as unknown as Blob);
        }
        payload = formData;
      } else {
        payload = {
          displayName: displayName.trim(),
          vehicleName: vehicleName.trim(),
          engineType: engineType.trim(),
        };
      }
      const saved = await upsertUser(payload, user?.id);
      await setUser(saved);
      setImageUri(
        saved.avatar
          ? saved.avatar.startsWith('http')
            ? saved.avatar
            : `${API_BASE_URL}${saved.avatar}`
          : undefined,
      );
      setPickedNewAvatar(false);
      Alert.alert('Thành công', 'Đã cập nhật hồ sơ.');
    } catch (error) {
      if (axios.isAxiosError(error) && !error.response) {
        Alert.alert('Lỗi mạng', 'Không kết nối được backend.');
      } else {
        Alert.alert('Lỗi', 'Không thể lưu hồ sơ.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Hồ Sơ Tay Đua</Text>
      <TextInput
        style={styles.input}
        placeholder="Tên hiển thị"
        placeholderTextColor="#888"
        value={displayName}
        onChangeText={setDisplayName}
      />
      <Text style={styles.phone}>Số điện thoại: {user?.phoneNumber ?? '-'}</Text>
      <Pressable onPress={pickAvatar} style={styles.avatarWrap}>
        {imageUri ? (
          <Image style={styles.avatar} source={{ uri: imageUri }} />
        ) : (
          <Text style={styles.avatarPlaceholder}>Tải ảnh đại diện</Text>
        )}
      </Pressable>
      <TextInput
        style={styles.input}
        placeholder="Tên xe (Exciter 155)"
        placeholderTextColor="#888"
        value={vehicleName}
        onChangeText={setVehicleName}
      />
      <TextInput
        style={styles.input}
        placeholder="Loại động cơ (150cc / điện)"
        placeholderTextColor="#888"
        value={engineType}
        onChangeText={setEngineType}
      />
      <Pressable style={styles.button} onPress={saveProfile} disabled={isSaving}>
        <Text style={styles.buttonLabel}>{isSaving ? 'Đang lưu...' : 'Lưu hồ sơ'}</Text>
      </Pressable>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#090909', padding: 20, gap: 12 },
  title: { color: '#ff4d4f', fontSize: 28, fontWeight: '900' },
  phone: { color: '#ffb7b7' },
  avatarWrap: {
    width: 130,
    height: 130,
    borderRadius: 65,
    backgroundColor: '#1d1d1d',
    justifyContent: 'center',
    alignItems: 'center',
    alignSelf: 'center',
    marginVertical: 12,
    overflow: 'hidden',
  },
  avatar: { width: 130, height: 130 },
  avatarPlaceholder: { color: '#bbb', fontWeight: '600' },
  input: {
    backgroundColor: '#151515',
    borderRadius: 10,
    paddingHorizontal: 14,
    color: 'white',
    height: 48,
    borderWidth: 1,
    borderColor: '#2d2d2d',
  },
  button: {
    marginTop: 8,
    backgroundColor: '#b82020',
    padding: 14,
    borderRadius: 10,
    alignItems: 'center',
  },
  buttonLabel: { color: 'white', fontSize: 16, fontWeight: '700' },
});
