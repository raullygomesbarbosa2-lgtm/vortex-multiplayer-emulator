package com.bytevortex.vortexonline;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 8241;
    private static final String HOME = "https://raullygomesbarbosa2-lgtm.github.io/vortex-multiplayer-emulator/";
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff06100c);
        getWindow().setNavigationBarColor(0xff06100c);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        webView.setBackgroundColor(0xff06100c);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try { startActivityForResult(intent, PICK_FILE); return true; }
                catch (ActivityNotFoundException e) { fileCallback = null; Toast.makeText(MainActivity.this, "Não encontrei um seletor de arquivos.", Toast.LENGTH_LONG).show(); return false; }
            }
            @Override public void onShowCustomView(View view, CustomViewCallback callback) { getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN); }
            @Override public void onHideCustomView() { getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE); }
        });
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        if (state == null) webView.loadUrl(HOME); else webView.restoreState(state);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            Uri[] results = resultCode == RESULT_OK && data != null && data.getData() != null ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); if (webView != null) webView.saveState(out); }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if (webView != null) { webView.destroy(); webView = null; } super.onDestroy(); }
}
