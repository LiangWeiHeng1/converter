package com.example.converter;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private EditText fixedInput, msgInput;
    private TextView result1Text, result2Text, hitCount, missCount;
    private Button pasteBtn, clearBtn, copy1Btn, copy2Btn, copyAllBtn;
    private String lastHitResult = "", lastMissResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fixedInput = findViewById(R.id.fixedInput);
        msgInput = findViewById(R.id.msgInput);
        result1Text = findViewById(R.id.result1Text);
        result2Text = findViewById(R.id.result2Text);
        hitCount = findViewById(R.id.hitCount);
        missCount = findViewById(R.id.missCount);
        pasteBtn = findViewById(R.id.pasteBtn);
        clearBtn = findViewById(R.id.clearBtn);
        copy1Btn = findViewById(R.id.copy1Btn);
        copy2Btn = findViewById(R.id.copy2Btn);
        copyAllBtn = findViewById(R.id.copyAllBtn);

        pasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        String text = clip.getItemAt(0).getText().toString();
                        if (text != null && !text.trim().isEmpty()) {
                            msgInput.setText(text.trim());
                            doConvert();
                            pasteBtn.setText("✅ 已粘贴");
                            pasteBtn.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    pasteBtn.setText("📋 粘贴");
                                }
                            }, 1500);
                        }
                    }
                }
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                msgInput.setText("");
                result1Text.setText("无数据");
                result2Text.setText("无数据");
                hitCount.setText("共 0 个");
                missCount.setText("共 0 个");
                lastHitResult = "";
                lastMissResult = "";
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { doConvert(); }
        };
        fixedInput.addTextChangedListener(watcher);
        msgInput.addTextChangedListener(watcher);

        copy1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyText(lastHitResult, copy1Btn);
            }
        });
        copy2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyText(lastMissResult, copy2Btn);
            }
        });
        copyAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String combined = (lastHitResult + " " + lastMissResult).trim();
                String[] parts = combined.split(" ");
                Set<String> set = new TreeSet<>();
                for (String p : parts) {
                    if (!p.isEmpty()) set.add(p);
                }
                String output = String.join(" ", set);
                copyText(output, copyAllBtn);
            }
        });
    }

    private void copyText(String text, final Button btn) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", text);
        clipboard.setPrimaryClip(clip);
        String originalText = btn.getText().toString();
        btn.setText("✅ 已复制");
        btn.postDelayed(new Runnable() {
            @Override
            public void run() {
                btn.setText(originalText);
            }
        }, 1500);
    }

    private void doConvert() {
        String msg = msgInput.getText().toString().trim();
        if (msg.isEmpty()) {
            result1Text.setText("无数据");
            result2Text.setText("无数据");
            hitCount.setText("共 0 个");
            missCount.setText("共 0 个");
            lastHitResult = "";
            lastMissResult = "";
            return;
        }

        Set<String> fixedSet = new HashSet<>();
        String fixedText = fixedInput.getText().toString().trim();
        if (!fixedText.isEmpty()) {
            String[] nums = fixedText.split("\\D+");
            for (String n : nums) {
                if (!n.isEmpty()) {
                    int val = Integer.parseInt(n);
                    if (val >= 1 && val <= 49) {
                        fixedSet.add(String.format("%02d", val));
                    }
                }
            }
        }

        Map<String, Integer> dict = new HashMap<>();
        String[] lines = msg.split("\n");
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            for (String part : parts) {
                if (part.contains("=")) {
                    String[] sp = part.split("=");
                    if (sp.length == 2) {
                        try {
                            int val = Integer.parseInt(sp[0].trim());
                            int amount = Integer.parseInt(sp[1].trim());
                            if (val >= 1 && val <= 49) {
                                String key = String.format("%02d", val);
                                dict.put(key, dict.getOrDefault(key, 0) + amount);
                            }
                        } catch (NumberFormatException e) {}
                    }
                }
            }
        }

        if (dict.isEmpty()) {
            result1Text.setText("解析失败");
            result2Text.setText("解析失败");
            hitCount.setText("共 0 个");
            missCount.setText("共 0 个");
            return;
        }

        Map<String, Integer> hitDict = new HashMap<>();
        Map<String, Integer> missDict = new HashMap<>();
        int total = 0;
        for (Map.Entry<String, Integer> entry : dict.entrySet()) {
            String key = entry.getKey();
            int val = entry.getValue();
            total += val;
            if (fixedSet.contains(key)) {
                hitDict.put(key, val);
            } else {
                missDict.put(key, val);
            }
        }

        int hitRaw = 0;
        for (String k : dict.keySet()) {
            if (fixedSet.contains(k)) hitRaw++;
        }
        int missRaw = total - hitRaw;

        if (!hitDict.isEmpty()) {
            result1Text.setText(formatResult(hitDict));
            lastHitResult = formatResultPlain(hitDict);
            hitCount.setText("共 " + hitDict.size() + " 个（原始 " + hitRaw + " 个）");
        } else {
            result1Text.setText("无命中号码");
            lastHitResult = "";
            hitCount.setText("共 0 个");
        }

        if (!missDict.isEmpty()) {
            result2Text.setText(formatResult(missDict));
            lastMissResult = formatResultPlain(missDict);
            missCount.setText("共 " + missDict.size() + " 个（原始 " + missRaw + " 个）");
        } else {
            result2Text.setText("全部命中");
            lastMissResult = "";
            missCount.setText("共 0 个");
        }
    }

    private String formatResult(Map<String, Integer> dict) {
        List<String> keys = new ArrayList<>(dict.keySet());
        Collections.sort(keys);
        List<String> items = new ArrayList<>();
        for (String k : keys) {
            items.add(k + "=" + dict.get(k));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i));
            if ((i + 1) % 4 == 0) sb.append("\n");
            else if (i < items.size() - 1) sb.append("  ");
        }
        return sb.toString();
    }

    private String formatResultPlain(Map<String, Integer> dict) {
        List<String> keys = new ArrayList<>(dict.keySet());
        Collections.sort(keys);
        List<String> items = new ArrayList<>();
        for (String k : keys) {
            items.add(k + "=" + dict.get(k));
        }
        return String.join(" ", items);
    }
}