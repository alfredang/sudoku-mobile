import { SplashScreen } from '@capacitor/splash-screen';
import { Haptics, ImpactStyle, NotificationType } from '@capacitor/haptics';
import { StatusBar, Style } from '@capacitor/status-bar';
import { App } from '@capacitor/app';

// Hide splash screen once the DOM is ready
document.addEventListener('DOMContentLoaded', async () => {
  requestAnimationFrame(async () => {
    await SplashScreen.hide({ fadeOutDuration: 300 });
  });

  // Configure status bar for native feel
  try {
    await StatusBar.setStyle({ style: Style.Light });
    await StatusBar.setBackgroundColor({ color: '#FFF8E7' });
  } catch (e) {
    // StatusBar not available in web/browser
  }
});

// Expose haptic helper for game.js to use
window.triggerHaptic = async (style) => {
  try {
    if (style === 'success') {
      await Haptics.notification({ type: NotificationType.Success });
    } else if (style === 'error') {
      await Haptics.notification({ type: NotificationType.Error });
    } else if (style === 'medium') {
      await Haptics.impact({ style: ImpactStyle.Medium });
    } else {
      await Haptics.impact({ style: ImpactStyle.Light });
    }
  } catch (e) {
    // Haptics not available on web
  }
};

// Pause/resume timer when app goes to background/foreground
App.addListener('appStateChange', ({ isActive }) => {
  if (!isActive) {
    // App went to background — pause timer
    if (window.timerInterval) {
      clearInterval(window.timerInterval);
    }
  } else {
    // App came to foreground — resume timer if game is in progress
    if (typeof gameState !== 'undefined' && gameState === 'playing') {
      startTimer();
    }
  }
});

// Android back button: go to menu if playing, exit if on menu
App.addListener('backButton', () => {
  if (typeof gameState !== 'undefined' && gameState === 'playing') {
    showMenu();
  } else {
    App.exitApp();
  }
});
