package com.kirbosh.tessenprobe;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class TessenButtonService extends AccessibilityService {
    private static final int ROG_SCAN_CODE = 319;
    private static final int ASUS_VENDOR_ID = 2821;
    private static final int TESSEN_PRODUCT_ID = 6884;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getScanCode() != ROG_SCAN_CODE) return false;
        if (event.getDevice() == null) return false;
        if (event.getDevice().getVendorId() != ASUS_VENDOR_ID) return false;
        if (event.getDevice().getProductId() != TESSEN_PRODUCT_ID) return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            launchTarget();
        }
        return true;
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

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(launch);
    }
}
