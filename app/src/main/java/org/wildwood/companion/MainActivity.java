package org.wildwood.companion;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
  private static final int EXPORT_REQUEST = 100;
  private static final int IMPORT_REQUEST = 101;
  private WebView homeView;
  private WebView wikiView;
  private LinearLayout wikiPanel;
  private TextView wikiTitle;
  private String pendingExport;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(0xFF253C2D);
    getWindow().setNavigationBarColor(0xFF253C2D);

    FrameLayout root = new FrameLayout(this);
    homeView = new WebView(this);
    homeView.setBackgroundColor(0xFFF5EBD5);
    WebSettings settings = homeView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(true);
    settings.setAllowFileAccessFromFileURLs(false);
    settings.setAllowUniversalAccessFromFileURLs(false);
    homeView.addJavascriptInterface(new BackupBridge(), "AndroidBridge");
    homeView.setWebChromeClient(new WebChromeClient());
    homeView.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
        if (!request.isForMainFrame()) return false;
        Uri url = request.getUrl();
        if ("file".equalsIgnoreCase(url.getScheme())) return false;
        if (isWikiUrl(url)) { openWiki(url); return true; }
        openExternal(url);
        return true;
      }
    });
    root.addView(homeView, new FrameLayout.LayoutParams(-1, -1));

    // A separate reader keeps all offline tabs and user-entered notes in memory.
    wikiPanel = new LinearLayout(this);
    wikiPanel.setOrientation(LinearLayout.VERTICAL);
    wikiPanel.setBackgroundColor(0xFFF4EAD3);
    LinearLayout toolbar = new LinearLayout(this);
    toolbar.setOrientation(LinearLayout.HORIZONTAL);
    toolbar.setBackgroundColor(0xFF304937);
    Button back = readerButton("‹ Back");
    Button refresh = readerButton("↻");
    Button close = readerButton("✕ Close");
    wikiTitle = new TextView(this);
    wikiTitle.setText("Wildwood Wiki");
    wikiTitle.setTextColor(0xFFFFF6DF);
    wikiTitle.setTextSize(14);
    wikiTitle.setSingleLine(true);
    wikiTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
    wikiTitle.setGravity(android.view.Gravity.CENTER_VERTICAL);
    toolbar.addView(back);
    toolbar.addView(wikiTitle, new LinearLayout.LayoutParams(0, -1, 1));
    toolbar.addView(refresh);
    toolbar.addView(close);
    wikiPanel.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(48)));
    wikiView = new WebView(this);
    wikiView.setBackgroundColor(0xFFFFF6DF);
    WebSettings wikiSettings = wikiView.getSettings();
    wikiSettings.setJavaScriptEnabled(true); // Fandom pages require scripting.
    wikiSettings.setDomStorageEnabled(true);
    wikiSettings.setAllowFileAccess(false);
    wikiSettings.setAllowContentAccess(false);
    wikiSettings.setJavaScriptCanOpenWindowsAutomatically(false);
    wikiSettings.setSupportMultipleWindows(false);
    wikiSettings.setBuiltInZoomControls(true);
    wikiSettings.setDisplayZoomControls(false);
    wikiView.setWebChromeClient(new WebChromeClient());
    wikiView.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
        if (!request.isForMainFrame()) return false;
        Uri url = request.getUrl();
        if (isWikiUrl(url)) return false;
        openExternal(url);
        return true;
      }
      @Override public void onPageFinished(WebView v, String url) {
        String title = v.getTitle();
        wikiTitle.setText(title == null || title.trim().isEmpty() ? "Wildwood Wiki" : title);
      }
    });
    wikiPanel.addView(wikiView, new LinearLayout.LayoutParams(-1, 0, 1));
    wikiPanel.setVisibility(View.GONE);
    root.addView(wikiPanel, new FrameLayout.LayoutParams(-1, -1));
    back.setOnClickListener(v -> { if (wikiView.canGoBack()) wikiView.goBack(); else closeWiki(); });
    refresh.setOnClickListener(v -> wikiView.reload());
    close.setOnClickListener(v -> closeWiki());
    setContentView(root);
    homeView.loadUrl("file:///android_asset/index.html");
  }

  private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }

  private Button readerButton(String title) {
    Button b = new Button(this);
    b.setText(title);
    b.setTextSize(12);
    b.setAllCaps(false);
    b.setTextColor(0xFFFFF6DF);
    b.setBackgroundColor(0xFF425F45);
    b.setMinWidth(0);
    b.setMinimumWidth(0);
    b.setPadding(dp(10), 0, dp(10), 0);
    return b;
  }

  private boolean isWikiUrl(Uri url) {
    String scheme = url.getScheme();
    String host = url.getHost();
    return ("https".equalsIgnoreCase(scheme)) && host != null &&
        (host.equalsIgnoreCase("cattails-game.fandom.com") ||
         host.equalsIgnoreCase("cattailsgame.fandom.com"));
  }

  private void openWiki(Uri url) {
    wikiTitle.setText("Wildwood Wiki");
    wikiPanel.setVisibility(View.VISIBLE);
    wikiView.loadUrl(url.toString());
  }

  private void closeWiki() {
    wikiPanel.setVisibility(View.GONE);
    wikiView.loadUrl("about:blank"); // Stops video/ads using resources after closing.
  }

  private void openExternal(Uri uri) {
    String scheme = uri.getScheme();
    if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) return;
    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
    catch (Exception ignored) { Toast.makeText(this, "No browser available for this link", Toast.LENGTH_SHORT).show(); }
  }

  public class BackupBridge {
    @JavascriptInterface public void exportBackup(String json) {
      runOnUiThread(() -> {
        pendingExport = json;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "wildwood-companion-backup.json");
        startActivityForResult(intent, EXPORT_REQUEST);
      });
    }
    @JavascriptInterface public void importBackup() {
      runOnUiThread(() -> {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, IMPORT_REQUEST);
      });
    }
  }

  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    Uri uri = data.getData();
    try {
      if (request == EXPORT_REQUEST && pendingExport != null) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
          if (out != null) out.write(pendingExport.getBytes(StandardCharsets.UTF_8));
        }
        pendingExport = null;
      } else if (request == IMPORT_REQUEST) {
        byte[] bytes;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
          if (in == null) return;
          java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
          byte[] chunk = new byte[4096]; int n;
          while ((n = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
            if (buffer.size() > 2_000_000) throw new IllegalArgumentException("Backup is too large");
          }
          bytes = buffer.toByteArray();
        }
        String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
        homeView.evaluateJavascript("importFromNative('" + encoded + "')", null);
      }
    } catch (Exception ex) {
      String msg = ex.getMessage() == null ? "File operation failed" : ex.getMessage();
      Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
  }

  @Override public void onBackPressed() {
    if (wikiPanel.getVisibility() == View.VISIBLE) {
      if (wikiView.canGoBack()) wikiView.goBack(); else closeWiki();
    } else if (homeView.canGoBack()) homeView.goBack();
    else super.onBackPressed();
  }

  @Override public void onDestroy() {
    if (wikiView != null) wikiView.destroy();
    if (homeView != null) homeView.destroy();
    super.onDestroy();
  }
}
