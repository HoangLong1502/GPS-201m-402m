import React, { useMemo } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import Svg, { Circle, Line, Path, Text as SvgText } from 'react-native-svg';
import { RootStackParamList } from '../AppNavigator';
import { useTracking } from '../hooks/useTracking';
import { formatSpeed, formatTime } from '../utils';
import { submitResult } from '../api';
import { useAppContext } from '../context/AppContext';

type Props = NativeStackScreenProps<RootStackParamList, 'Tracking'>;

const getBorderColor = (speed: number) => {
  if (speed <= 100) return 'transparent';
  const clampedSpeed = Math.min(speed, 250);
  const intensity = Math.round(255 * ((clampedSpeed - 100) / 150));
  return `rgb(${255 - intensity}, 0, 0)`;
};

const getBorderOpacity = (speed: number) => {
  if (speed <= 100) return 0;
  return Math.min(1, (Math.min(speed, 250) - 100) / 150);
};

export const TrackingScreen = ({ route, navigation }: Props) => {
  const { mode } = route.params;
  const tracking = useTracking();
  const { user, setLatestResult } = useAppContext();
  const isRaceMode = mode === 'RACE_201' || mode === 'RACE_402';
  const isGpsMode = mode === 'GPS';
  const isStopwatchMode = mode === 'STOPWATCH';
  const gaugeStart = -210;
  const gaugeEnd = 30;
  const gaugeSpan = gaugeEnd - gaugeStart;
  const speedForNeedle = Math.max(10, Math.min(250, tracking.currentSpeed));
  const needleAngle = gaugeStart + ((speedForNeedle - 10) / 240) * gaugeSpan;
  const majorTicks = [10, 50, 90, 130, 170, 210, 250];
  const minorTicks = Array.from({ length: 25 }, (_, i) => 10 + i * 10);
  const avgSpeedNow =
    tracking.elapsed > 0 ? (tracking.distance / tracking.elapsed) * 3.6 : 0;

  const borderColor = useMemo(() => getBorderColor(tracking.currentSpeed), [tracking.currentSpeed]);
  const borderOpacity = useMemo(() => getBorderOpacity(tracking.currentSpeed), [tracking.currentSpeed]);

  const polarToCartesian = (cx: number, cy: number, radius: number, angleDeg: number) => {
    const angle = (angleDeg * Math.PI) / 180;
    return {
      x: cx + radius * Math.cos(angle),
      y: cy + radius * Math.sin(angle),
    };
  };

  const arcPath = (cx: number, cy: number, radius: number, startAngle: number, endAngle: number) => {
    const start = polarToCartesian(cx, cy, radius, startAngle);
    const end = polarToCartesian(cx, cy, radius, endAngle);
    const largeArcFlag = Math.abs(endAngle - startAngle) > 180 ? 1 : 0;
    const sweepFlag = endAngle > startAngle ? 1 : 0;
    return `M ${start.x} ${start.y} A ${radius} ${radius} 0 ${largeArcFlag} ${sweepFlag} ${end.x} ${end.y}`;
  };

  const onReady = async () => {
    if (!user) {
      Alert.alert('Thiếu hồ sơ', 'Vui lòng cập nhật hồ sơ trước khi chạy.');
      return;
    }
    try {
      await tracking.start(mode, async (result) => {
        setLatestResult(result);
        try {
          await submitResult(result);
        } catch {
          Alert.alert('Lưu thất bại', 'Kết quả chạy chưa được lưu vào cơ sở dữ liệu.');
        }
        if (result.mode === 'STOPWATCH') {
          navigation.replace('RouteMap');
        } else {
          navigation.replace('Result');
        }
      });
    } catch (error) {
      Alert.alert('Lỗi theo dõi GPS', (error as Error).message);
    }
  };

  const onFinish = async () => {
    await tracking.finish();
  };

  return (
    <View style={styles.container}>
      <View style={[styles.borderOverlay, { borderColor, opacity: borderOpacity }]} />
      <View style={styles.headerRow}>
        <Text style={styles.mode}>CHẾ ĐỘ: {mode}</Text>
        {isStopwatchMode && (
          <View style={styles.stopwatchPill}>
            <Text style={styles.stopwatchPillLabel}>BẤM GIỜ</Text>
            <Text style={styles.stopwatchPillValue}>{formatTime(tracking.elapsed)}</Text>
          </View>
        )}
      </View>

      <View style={styles.gaugeWrap}>
        <Svg width={310} height={220}>
          <Path d={arcPath(155, 165, 120, gaugeStart, gaugeEnd)} stroke="#351212" strokeWidth={20} fill="none" />
          <Path d={arcPath(155, 165, 120, gaugeStart, gaugeEnd)} stroke="#ff3b3b" strokeWidth={7} fill="none" />

          {minorTicks.map((value) => {
            const angle = gaugeStart + ((value - 10) / 240) * gaugeSpan;
            const p1 = polarToCartesian(155, 165, 102, angle);
            const p2 = polarToCartesian(155, 165, 114, angle);
            return <Line key={`minor-${value}`} x1={p1.x} y1={p1.y} x2={p2.x} y2={p2.y} stroke="#8a4a4a" strokeWidth={1.5} />;
          })}

          {majorTicks.map((value) => {
            const angle = gaugeStart + ((value - 10) / 240) * gaugeSpan;
            const p1 = polarToCartesian(155, 165, 95, angle);
            const p2 = polarToCartesian(155, 165, 116, angle);
            const label = polarToCartesian(155, 165, 78, angle);
            return (
              <React.Fragment key={`major-${value}`}>
                <Line x1={p1.x} y1={p1.y} x2={p2.x} y2={p2.y} stroke="#ffe3e3" strokeWidth={2.5} />
                <SvgText x={label.x} y={label.y + 4} fill="#ffd0d0" fontSize="12" fontWeight="700" textAnchor="middle">
                  {value}
                </SvgText>
              </React.Fragment>
            );
          })}

          <Line
            x1={155}
            y1={165}
            x2={polarToCartesian(155, 165, 86, needleAngle).x}
            y2={polarToCartesian(155, 165, 86, needleAngle).y}
            stroke="#ffd166"
            strokeWidth={4}
            strokeLinecap="round"
          />
          <Circle cx={155} cy={165} r={10} fill="#ffd166" stroke="#6a4f16" strokeWidth={2} />
        </Svg>
      </View>

      {isRaceMode ? (
        <View style={styles.speedPriorityWrap}>
          <Text style={styles.speedPriorityValue}>{tracking.currentSpeed.toFixed(1)}</Text>
          <Text style={styles.speedPriorityUnit}>km/h</Text>
        </View>
      ) : (
        <Text style={styles.speed}>{formatSpeed(tracking.currentSpeed)}</Text>
      )}
      <Text style={styles.meta}>Tốc độ tối đa: {formatSpeed(tracking.maxSpeed)}</Text>
      <Text style={styles.meta}>Quãng đường: {tracking.distance.toFixed(1)} m</Text>
      {isStopwatchMode && (
        <Text style={styles.meta}>Tốc độ trung bình: {formatSpeed(avgSpeedNow)}</Text>
      )}
      {isStopwatchMode && <Text style={styles.meta}>Thời gian: {formatTime(tracking.elapsed)}</Text>}
      {tracking.countdown !== null && (
        <Text style={styles.countdown}>{tracking.countdown === 0 ? 'BẮT ĐẦU' : tracking.countdown}</Text>
      )}
      {tracking.accuracyWarning && <Text style={styles.warning}>{tracking.accuracyWarning}</Text>}

      {!tracking.isRunning ? (
        <Pressable style={styles.primaryBtn} onPress={onReady}>
          <Text style={styles.primaryLabel}>SẴN SÀNG</Text>
        </Pressable>
      ) : isRaceMode ? (
        <View style={styles.autoFinishBox}>
          <Text style={styles.autoFinishText}>
            DRAG tự kết thúc khi đủ {mode === 'RACE_201' ? '201m' : '402m'}
          </Text>
        </View>
      ) : (
        <Pressable style={styles.finishBtn} onPress={onFinish}>
          <Text style={styles.primaryLabel}>KẾT THÚC</Text>
        </Pressable>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#050505', alignItems: 'center', justifyContent: 'center', gap: 10, padding: 20 },
  borderOverlay: {
    ...StyleSheet.absoluteFillObject,
    borderWidth: 8,
    borderColor: 'transparent',
    borderRadius: 0,
    pointerEvents: 'none',
  },
  headerRow: { width: '100%', flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 },
  mode: { color: '#ff9797', letterSpacing: 1.5, fontWeight: '700' },
  gaugeWrap: {
    width: 310,
    height: 210,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 2,
  },
  speed: { color: '#ff3b3b', fontSize: 44, fontWeight: '900' },
  stopwatchPill: { backgroundColor: '#121212', borderWidth: 1, borderColor: '#2f2f2f', borderRadius: 16, paddingHorizontal: 12, paddingVertical: 10, alignItems: 'center' },
  stopwatchPillLabel: { color: '#ffd166', fontWeight: '900', letterSpacing: 2, fontSize: 12 },
  stopwatchPillValue: { color: '#ff4d4f', fontWeight: '900', fontSize: 20, lineHeight: 22 },
  speedPriorityWrap: { flexDirection: 'row', alignItems: 'flex-end', gap: 8 },
  speedPriorityValue: { color: '#ff3b3b', fontSize: 66, fontWeight: '900', lineHeight: 68 },
  speedPriorityUnit: { color: '#ffd7d7', fontSize: 20, fontWeight: '700', marginBottom: 10 },
  meta: { color: '#ddd', fontSize: 18 },
  countdown: { color: '#ffd166', fontSize: 56, fontWeight: '900' },
  warning: { color: '#ffd166', textAlign: 'center', marginTop: 8 },
  primaryBtn: { backgroundColor: '#b82020', borderRadius: 14, paddingVertical: 14, paddingHorizontal: 44, marginTop: 12 },
  finishBtn: { backgroundColor: '#d63031', borderRadius: 14, paddingVertical: 14, paddingHorizontal: 44, marginTop: 12 },
  autoFinishBox: { backgroundColor: '#1f1f1f', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 10, marginTop: 12 },
  autoFinishText: { color: '#ffd7d7', fontWeight: '700' },
  primaryLabel: { color: 'white', fontSize: 20, fontWeight: '800' },
});