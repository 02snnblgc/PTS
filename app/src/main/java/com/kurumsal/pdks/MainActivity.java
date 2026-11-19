package com.kurumsal.pdks;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                checkNfcStatus();
            }
        });

        webView.loadUrl("file:///android_asset/index.html");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
    }

    private void checkNfcStatus() {
        String status;
        if (nfcAdapter == null) {
            status = "Cihazda NFC Yok";
        } else if (!nfcAdapter.isEnabled()) {
            status = "NFC Kapalı (Ayarlardan Açın)";
        } else {
            status = "📡 NFC Aktif";
        }
        webView.evaluateJavascript("if(window.updateNfcHardwareStatus) window.updateNfcHardwareStatus('" + status + "');", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNfcStatus();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                String uid = bytesToHex(tag.getId());
                webView.evaluateJavascript("if(window.onNfcCardScanned) window.onNfcCardScanned('" + uid + "');", null);
            }
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}