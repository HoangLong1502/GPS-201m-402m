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
          Alert.alert('Đăng nhập thất bại', 'Sai số điện thoại hoặc mật khẩu');
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
          {isLoading ? 'Đang xử lý...' : isRegisterMode ? 'Tạo tài khoản' : 'Vào đường đua'}
        </Text>
      </Pressable>
      <Pressable onPress={() => setIsRegisterMode((prev) => !prev)}>
        <Text style={styles.toggle}>
          {isRegisterMode ? 'Đã có tài khoản? Đăng nhập' : 'Chưa có tài khoản? Đăng ký'}
        </Text>
      </Pressable>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#080808', justifyContent: 'center', padding: 20, gap: 12 },
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
  toggle: { color: '#ffb1b1', textAlign: 'center', marginTop: 6 },
});
