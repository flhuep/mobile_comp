import { Tabs } from 'expo-router';
import { useEffect } from 'react';
import { Platform } from 'react-native';
import { enableScreens } from 'react-native-screens';

export default function RootLayout() {
  useEffect(() => {
    if (Platform.OS !== 'web') {
      enableScreens(true);
    }
  }, []);

  return (
    <Tabs screenOptions={{ headerShown: false }}>
      <Tabs.Screen name="tabs/home" options={{ title: 'Home' }} />
      <Tabs.Screen name="tabs/exercise" options={{ title: 'Exercise' }} />
      <Tabs.Screen name="tabs/workout" options={{ title: 'Workout' }} />
      <Tabs.Screen name="tabs/statistics" options={{ title: 'Statistics' }} />
    </Tabs>
  );
}


