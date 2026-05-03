package com.jahid.multiplusmail;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.URLDecoder;

public class BaseContainerActivity extends AppCompatActivity {
    WebView webView;
    String id, name, provider, url;
    String lastSmsUrl = "";
    String smsNumber = "";
    String smsBody = "";
    TextView title;
    TextView outlookModeBtn;
    View helperBar;
    long lastRefresh = 0;
    boolean outlookAccountMode = false;
    long lastOutlookInboxRedirect = 0;

    public int slot() { return 1; }

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle b) {
        if (Build.VERSION.SDK_INT >= 28) {
            try { WebView.setDataDirectorySuffix("mail_slot_" + slot()); } catch (Exception ignored) {}
        }
        super.onCreate(b);
        setContentView(R.layout.activity_mail_webview);

        id = getIntent().getStringExtra("id");
        name = getIntent().getStringExtra("name");
        provider = getIntent().getStringExtra("provider");
        url = getIntent().getStringExtra("url");

        if (name == null || name.trim().isEmpty()) name = "Container " + slot();
        if ("gmail".equals(provider)) url = "https://accounts.google.com/";
        else if ("outlook".equals(provider)) url = isOutlookAlreadyLoggedIn() ? "https://outlook.live.com/mail/0/" : outlookCleanLoginUrl();
        else if ("yahoo".equals(provider)) url = "https://login.yahoo.com/";
        else if ("exchange".equals(provider)) url = "https://login.microsoftonline.com/";
        else if (url == null || url.trim().isEmpty()) url = "https://accounts.google.com/";

        webView = findViewById(R.id.webview);
        ProgressBar progress = findViewById(R.id.progress);
        title = findViewById(R.id.titleProvider);
        helperBar = findViewById(R.id.verifyHelperBar);
        outlookModeBtn = findViewById(R.id.btnOutlookMode);
        if (helperBar != null) helperBar.setVisibility(View.GONE);
        setupOutlookModeButton();
        updateTitle();

        findViewById(R.id.btnHome).setOnClickListener(v -> finish());
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
            else finish();
        });
        findViewById(R.id.btnRefresh).setOnClickListener(v -> smartRefresh());
        // Edit button removed in v21 UI.
