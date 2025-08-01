/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.cordova.statusbar;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

public class StatusBar extends CordovaPlugin {
    private static final String TAG = "StatusBar";

    private static final String ACTION_HIDE = "hide";
    private static final String ACTION_SHOW = "show";
    private static final String ACTION_READY = "_ready";
    private static final String ACTION_BACKGROUND_COLOR_BY_HEX_STRING = "backgroundColorByHexString";
    private static final String ACTION_NAVIGATION_BACKGROUND_COLOR_BY_HEX_STRING = "navigationBackgroundColorByHexString";
    private static final String ACTION_OVERLAYS_WEB_VIEW = "overlaysWebView";

    private AppCompatActivity activity;
    private Window window;

    private int currentStatusBarColor;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "StatusBar: initialization");
        super.initialize(cordova, webView);

        activity = this.cordova.getActivity();
        window = activity.getWindow();

        this.currentStatusBarColor = window.getStatusBarColor();

        activity.runOnUiThread(() -> {
            // Clear flag FLAG_FORCE_NOT_FULLSCREEN which is set initially
            // by the Cordova.
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

            // Read 'StatusBarOverlaysWebView' from config.xml, default is true.
            this.setOverlaysWebView(preferences.getBoolean("StatusBarOverlaysWebView", true));

            // Read 'StatusBarBackgroundColor' from config.xml, default is #000000.
            this.setStatusBarBackgroundColor(preferences.getString("StatusBarBackgroundColor", "#000000"));

            // Read 'NavigationBarBackgroundColor' from config.xml, default is #000000.
            this.setNavigationBarBackgroundColor(preferences.getString("NavigationBarBackgroundColor", "#000000"));
        });
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) {
        LOG.v(TAG, "Executing action: " + action);

        switch (action) {
            case ACTION_READY:
                WindowInsetsCompat windowInsetsCompat = ViewCompat.getRootWindowInsets(window.getDecorView());
                boolean isVisible = windowInsetsCompat != null && windowInsetsCompat.isVisible(WindowInsetsCompat.Type.statusBars());
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, isVisible));
                return true;

            case ACTION_SHOW:
                activity.runOnUiThread(() -> {
                    this.show();
                });
                return true;

            case ACTION_HIDE:
                activity.runOnUiThread(() -> {
                    this.hide();
                });
                return true;

            case ACTION_BACKGROUND_COLOR_BY_HEX_STRING:
                activity.runOnUiThread(() -> {
                    try {
                        this.setStatusBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                });
                return true;

            case ACTION_NAVIGATION_BACKGROUND_COLOR_BY_HEX_STRING:
                activity.runOnUiThread(() -> {
                    try {
                        this.setNavigationBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                });
                return true;

            case ACTION_OVERLAYS_WEB_VIEW:
                activity.runOnUiThread(() -> {
                    try {
                        this.setOverlaysWebView(args.getBoolean(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid boolean argument");
                    }
                });
                return true;

            default:
                return false;
        }
    }

    private void setStatusBarBackgroundColor(final String colorPref) {
        if (colorPref.isEmpty()) return;

        int color;
        try {
            color = Color.parseColor(colorPref);
        } catch (IllegalArgumentException ignore) {
            LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
            return;
        }

        // Decide foreground depending on background.
        final boolean lightTextNeeded = this.isLightTextNeeded(color);

        LOG.d(TAG, "Setting status bar color to " + colorPref + " foreground " + (lightTextNeeded ? "light" : "dark"));

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS); // SDK 19-30
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS); // SDK 21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // The function is deprecated in Android 15+.
            window.setStatusBarColor(color);
        }

        WindowInsetsControllerCompat winInsetsController = this.getInsetsController();
        if (winInsetsController == null) {
            return;
        }

        winInsetsController.setAppearanceLightStatusBars(!lightTextNeeded);
    }

    @SuppressWarnings("deprecation")
    public void setOverlaysWebView(Boolean overlays) {
        View decorView = window.getDecorView();
        int uiOptions = decorView.getSystemUiVisibility();
        if (overlays) {
            // Sets the layout to a fullscreen one that does not hide the actual status bar, so the WebView is displayed behind it.
            uiOptions = uiOptions | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                currentStatusBarColor = window.getStatusBarColor();
                // The function is deprecated in Android 15+.
                window.setStatusBarColor(Color.TRANSPARENT);
            }
        } else {
            // Sets the layout to a normal one that displays the WebView below the status bar.
            uiOptions = uiOptions & ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE & ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // recover the previous color of the status bar
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // The function is deprecated in Android 15+.
                window.setStatusBarColor(currentStatusBarColor);
            }
        }
    }

    private boolean isLightTextNeeded(int color) {
        return Color.luminance(color) < 0.5;
    }

    private void setNavigationBarBackgroundColor(final String colorPref) {
        if (colorPref.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return; // SDK 26

        int color;
        try {
            color = Color.parseColor(colorPref);
        } catch (IllegalArgumentException ignore) {
            LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // The function is deprecated in Android 15+.
            window.setNavigationBarColor(color);
        }

        final boolean lightTextNeeded = this.isLightTextNeeded(color);

        LOG.d(TAG, "Setting navigation bar color to " + colorPref + " foreground " + (lightTextNeeded ? "light" : "dark"));

        WindowInsetsControllerCompat winInsetsController = this.getInsetsController();
        if (winInsetsController == null) {
            return;
        }

        winInsetsController.setAppearanceLightNavigationBars(!lightTextNeeded);
    }

    private void hide() {
        WindowInsetsControllerCompat winInsetsController = this.getInsetsController();
        if (winInsetsController == null) {
            return;
        }
        winInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }

    private void show() {
        WindowInsetsControllerCompat winInsetsController = this.getInsetsController();
        if (winInsetsController == null) {
            return;
        }
        winInsetsController.show(WindowInsetsCompat.Type.systemBars());
    }

    private WindowInsetsControllerCompat getInsetsController() {
        View decorView = window.getDecorView();
        return WindowCompat.getInsetsController(window, decorView);
    }

}
