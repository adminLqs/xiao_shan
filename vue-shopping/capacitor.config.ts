import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.yourcompany.sellerassistant',
  appName: '云杉购',
  webDir: 'dist',
  plugins: {
    SplashScreen: {
      launchShowDuration: 0,
      backgroundColor: "#ffffff"
    },
    Assets: {
      icon: 'public/icon.png',
      splash: 'public/splash.png'  // 如果有启动图的话
    }
  }
};

export default config;