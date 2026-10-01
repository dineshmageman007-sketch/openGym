package com.opengym.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST_CODE = 1;
    private static final int EXPORT_REQUEST_CODE = 2;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setSupportMultipleWindows(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // Lets the web app save backup files (window.OpenGymNative.saveFile)
        webView.addJavascriptInterface(new NativeBridge(), "OpenGymNative");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    Intent intent;
                    String[] types = params.getAcceptTypes();
                    boolean imageOnly = types != null && types.length > 0;
                    if (imageOnly) {
                        for (String t : types) {
                            if (t == null || !t.startsWith("image/")) {
                                imageOnly = false;
                                break;
                            }
                        }
                    }
                    if (imageOnly) {
                        // photo / GIF pickers: keep the normal behaviour
                        intent = params.createIntent();
                    } else {
                        // backup (.json) and CSV imports: no type filter, so every
                        // file stays selectable whatever mime type it was saved with
                        intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        }
                    }
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE);
                } catch (Exception e) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/www/index.html");
    }

    /** Methods here are callable from JS as window.OpenGymNative.<method>(...) */
    private class NativeBridge {
        @JavascriptInterface
        public boolean saveFile(final String fileName, final String content) {
            pendingExport = content;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/json");
                        intent.putExtra(Intent.EXTRA_TITLE, fileName);
                        startActivityForResult(intent, EXPORT_REQUEST_CODE);
                    } catch (Exception e) {
                        pendingExport = null;
                        toast("Export failed: " + e.getMessage());
                    }
                }
            });
            return true;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void writeExport(Uri uri) {
        String data = pendingExport;
        pendingExport = null;
        if (data == null) {
            return;
        }
        try {
            OutputStream out = getContentResolver().openOutputStream(uri, "wt");
            if (out == null) {
                throw new IOException("Could not open the selected file");
            }
            try {
                out.write(data.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } finally {
                out.close();
            }
            toast("Backup saved");
        } catch (Exception e) {
            toast("Export failed: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (filePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                ClipData clip = data.getClipData();
                if (clip != null) {
                    int count = clip.getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = clip.getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }

        if (requestCode == EXPORT_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                writeExport(data.getData());
            } else {
                pendingExport = null;
                toast("Export cancelled");
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
