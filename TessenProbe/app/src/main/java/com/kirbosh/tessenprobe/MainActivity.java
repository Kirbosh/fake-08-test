package com.kirbosh.tessenprobe;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private TextView status;
    private TextView logView;
    private final StringBuilder report = new StringBuilder();
    private long startedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startedAt = SystemClock.elapsedRealtime();
        buildUi();
        appendHeader();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView makeText(String text, float size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private void buildUi() {
        int bg = Color.rgb(15, 17, 21);
        int card = Color.rgb(27, 30, 36);
        int primary = Color.rgb(245, 247, 250);
        int secondary = Color.rgb(180, 186, 198);
        int accent = Color.rgb(255, 42, 114);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));
        root.setBackgroundColor(bg);

        TextView title = makeText("Tessen Probe", 28, primary);
        title.setTypeface(title.getTypeface(), 1);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView instructions = makeText(
                "Attach the ROG Tessen, then press the ROG button once. This app records the Android key event and controller identity. It uses no Accessibility service and no background service.",
                16,
                secondary
        );
        LinearLayout.LayoutParams instructionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        instructionParams.topMargin = dp(10);
        root.addView(instructions, instructionParams);

        status = makeText("Waiting for input…", 20, accent);
        status.setPadding(dp(16), dp(16), dp(16), dp(16));
        status.setBackgroundColor(card);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(18);
        root.addView(status, statusParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);

        Button copy = makeButton("Copy report");
        copy.setOnClickListener(v -> copyReport());
        buttons.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button clear = makeButton("Clear");
        clear.setOnClickListener(v -> {
            report.setLength(0);
            appendHeader();
            status.setText("Waiting for input…");
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clearParams.leftMargin = dp(8);
        buttons.addView(clear, clearParams);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(12);
        root.addView(buttons, buttonParams);

        logView = makeText("", 14, primary);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logView.setTextIsSelectable(true);
        logView.setPadding(dp(14), dp(14), dp(14), dp(14));
        logView.setBackgroundColor(card);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(logView, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(12);
        root.addView(scroll, scrollParams);

        setContentView(root);
        root.setFocusableInTouchMode(true);
        root.requestFocus();
    }

    private void appendHeader() {
        report.append("Tessen Probe 1.0\n");
        report.append("Android API: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
        report.append("Device: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
        report.append("\nPress the ROG button once.\n\n");
        refreshLog();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        recordKeyEvent(event);
        if (event.getScanCode() == 319) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void recordKeyEvent(KeyEvent event) {
        InputDevice input = event.getDevice();
        String action = event.getAction() == KeyEvent.ACTION_DOWN ? "DOWN" : event.getAction() == KeyEvent.ACTION_UP ? "UP" : String.valueOf(event.getAction());
        String keyName = KeyEvent.keyCodeToString(event.getKeyCode());
        String source = String.format(Locale.US, "0x%08X", event.getSource());

        StringBuilder block = new StringBuilder();
        block.append("KEY EVENT\n");
        block.append("action: ").append(action).append("\n");
        block.append("keyCode: ").append(event.getKeyCode()).append(" (").append(keyName).append(")\n");
        block.append("scanCode: ").append(event.getScanCode()).append("\n");
        block.append("repeatCount: ").append(event.getRepeatCount()).append("\n");
        block.append("source: ").append(source).append("\n");
        block.append("deviceId: ").append(event.getDeviceId()).append("\n");
        block.append("flags: ").append(String.format(Locale.US, "0x%08X", event.getFlags())).append("\n");
        block.append("metaState: ").append(String.format(Locale.US, "0x%08X", event.getMetaState())).append("\n");

        if (input != null) {
            block.append("deviceName: ").append(input.getName()).append("\n");
            block.append("descriptor: ").append(input.getDescriptor()).append("\n");
            block.append("vendorId: ").append(input.getVendorId()).append("\n");
            block.append("productId: ").append(input.getProductId()).append("\n");
            block.append("deviceSources: ").append(String.format(Locale.US, "0x%08X", input.getSources())).append("\n");
            block.append("external: ").append(input.isExternal()).append("\n");
        }

        block.append("timeSinceOpenMs: ").append(SystemClock.elapsedRealtime() - startedAt).append("\n\n");
        report.append(block);

        if (event.getScanCode() == 319) {
            status.setText("ROG button captured ✓  scan 319  " + keyName);
        } else {
            status.setText("Captured " + keyName + "  scan " + event.getScanCode());
        }
        refreshLog();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        InputDevice input = event.getDevice();
        if (input != null && (event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            status.setText("Joystick motion seen from " + input.getName());
        }
        return super.dispatchGenericMotionEvent(event);
    }

    private void refreshLog() {
        if (logView != null) {
            logView.setText(report.toString());
        }
    }

    private void copyReport() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Tessen Probe report", report.toString()));
        Toast.makeText(this, "Report copied. Paste it into ChatGPT.", Toast.LENGTH_LONG).show();
    }
}
