import React from 'react';
import * as Clipboard from 'expo-clipboard';
import { Alert, Image, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../AppNavigator';
import { Mode } from '../types';
import { useAppContext } from '../context/AppContext';
import { AdMobBanner } from '../components/AdMobBanner';

type Props = NativeStackScreenProps<RootStackParamList, 'Home'>;

export const HomeScreen = ({ navigation }: Props) => {
  const { user, clearAuthSession } = useAppContext();
  const [showDonateModal, setShowDonateModal] = React.useState(false);

  const bankInfo = {
    bankName: 'Ngân hàng: Techcombank',
    accountName: 'Chủ tài khoản: VÕ HOÀNG LONG',
    accountNumber: '19036970601010',
  };

  const openMode = (mode: Mode) => {
    navigation.navigate('Tracking', { mode });
  };

  const copyAccountNumber = async () => {
    await Clipboard.setStringAsync(bankInfo.accountNumber);
    Alert.alert('Đã sao chép', 'Đã copy số tài khoản.');
  };

  return (
    <View style={styles.root}>
      <View style={styles.container}>
      <Pressable style={styles.profileTopBtn} onPress={() => navigation.navigate('Profile')}>
        <Text style={styles.profileTopLabel}>Hồ sơ</Text>
      </Pressable>

      <Text style={styles.greeting}>{user?.displayName ? `Xin chào, ${user.displayName}` : 'Xin chào tay đua'}</Text>
      <Text style={styles.vehicle}>{user ? `Xe: ${user.vehicleName ?? 'Chưa cập nhật'}` : 'Chưa có hồ sơ'}</Text>
      <Text style={styles.title}>CHỌN CHẾ ĐỘ ĐUA</Text>
      <Pressable style={styles.modeBtn} onPress={() => openMode('GPS')}>
        <Text style={styles.modeLabel}>GPS TỰ DO</Text>
      </Pressable>
      <Pressable style={styles.modeBtn} onPress={() => openMode('STOPWATCH')}>
        <Text style={styles.modeLabel}>BẤM GIỜ</Text>
      </Pressable>
      <Pressable style={styles.modeBtn} onPress={() => openMode('RACE_201')}>
        <Text style={styles.modeLabel}>DRAG 201M</Text>
      </Pressable>
      <Pressable style={styles.modeBtn} onPress={() => openMode('RACE_402')}>
        <Text style={styles.modeLabel}>DRAG 402M</Text>
      </Pressable>
      <Pressable style={styles.secondaryBtn} onPress={() => navigation.navigate('Leaderboard')}>
        <Text style={styles.secondaryLabel}>Bảng xếp hạng</Text>
      </Pressable>
      <Pressable style={styles.logoutBtn} onPress={() => void clearAuthSession()}>
        <Text style={styles.secondaryLabel}>Đăng xuất</Text>
      </Pressable>

      <Pressable style={styles.donateButton} onPress={() => setShowDonateModal(true)}>
        <Text style={styles.donateButtonText}>Ấn vào đây để donate, cảm ơn ae =))</Text>
      </Pressable>

      <Modal visible={showDonateModal} transparent animationType="fade" onRequestClose={() => setShowDonateModal(false)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Chuyển khoản ủng hộ</Text>
            <Text style={styles.modalSubtitle}> Ấn vào đây để mình có bánh mì ăn =))</Text>
            <Image
              source={require('../../assets/donate-qr.png')}
              style={styles.modalQr}
              resizeMode="contain"
            />
            <Text style={styles.bankInfo}>{bankInfo.bankName}</Text>
            <Text style={styles.bankInfo}>{bankInfo.accountName}</Text>
            <Text style={styles.bankInfo}>Số tài khoản: {bankInfo.accountNumber}</Text>
            <Pressable style={styles.copyButton} onPress={() => void copyAccountNumber()}>
              <Text style={styles.secondaryLabel}>Copy STK</Text>
            </Pressable>
            <Pressable style={styles.closeButton} onPress={() => setShowDonateModal(false)}>
              <Text style={styles.secondaryLabel}>Đóng</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
      </View>
      <AdMobBanner />
    </View>
  );
};

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#090909' },
  container: { flex: 1, backgroundColor: '#090909', padding: 20, justifyContent: 'center', gap: 12 },
  profileTopBtn: {
    position: 'absolute',
    top: 12,
    right: 14,
    backgroundColor: '#2a2a2a',
    borderWidth: 1,
    borderColor: '#ff4d4f',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 999,
  },
  profileTopLabel: { color: '#ffb3b3', fontWeight: '700' },
  greeting: { color: '#f3f3f3', textAlign: 'center', fontSize: 18, fontWeight: '700' },
  vehicle: { color: '#bbbbbb', textAlign: 'center', marginBottom: 8 },
  title: { color: '#ff4d4f', fontSize: 28, textAlign: 'center', fontWeight: '900', marginBottom: 4 },
  modeBtn: {
    backgroundColor: '#141414',
    borderRadius: 14,
    padding: 18,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#333',
  },
  modeLabel: { color: 'white', fontWeight: '800', fontSize: 18, letterSpacing: 1 },
  secondaryBtn: { backgroundColor: '#7a1f1f', borderRadius: 12, padding: 14, alignItems: 'center' },
  logoutBtn: { backgroundColor: '#3b3b3b', borderRadius: 12, padding: 14, alignItems: 'center' },
  secondaryLabel: { color: 'white', fontWeight: '700' },
  donateButton: {
    marginTop: 8,
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
    backgroundColor: '#2a2a2a',
    borderWidth: 1,
    borderColor: '#ff6b6b',
  },
  donateButtonText: { color: '#ffd4d4', fontWeight: '700' },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalCard: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: '#141414',
    borderRadius: 16,
    padding: 18,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#2f2f2f',
  },
  modalTitle: { color: '#ff6b6b', fontSize: 22, fontWeight: '900' },
  modalSubtitle: { color: '#ddd', fontSize: 13, marginTop: 4, marginBottom: 12 },
  modalQr: {
    width: 220,
    height: 220,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#454545',
    marginBottom: 10,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  bankInfo: { color: '#fff', fontSize: 14, marginBottom: 4 },
  copyButton: {
    marginTop: 8,
    backgroundColor: '#2f6f2f',
    borderRadius: 10,
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  closeButton: {
    marginTop: 10,
    backgroundColor: '#7a1f1f',
    borderRadius: 10,
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
});
