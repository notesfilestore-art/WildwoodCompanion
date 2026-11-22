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
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import android.text.Html;
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
  private volatile boolean stopDownload = false;
  private volatile boolean downloading = false;
  private final Object libraryLock = new Object();
  private static final String WIKI_API = "https://cattails-game.fandom.com/api.php?";

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
    @JavascriptInterface public void syncOffline() { startOfflineSync(); }
    @JavascriptInterface public void stopOffline() { stopDownload = true; }
    @JavascriptInterface public void listOffline() { new Thread(() -> sendOfflineList("")).start(); }
    @JavascriptInterface public void searchOffline(String term) { new Thread(() -> sendOfflineList(term)).start(); }
    @JavascriptInterface public void readOffline(String title) { new Thread(() -> loadOfflinePage(title)).start(); }
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


  // Offline library: download only on demand; never replace user notes or app storage.
  private File cacheFolder() {
    File f = new File(getFilesDir(), "wildwood_offline_v1");
    if (!f.exists()) f.mkdirs();
    return f;
  }

  private String cacheName(String title) {
    // Collision-resistant stable file names, no path traversal from titles.
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(title.getBytes(StandardCharsets.UTF_8));
      StringBuilder b = new StringBuilder();
      for (byte v : bytes) b.append(String.format(java.util.Locale.US, "%02x", v & 255));
      return b.toString() + ".json";
    } catch (Exception e) { return Integer.toHexString(title.hashCode()) + ".json"; }
  }

  private String readLimited(InputStream in, int maxBytes) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[8192]; int n;
    while ((n = in.read(buf)) >= 0) {
      out.write(buf, 0, n);
      if (out.size() > maxBytes) throw new Exception("Response exceeded size limit");
    }
    return out.toString("UTF-8");
  }

  private byte[] downloadBytes(String address, int maxBytes) throws Exception {
    URL url = new URL(address);
    if (!"https".equalsIgnoreCase(url.getProtocol())) throw new Exception("HTTPS required");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setConnectTimeout(16000); conn.setReadTimeout(25000);
    conn.setRequestProperty("User-Agent", "WildwoodCompanion/0.3 (personal offline reader; respectful sequential MediaWiki API requests)");
    conn.setRequestProperty("Accept", "application/json, text/html, image/webp, image/png, image/jpeg, */*");
    try {
      int code = conn.getResponseCode();
      if (code != 200) throw new Exception("Wiki HTTP " + code);
      if (conn.getContentLength() > maxBytes) throw new Exception("File too large");
      try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) != -1) {
          out.write(buf, 0, n);
          if (out.size() > maxBytes) throw new Exception("File too large");
        }
        return out.toByteArray();
      }
    } finally { conn.disconnect(); }
  }

  private String enc(String s) throws Exception { return URLEncoder.encode(s, "UTF-8"); }
  private JSONObject api(String query) throws Exception {
    byte[] content = downloadBytes(WIKI_API + query + "&format=json&maxlag=5", 7_000_000);
    JSONObject result = new JSONObject(new String(content, StandardCharsets.UTF_8));
    if (result.has("error")) throw new Exception(result.getJSONObject("error").optString("info", "Wiki API unavailable"));
    return result;
  }

  private LinkedHashSet<String> discoverPages() throws Exception {
    LinkedHashSet<String> pages = new LinkedHashSet<>();
    ArrayList<String> categories = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    categories.add("Category:Wildwood Story");
    categories.add("Category:Cats (Wildwood Story)");
    for (int k = 0; k < categories.size() && k < 85 && !stopDownload; k++) {
      String category = categories.get(k);
      if (!visited.add(category)) continue;
      String continuation = "";
      do {
        JSONObject result = api("action=query&list=categorymembers&cmtitle=" + enc(category) +
          "&cmlimit=100&cmtype=page%7Csubcat" + (continuation.isEmpty() ? "" : "&cmcontinue=" + enc(continuation)));
        JSONArray arr = result.getJSONObject("query").optJSONArray("categorymembers");
        if (arr != null) for (int i = 0; i < arr.length(); i++) {
          JSONObject node = arr.getJSONObject(i);
          int ns = node.optInt("ns", -1);
          String name = node.optString("title");
          if (ns == 0 && pages.size() < 650) pages.add(name);
          else if (ns == 14 && categories.size() < 85) categories.add(name);
        }
        continuation = result.optJSONObject("continue") == null ? "" :
          result.getJSONObject("continue").optString("cmcontinue", "");
      } while (!continuation.isEmpty() && pages.size() < 650 && !stopDownload);
    }
    pages.add("Cattails: Wildwood Story");
    return pages;
  }

  private String cleanHtml(String raw) {
    // No executable page scripts or resource loads are permitted in the sandboxed iframe.
    String html = raw.replaceAll("(?is)<(script|iframe|form|object|embed|style|noscript)\\b[^>]*>.*?</\\1\\s*>", "");
    html = html.replaceAll("(?is)<(script|iframe|form|object|embed|link|meta)\\b[^>]*?/?>", "");
    html = html.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*(\\\"[^\\\"]*\\\"|'[^']*'|[^\\s>]+)", "");
    html = html.replaceAll("(?i)javascript:", "#");
    // Strip embedded widgets & scripts; the iframe also has no script permissions.
    return html;
  }

  private String includeSmallImages(String html) {
    // Only the first two reasonably small images are cached per page, to limit storage.
    Pattern p = Pattern.compile("(?is)<img\\b[^>]*>");
    Matcher m = p.matcher(html);
    StringBuffer b = new StringBuffer(); int count = 0;
    while (m.find()) {
      String element = m.group();
      String link = "";
      Matcher src = Pattern.compile("(?i)(?:data-src|src)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']").matcher(element);
      while (src.find()) { String v = src.group(1).replace("&amp;", "&"); if (v.startsWith("https://")) { link = v; break; } }
      String replacement = "";
      if (count < 2 && !stopDownload && link.startsWith("https://") &&
          (link.contains("static.wikia.nocookie.net") || link.contains("fandom.com"))) {
        try {
          byte[] bytes = downloadBytes(link, 180_000);
          String mime = link.toLowerCase().contains(".png") ? "image/png" :
             link.toLowerCase().contains(".webp") ? "image/webp" : "image/jpeg";
          replacement = "<img loading=lazy alt=\"Wiki illustration\" src=\"data:" + mime + ";base64," +
             Base64.encodeToString(bytes, Base64.NO_WRAP) + "\">";
          count++;
        } catch (Exception ignored) { }
      }
      m.appendReplacement(b, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(b);
    return b.toString();
  }

  private void saveArticle(String title) throws Exception {
    JSONObject result = api("action=parse&page=" + enc(title) + "&prop=text%7Cdisplaytitle&formatversion=2");
    JSONObject parsed = result.getJSONObject("parse");
    String html = parsed.optString("text", "");
    if (html.isEmpty()) throw new Exception("Empty wiki page");
    html = includeSmallImages(cleanHtml(html));
    String text = Html.fromHtml(html.replaceAll("(?is)<img\\b[^>]*>", ""), Html.FROM_HTML_MODE_COMPACT).toString();
    JSONObject article = new JSONObject();
    article.put("title", title);
    article.put("text", text);
    article.put("html", html);
    article.put("url", "https://cattails-game.fandom.com/wiki/" + enc(title).replace("+", "_"));
    article.put("saved", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date()));
    byte[] bytes = article.toString().getBytes(StandardCharsets.UTF_8);
    if (bytes.length > 3_000_000) throw new Exception("Page exceeds 3 MB");
    File target = new File(cacheFolder(), cacheName(title));
    File temp = new File(cacheFolder(), cacheName(title) + ".tmp");
    try (FileOutputStream out = new FileOutputStream(temp)) { out.write(bytes); }
    if (!temp.renameTo(target)) throw new Exception("Unable to save article");
  }

  private void js(String method, String arguments) {
    runOnUiThread(() -> {
      if (homeView != null) homeView.evaluateJavascript(method + "(" + arguments + ")", null);
    });
  }
  private void progress(int done, int total, int failed, String message) {
    js("syncProgress", done + "," + total + "," + failed + "," + JSONObject.quote(message));
  }

  private void startOfflineSync() {
    if (downloading) return;
    downloading = true;
    stopDownload = false;
    new Thread(() -> {
      int done = 0, failed = 0, total = 0;
      try {
        progress(0, 0, 0, "Finding Wildwood Story pages");
        LinkedHashSet<String> names = discoverPages();
        total = names.size();
        progress(0, total, 0, "Downloading articles (can resume later)");
        for (String title : names) {
          if (stopDownload) break;
          File target = new File(cacheFolder(), cacheName(title));
          if (!target.exists()) {
            try { saveArticle(title); }
            catch (Exception e) { failed++; }
            try { Thread.sleep(500); } catch (InterruptedException ignored) { }
          }
          done++;
          if (done % 3 == 0 || done == total) progress(done, total, failed, "Downloading");
        }
        progress(done, total, failed, stopDownload ? "Stopped" : "Finished");
        sendOfflineList("");
      } catch (Exception e) {
        js("syncError", JSONObject.quote(e.getMessage() == null ? "Connection failed" : e.getMessage()));
      } finally { downloading = false; }
    }).start();
  }

  private void sendOfflineList(String search) {
    String query = search == null ? "" : search.toLowerCase(java.util.Locale.ROOT).trim();
    JSONArray found = new JSONArray();
    File[] files = cacheFolder().listFiles((dir, name) -> name.endsWith(".json"));
    int total = 0;
    if (files != null) {
      total = files.length;
      java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));
      for (File file : files) {
        try {
          String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
          JSONObject entry = new JSONObject(content);
          if (!query.isEmpty() && !entry.optString("title").toLowerCase(java.util.Locale.ROOT).contains(query) &&
              !entry.optString("text").toLowerCase(java.util.Locale.ROOT).contains(query)) continue;
          JSONObject summary = new JSONObject();
          summary.put("title", entry.optString("title"));
          String excerpt = entry.optString("text").replaceAll("\\s+", " ");
          summary.put("text", excerpt.substring(0, Math.min(180, excerpt.length())));
          found.put(summary);
        } catch (Exception ignored) { }
      }
    }
    js("updateLibrary", found.toString());
    js("setOfflineTotal", Integer.toString(total));
  }

  private void loadOfflinePage(String title) {
    if (title == null || title.length() > 300) return;
    File f = new File(cacheFolder(), cacheName(title));
    if (!f.isFile()) return;
    try {
      String raw = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
      js("offlineArticle", raw);
    } catch (Exception e) { js("syncError", JSONObject.quote("Could not read cached page")); }
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
