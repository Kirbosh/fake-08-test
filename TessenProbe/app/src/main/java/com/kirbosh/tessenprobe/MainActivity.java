package com.kirbosh.tessenprobe;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private final List<AppEntry> apps = new ArrayList<>();
    private Spinner appSpinner;
    private CheckBox landscapeCheck;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadApps();
        restoreSelection();
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

        TextView title = makeText("Tessen Gaming Button", 28, primary);
        title.setTypeface(title.getTypeface(), 1);
        root.addView(title);

        TextView desc = makeText("ROG button scan 319 is confirmed. Pick the frontend you want the button to open.", 16, secondary);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(10);
        root.addView(desc, dlp);

        status = makeText("Setup needed", 18, accent);
        status.setPadding(dp(14), dp(14), dp(14), dp(14));
        status.setBackgroundColor(card);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(18);
        root.addView(status, slp);

        TextView choose = makeText("Frontend app", 16, primary);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(18);
        root.addView(choose, clp);

        appSpinner = new Spinner(this);
        root.addView(appSpinner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        landscapeCheck = new CheckBox(this);
        landscapeCheck.setText("Force landscape when ROG is pressed");
        landscapeCheck.setTextColor(primary);
        landscapeCheck.setChecked(true);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(12);
        root.addView(landscapeCheck, llp);

        Button save = makeButton("Save target");
        save.setOnClickListener(v -> saveSettings());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(14);
        root.addView(save, blp);

        Button accessibility = makeButton("Enable Tessen button service");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(8);
        root.addView(accessibility, alp);

        Button writeSettings = makeButton("Allow rotation control");
        writeSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wlp.topMargin = dp(8);
        root.addView(writeSettings, wlp);

        TextView note = makeText("The service only checks hardware key events and ignores normal Accessibility events. It does not scan the screen or inspect app content.", 14, secondary);
        note.setGravity(Gravity.START);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(16);
        root.addView(note, nlp);

        setContentView(root);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN, null);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> infos = pm.queryIntentActivities(launcher, 0);

        apps.clear();
        for (android.content.pm.ResolveInfo info : infos) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            CharSequence label = info.loadLabel(pm);
            apps.add(new AppEntry(label == null ? pkg : label.toString(), pkg));
        }

        Collections.sort(apps, Comparator.comparing(a -> a.label.toLowerCase()));
        List<String> labels = new ArrayList<>();
        for (AppEntry a : apps) labels.add(a.label + "  (" + a.packageName + ")");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        appSpinner.setAdapter(adapter);
    }

    private void restoreSelection() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String saved = prefs.getString("target_package", null);
        landscapeCheck.setChecked(prefs.getBoolean("force_landscape", true));
        if (saved != null) {
            for (int i = 0; i < apps.size(); i++) {
                if (apps.get(i).packageName.equals(saved)) {
                    appSpinner.setSelection(i);
                    status.setText("Ready for " + apps.get(i).label);
                    return;
                }
            }
        }
    }

    private void saveSettings() {
        int pos = appSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= apps.size()) {
            Toast.makeText(this, "No app selected", Toast.LENGTH_SHORT).show();
            return;
        }
        AppEntry app = apps.get(pos);
        getSharedPreferences("settings", MODE_PRIVATE).edit()
                .putString("target_package", app.packageName)
                .putBoolean("force_landscape", landscapeCheck.isChecked())
                .apply();
        status.setText("Ready for " + app.label);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private static class AppEntry {
        final String label;
        final String packageName;
        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }
}
