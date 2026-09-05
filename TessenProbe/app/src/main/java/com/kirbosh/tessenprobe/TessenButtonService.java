package com.kirbosh.tessenprobe;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.provider.Settings;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class TessenButtonService extends AccessibilityService implements InputManager.InputDeviceListener {
    private static final int ROG_SCAN_CODE = 319;
    private static final int ASUS_VENDOR_ID = 2821;
    private static final int TESSEN_PRODUCT_ID = 6884;

    private InputManager inputManager;
    private boolean tessenConnected;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(this, null);
        }

        refreshTessenState();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally unused. This service never needs screen or UI events.
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(this);
        }
        super.onDestroy();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (!tessenConnected) return false;
        if (event.getScanCode() != ROG_SCAN_CODE) return false;

        InputDevice device = event.getDevice();
        if (!isTessen(device)) return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            launchTarget();
        }
        return true;
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if (isTessen(device)) {
            setTessenConnected(true);
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        refreshTessenState();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        refreshTessenState();
    }

    private void refreshTessenState() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            if (isTessen(InputDevice.getDevice(id))) {
                found = true;
                break;
            }
        }
        setTessenConnected(found);
    }

    private boolean isTessen(InputDevice device) {
        return device != null
                && device.getVendorId() == ASUS_VENDOR_ID
                && device.getProductId() == TESSEN_PRODUCT_ID;
    }

    private void setTessenConnected(boolean connected) {
        if (tessenConnected == connected) {
            ensureNoAccessibilityEvents();
            return;
        }

        tessenConnected = connected;
        updateKeyFiltering(connected);
    }

    private void ensureNoAccessibilityEvents() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null && info.eventTypes != 0) {
            info.eventTypes = 0;
            info.notificationTimeout = 0;
            setServiceInfo(info);
        }
    }

    private void updateKeyFiltering(boolean enabled) {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;

        // Zero means Android does not deliver any Accessibility UI events to us.
        info.eventTypes = 0;
        info.notificationTimeout = 0;

        if (enabled) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        } else {
            info.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        }

        setServiceInfo(info);
    }

    private void launchTarget() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String packageName = prefs.getString("target_package", null);
        boolean forceLandscape = prefs.getBoolean("force_landscape", true);

        if (forceLandscape && Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                Settings.System.putInt(getContentResolver(), Settings.System.USER_ROTATION, 1);
            } catch (Exception ignored) {
            }
        }

        if (packageName == null || packageName.isEmpty()) return;

        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(packageName);
        if (launch == null) return;

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(launch);
    }
}