try {
            findViewById(R.id.btnCopyNumber).setOnClickListener(v -> copy("SMS Number", smsNumber));
            findViewById(R.id.btnCopyText).setOnClickListener(v -> copy("SMS Text", smsBody));
            findViewById(R.id.btnShowSMS).setOnClickListener(v -> showSmsCard());
            findViewById(R.id.btnSent).setOnClickListener(v -> afterManualSent());
            findViewById(R.id.btnCheck).setOnClickListener(v -> smartRefresh());
        } catch (Exception ignored) {}

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 19) webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String currentUrl) {
                return handleUrl(currentUrl);
            }
            @Override public void onPageFinished(WebView view, String currentUrl) {
                super.onPageFinished(view, currentUrl);
                handleOutlookInboxRedirect(currentUrl);
                updateOutlookModeButton(currentUrl);
                try { CookieManager.getInstance().flush(); } catch (Exception ignored) {}
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int n) {
                progress.setProgress(n);
                progress.setAlpha(n == 100 ? 0f : 1f);
            }
            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView popup = new WebView(BaseContainerActivity.this);
                popup.getSettings().setJavaScriptEnabled(true);
                popup.getSettings().setDomStorageEnabled(true);
                popup.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        String u = request.getUrl().toString();
                        if (handleUrl(u)) return true;
                        webView.loadUrl(u);
                        return true;
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView v, String u) {
                        if (handleUrl(u)) return true;
                        webView.loadUrl(u);
                        return true;
                    }
                });
                WebView.WebViewTransport t = (WebView.WebViewTransport) resultMsg.obj;
                t.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }
        });

        prepareOutlookSafeLoginIfNeeded();
        webView.loadUrl(url);
    }




    String outlookFreshPrepareKey() {
        return "outlook_fresh_prepared_" + (id == null ? ("slot_" + slot()) : id);
    }

    boolean isOutlookFreshPrepared() {
        try {
            return getSharedPreferences("outlook_session_state", 0).getBoolean(outlookFreshPrepareKey(), false);
        } catch (Exception e) {
            return false;
        }
    }

    void markOutlookFreshPrepared() {
        try {
            getSharedPreferences("outlook_session_state", 0).edit().putBoolean(outlookFreshPrepareKey(), true).apply();
        } catch (Exception ignored) {}
    }

    void prepareOutlookSafeLoginIfNeeded() {
        // PC final style: fresh Outlook login only before first successful inbox.
        // Existing logged-in containers are never cleared.
        if (!isOutlookOrExchange()) return;
        if (isOutlookAlreadyLoggedIn()) return;
        if (isOutlookFreshPrepared()) return;

        try {
            webView.clearCache(true);
            webView.clearHistory();
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } catch (Exception ignored) {}

        markOutlookFreshPrepared();
    }


    String outlookCleanLoginUrl() {
        // Outlook-branded Microsoft login endpoint.
        // Still starts with login.live.com, but gives Microsoft the correct Outlook inbox reply target.
        return "https://login.live.com/login.srf?wa=wsignin1.0&wreply=https%3A%2F%2Foutlook.live.com%2Fmail%2F0%2F&id=292841&lc=1033";
    }

    String outlookLoginKey() {
        return "outlook_logged_in_" + (id == null ? ("slot_" + slot()) : id);
    }

    boolean isOutlookAlreadyLoggedIn() {
        if (!isOutlookOrExchange()) return false;
        try {
            return getSharedPreferences("outlook_session_state", 0).getBoolean(outlookLoginKey(), false);
        } catch (Exception e) {
            return false;
        }
    }

    void markOutlookLoggedIn() {
        if (!isOutlookOrExchange()) return;
        try {
            getSharedPreferences("outlook_session_state", 0).edit().putBoolean(outlookLoginKey(), true).apply();
            CookieManager.getInstance().flush();
        } catch (Exception ignored) {}
    }

    boolean isOutlookOrExchange() {
        return "outlook".equals(provider) || "exchange".equals(provider);
    }

    boolean isOutlookInboxUrl(String u) {
        if (u == null) return false;
        return u.toLowerCase().contains("outlook.live.com/mail");
    }

    boolean isMicrosoftPostLoginPage(String u) {
        if (u == null) return false;
        String l = u.toLowerCase();
        return l.contains("account.microsoft.com")
                || l.contains("myaccount.microsoft.com")
                || l.contains("account.live.com/proofs")
                || l.contains("account.live.com/recover")
                || l.contains("account.live.com/identity")
                || l.contains("login.live.com/ppsecure/post.srf")
                || l.contains("login.live.com/oauth20_desktop.srf")
                || l.contains("login.microsoftonline.com/common/oauth2")
                || l.contains("login.microsoftonline.com/consumers/oauth2")
                || ((l.contains("office.com") || l.contains("microsoft365.com")) && !l.contains("outlook"));
    }

    void handleOutlookInboxRedirect(String currentUrl) {
        // Minimal Outlook-only redirect.
        // Container isolation, WebView suffix, slot/process and login start URL stay exactly like v25.
        if (!isOutlookOrExchange() || currentUrl == null) return;
        if (outlookAccountMode) return;
        if (isOutlookInboxUrl(currentUrl)) {
            markOutlookFreshPrepared();
            markOutlookLoggedIn();
            return;
        }
        if (!isMicrosoftPostLoginPage(currentUrl)) return;

        long now = System.currentTimeMillis();
        if (now - lastOutlookInboxRedirect < 3000) return;
        lastOutlookInboxRedirect = now;

        webView.postDelayed(() -> {
            try {
                String u = webView.getUrl();
                if (u != null && !isOutlookInboxUrl(u) && isMicrosoftPostLoginPage(u)) {
                    webView.loadUrl("https://outlook.live.com/mail/0/");
                }
            } catch (Exception ignored) {}
        }, 800);
    }



    int dpRound(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    void setRoundIndicator(boolean active) {
        if (outlookModeBtn == null) return;
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(active ? 0xFFE11D48 : 0xFF16A34A);
        g.setStroke(dpRound(2), active ? 0xFFFF8AA0 : 0xFF7CFFB2);
        outlookModeBtn.setBackground(g);
        outlookModeBtn.setText("");
        if (Build.VERSION.SDK_INT >= 26) {
            outlookModeBtn.setTooltipText(active ? "Direct inbox mode active - tap to return account mode" : "Account mode - tap to open inbox");
        }
    }


    void setupOutlookModeButton() {
        if (outlookModeBtn == null) return;
        outlookModeBtn.setVisibility(View.GONE);
        outlookModeBtn.setOnClickListener(v -> toggleOutlookAccountMode());
    }


    void toggleOutlookAccountMode() {
        if (!isOutlookModeProvider()) return;
        outlookAccountMode = !outlookAccountMode;
        updateOutlookModeButton(webView == null ? null : webView.getUrl());
        if (outlookAccountMode) {
            try { webView.loadUrl("https://account.microsoft.com/"); } catch (Exception ignored) {}
        } else {
            try { webView.loadUrl("https://outlook.live.com/mail/0/"); } catch (Exception ignored) {}
        }
    }

    boolean isOutlookModeProvider() {
        return "outlook".equals(provider) || "exchange".equals(provider);
    }

    void updateOutlookModeButton(String currentUrl) {
        if (outlookModeBtn == null) return;
        if (!isOutlookModeProvider()) {
            outlookModeBtn.setVisibility(View.GONE);
            return;
        }
        boolean visible = outlookAccountMode || isOutlookInboxUrlSafe(currentUrl) || isMicrosoftAccountUrlSafe(currentUrl);
        if (!visible) {
            outlookModeBtn.setVisibility(View.GONE);
            return;
        }
        outlookModeBtn.setVisibility(View.VISIBLE);
        setRoundIndicator(outlookAccountMode);
    }

    boolean isOutlookInboxUrlSafe(String u) {
        return u != null && u.toLowerCase().contains("outlook.live.com/mail");
    }

    boolean isMicrosoftAccountUrlSafe(String u) {
        if (u == null) return false;
        String l = u.toLowerCase();
        return l.contains("account.microsoft.com")
                || l.contains("myaccount.microsoft.com")
                || l.contains("account.live.com/proofs")
                || l.contains("account.live.com/recover")
                || l.contains("account.live.com/identity");
    }


    void updateTitle() {
        title.setText(name);
        title.post(() -> {
            LinearGradient g = new LinearGradient(
                    0, 0, title.getWidth(), 0,
                    new int[]{0xFF7C3AED, 0xFFFF3DCE, 0xFF00DCFF, 0xFF7DF9FF},
                    null,
                    Shader.TileMode.CLAMP
            );
            title.getPaint().setShader(g);
            title.invalidate();
        });
    }

    boolean handleUrl(String u) {
        if (u == null) return false;
        String l = u.toLowerCase();

        if (l.startsWith("sms:") || l.startsWith("smsto:")) {
            captureSms(u);
            showVerifyHelper();
            showSmsCard();
            return true;
        }

        if (l.startsWith("intent:")) {
            try {
                android.content.Intent intent = android.content.Intent.parseUri(u, android.content.Intent.URI_INTENT_SCHEME);
                String scheme = intent.getScheme();
                if ("sms".equalsIgnoreCase(scheme) || "smsto".equalsIgnoreCase(scheme)) {
                    String data = intent.getDataString();
                    if (data != null) captureSms(data);
                    String body = intent.getStringExtra("sms_body");
                    if (body != null && body.trim().length() > 0) smsBody = body;
                    lastSmsUrl = u;
                    showVerifyHelper();
                    showSmsCard();
                    return true;
                }
            } catch (Exception ignored) {}
            return false;
        }
        return false;
    }

    void captureSms(String smsUrl) {
        lastSmsUrl = smsUrl;
        smsNumber = "";
        smsBody = "";
        try {
            Uri uri = Uri.parse(smsUrl);
            String schemeSpecific = uri.getSchemeSpecificPart();
            if (schemeSpecific != null) {
                String main = schemeSpecific;
                int q = main.indexOf("?");
                if (q >= 0) main = main.substring(0, q);
                smsNumber = main.replace("//", "").trim();
            }
            String body = uri.getQueryParameter("body");
            if (body == null) body = uri.getQueryParameter("sms_body");
            if (body != null) smsBody = body;

            if ((smsBody == null || smsBody.length() == 0) && smsUrl.contains("body=")) {
                int idx = smsUrl.indexOf("body=");
                String raw = smsUrl.substring(idx + 5);
                int amp = raw.indexOf("&");
                if (amp >= 0) raw = raw.substring(0, amp);
                smsBody = URLDecoder.decode(raw, "UTF-8");
            }
        } catch (Exception ignored) {}

        if (smsNumber == null) smsNumber = "";
        if (smsBody == null) smsBody = "";
    }

    void showVerifyHelper() {
        if (helperBar != null) helperBar.setVisibility(View.VISIBLE);
    }

    void showSmsCard() {
        String msg = "Number:\n" + (smsNumber.length() == 0 ? "Not captured" : smsNumber)
                + "\n\nSMS Text:\n" + (smsBody.length() == 0 ? "Not captured" : smsBody)
                + "\n\nSend this SMS manually, then tap Sent or Check.";
        new AlertDialog.Builder(this)
                .setTitle("Manual SMS Verification")
                .setMessage(msg)
                .setPositiveButton("Copy Number", (d, w) -> copy("SMS Number", smsNumber))
                .setNegativeButton("Copy Text", (d, w) -> copy("SMS Text", smsBody))
                .setNeutralButton("I Sent SMS", (d, w) -> afterManualSent())
                .show();
    }

    void copy(String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            Toast.makeText(this, label + " not captured", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, label + " copied", Toast.LENGTH_SHORT).show();
    }


    void clickSentSmsPageButton() {
        if (webView == null) return;
        try {
            webView.evaluateJavascript(
                    "(function(){"
                    + "function visible(e){var r=e.getBoundingClientRect();var s=getComputedStyle(e);return r.width>0&&r.height>0&&s.visibility!='hidden'&&s.display!='none'&&!e.disabled;}"
                    + "function text(e){return ((e.innerText||e.textContent||e.value||e.getAttribute('aria-label')||e.getAttribute('title')||'')+'').toLowerCase().replace(/\\\\s+/g,' ').trim();}"
                    + "var words=['i sent it','i have sent it',\\\"i've sent it\\\",'sent it','already sent','sent sms','i sent sms','sms sent','check','verify','next','continue'];"
                    + "var els=[].slice.call(document.querySelectorAll('button,input[type=button],input[type=submit],a,div[role=button],span[role=button]'));"
                    + "for(var i=0;i<els.length;i++){var e=els[i];if(!visible(e))continue;var t=text(e);"
                    + " for(var j=0;j<words.length;j++){if(t.indexOf(words[j])>=0){e.focus();e.click();return 'clicked:'+t;}}"
                    + "}"
                    + "return 'not_found';"
                    + "})()",
                    value -> {}
            );
        } catch (Exception ignored) {}
    }

    void afterManualSent() {
        Toast.makeText(this, "Checking verification...", Toast.LENGTH_SHORT).show();
        clickSentSmsPageButton();
        webView.postDelayed(this::clickSentSmsPageButton, 900);
        webView.postDelayed(this::clickSentSmsPageButton, 1800);
        webView.postDelayed(this::smartRefresh, 4200);
    }

    void smartRefresh() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < 800) return;
        lastRefresh = now;
        try {
            String current = webView.getUrl();
            if (current == null || current.trim().isEmpty()) webView.loadUrl(url);
            else webView.reload();
        } catch (Exception ignored) {}
    }

    void rename() {
        EditText input = new EditText(this);
        input.setText(name);
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("Rename Container")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String n = input.getText().toString().trim();
                    if (!n.isEmpty()) {
                        name = n;
                        MailStorage.rename(this, id, name);
                        updateTitle();
                        Toast.makeText(this, "Container updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override protected void onPause() {
        try { CookieManager.getInstance().flush(); } catch (Exception ignored) {}
        super.onPause();
    }

    @Override protected void onStop() {
        try { CookieManager.getInstance().flush(); } catch (Exception ignored) {}
        super.onStop();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
