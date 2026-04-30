import React, { useState } from 'react';
import axios from 'axios';
import { Alert, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { loginByPhone, registerByPhone } from '../api';
import { useAppContext } from '../context/AppContext';

export const LoginScreen = () => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const { setAuthSession } = useAppContext();

  const onLogin = async () => {
    if (!phoneNumber.trim() || !password.trim() || (isRegisterMode && !displayName.trim())) {
      Alert.alert('Thiếu thông tin', 'Vui lòng nhập đầy đủ thông tin');
      return;
    }
    try {
      setIsLoading(true);
      const response = isRegisterMode
        ? await registerByPhone(phoneNumber.trim(), password, displayName.trim())
        : await loginByPhone(phoneNumber.trim(), password);
      await setAuthSession(response.user, response.accessToken);
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const status = error.response?.status;
        if (status === 401) {
          Alert.alert('Đăng nhập thất bại', 'Chưa có tài khoản hoặc sai mật khẩu. Bạn hãy đăng ký nếu chưa có tài khoản.', [
            { text: 'Để sau', style: 'cancel' },
            { text: 'Đăng ký ngay', onPress: () => setIsRegisterMode(true) },
          ]);
          return;
        }
        if (status === 409) {
          Alert.alert('Đăng ký thất bại', 'Số điện thoại đã được đăng ký');
          return;
        }
        if (!error.response) {
          Alert.alert('Lỗi mạng', 'Không kết nối được backend. Kiểm tra Docker/backend.');
          return;
        }
      }
      Alert.alert('Đăng nhập thất bại', 'Không thể đăng nhập lúc này');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.speedStripeTop} />
      <View style={styles.speedStripeBottom} />
      <Text style={styles.title}>{isRegisterMode ? 'Đăng ký tay đua' : 'Đăng nhập đường đua'}</Text>
      <Text style={styles.subtitle}>Số điện thoại + mật khẩu bảo mật</Text>
      {isRegisterMode && (
        <TextInput
          style={styles.input}
          placeholder="Tên hiển thị"
          placeholderTextColor="#888"
          value={displayName}
          onChangeText={setDisplayName}
        />
      )}
      <TextInput
        style={styles.input}
        placeholder="+84901234567"
        placeholderTextColor="#888"
        keyboardType="phone-pad"
        value={phoneNumber}
        onChangeText={setPhoneNumber}
      />
      <TextInput
        style={styles.input}
        placeholder="Mật khẩu tối thiểu 8 ký tự"
        placeholderTextColor="#888"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
      />
      <Pressable style={styles.button} onPress={onLogin} disabled={isLoading}>
        <Text style={styles.buttonText}>
          {isLoading ? 'Đang xử lý...' : isRegisterMode ? 'Tạo tài khoản' : ' ĐĂNG NHẬP'}
        </Text>
      </Pressable>
      <View style={styles.modeRow}>
        <Pressable
          style={[styles.modeButton, !isRegisterMode && styles.modeButtonActive]}
          onPress={() => setIsRegisterMode(false)}
          disabled={isLoading}
        >
          <Text style={[styles.modeButtonText, !isRegisterMode && styles.modeButtonTextActive]}>Đăng nhập</Text>
        </Pressable>
        <Pressable
          style={[styles.modeButton, isRegisterMode && styles.modeButtonActive]}
          onPress={() => setIsRegisterMode(true)}
          disabled={isLoading}
        >
          <Text style={[styles.modeButtonText, isRegisterMode && styles.modeButtonTextActive]}>Đăng ký</Text>
        </Pressable>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#050507', justifyContent: 'center', padding: 20, gap: 12 },
  speedStripeTop: {
    position: 'absolute',
    top: 140,
    right: -40,
    width: 220,
    height: 24,
    backgroundColor: 'rgba(184,32,32,0.26)',
    transform: [{ rotate: '-12deg' }],
    borderRadius: 24,
  },
  speedStripeBottom: {
    position: 'absolute',
    bottom: 120,
    left: -50,
    width: 240,
    height: 20,
    backgroundColor: 'rgba(255,77,79,0.2)',
    transform: [{ rotate: '-12deg' }],
    borderRadius: 20,
  },
  modeRow: { flexDirection: 'row', gap: 8, marginTop: 6 },
  modeButton: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#2f2f2f',
    borderRadius: 10,
    paddingVertical: 10,
    alignItems: 'center',
    backgroundColor: '#121212',
  },
  modeButtonActive: { backgroundColor: '#b82020', borderColor: '#b82020' },
  modeButtonText: { color: '#d0d0d0', fontWeight: '600' },
  modeButtonTextActive: { color: '#fff' },
  title: { color: '#ff4d4f', fontSize: 31, fontWeight: '900' },
  subtitle: { color: '#c4c4c4' },
  input: {
    height: 50,
    backgroundColor: '#151515',
    borderRadius: 10,
    paddingHorizontal: 12,
    color: '#fff',
    borderWidth: 1,
    borderColor: '#2f2f2f',
  },
  button: { backgroundColor: '#b82020', borderRadius: 10, padding: 14, alignItems: 'center', marginTop: 4 },
  buttonText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
