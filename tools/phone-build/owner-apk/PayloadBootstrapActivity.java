package dev.ror2owner.bootstrap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;

public final class PayloadBootstrapActivity extends Activity {
    private static final String ORIGIN = "https://ror2.launcher.local/";
    private static final String ASSETS = "owner-bootstrap/";
    private static final int PICK_ZIP_REQUEST = 1001;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Backend singleton;
    private WebView web;
    private Backend backend;
    private boolean handingOff;

    static {
        try {
            System.loadLibrary("ror2_x64_bridge");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    public static native String getNativeArchInfo();

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        try {
            synchronized (PayloadBootstrapActivity.class) {
                if (singleton == null) singleton = new Backend(getApplicationContext());
                backend = singleton;
            }
            backend.attach(this);
            web = new WebView(this);
            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setDomStorageEnabled(true);
            web.addJavascriptInterface(backend, "OwnerBootstrapHost");
            web.addJavascriptInterface(backend, "OwnerPayload");
            web.setWebViewClient(new Client());
            setContentView(web);
            web.loadUrl(ORIGIN + "index.html");
        } catch (Throwable t) {
            launchUnityDirectly();
        }
    }

    @Override protected void onDestroy() {
        if (backend != null) backend.detach(this);
        backend = null;
        if (web != null) web.destroy();
        web = null;
        super.onDestroy();
    }

    public void openZipPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите ZIP-архив с файлами Risk of Rain 2"), PICK_ZIP_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ZIP_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (backend != null) {
                backend.importZipUri(data.getData());
            }
        }
    }

    private void launchUnityDirectly() {
        if (handingOff) return;
        handingOff = true;
        try {
            Intent intent = new Intent();
            intent.setClassName(this, "com.unity3d.player.UnityPlayerActivity");
            startActivity(intent);
        } catch (Throwable ignored) {
        }
        finish();
    }

    private final class Client extends WebViewClient {
        @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (!url.startsWith(ORIGIN)) return null;
            String path = url.substring(ORIGIN.length());
            if (path.isEmpty()) path = "index.html";
            try {
                InputStream is = getAssets().open(ASSETS + path);
                String mime = path.endsWith(".html") ? "text/html"
                        : path.endsWith(".js") ? "application/javascript"
                        : path.endsWith(".css") ? "text/css"
                        : path.endsWith(".png") ? "image/png"
                        : "application/octet-stream";
                return new WebResourceResponse(mime, "UTF-8", is);
            } catch (IOException e) {
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null, new ByteArrayInputStream(new byte[0]));
            }
        }
    }

    public static final class Backend {
        private final Context app;
        private final PayloadStore store;
        private final ProfileStore profiles;
        private WeakReference<PayloadBootstrapActivity> activityRef = new WeakReference<>(null);
        private volatile String statusOverride = null;
        private volatile Integer progressOverride = null;
        private volatile String messageOverride = null;

        public Backend(Context context) {
            this.app = context.getApplicationContext();
            this.store = new PayloadStore(app);
            this.profiles = new ProfileStore(app);
        }

        public synchronized void attach(PayloadBootstrapActivity act) {
            this.activityRef = new WeakReference<>(act);
        }

        public synchronized void detach(PayloadBootstrapActivity act) {
            if (this.activityRef.get() == act) {
                this.activityRef = new WeakReference<>(null);
            }
        }

        public void importZipUri(Uri uri) {
            statusOverride = "preparing";
            progressOverride = 5;
            messageOverride = "Распаковка игровых файлов в ror2_payload/...";
            new Thread(() -> {
                File outDir = new File(app.getFilesDir(), "ror2_payload");
                if (!outDir.exists()) outDir.mkdirs();
                int count = 0;
                byte[] buffer = new byte[65536];
                try (InputStream is = app.getContentResolver().openInputStream(uri);
                     ZipInputStream zis = new ZipInputStream(is)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        File outFile = new File(outDir, entry.getName());
                        if (!outFile.getCanonicalPath().startsWith(outDir.getCanonicalPath())) {
                            continue;
                        }
                        if (entry.isDirectory()) {
                            outFile.mkdirs();
                        } else {
                            File parent = outFile.getParentFile();
                            if (parent != null && !parent.exists()) parent.mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                int len;
                                while ((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                            count++;
                            progressOverride = Math.min(95, 5 + (count % 90));
                            messageOverride = "Распаковано файлов: " + count + " (" + entry.getName() + ")";
                        }
                        zis.closeEntry();
                    }
                    statusOverride = "ready";
                    progressOverride = 100;
                    messageOverride = "Готово! Импортировано файлов: " + count + ". Нажмите Play для запуска.";
                } catch (Throwable t) {
                    statusOverride = "error";
                    progressOverride = null;
                    messageOverride = "Ошибка распаковки ZIP: " + t.getMessage();
                }
            }).start();
        }

        @JavascriptInterface public String getState() {
            try {
                JSONObject obj = new JSONObject();
                boolean hasPayload = store.getSummaryJson().optBoolean("exists", false);
                String st = statusOverride != null ? statusOverride : (hasPayload ? "ready" : "idle");
                obj.put("status", st);
                if (progressOverride != null) {
                    obj.put("progress", progressOverride);
                } else {
                    obj.put("progress", "ready".equals(st) ? 100 : JSONObject.NULL);
                }
                String msg = messageOverride != null ? messageOverride
                        : ("ready".equals(st)
                            ? "Файлы игры найдены в ror2_payload/. Нажмите Play для запуска (" + Build.CPU_ABI + ")."
                            : "Нажмите 'Select game files', чтобы выбрать ZIP с игрой прямо на телефоне (" + Build.CPU_ABI + ").");
                obj.put("message", msg);
                JSONObject caps = new JSONObject();
                caps.put("selectFiles", true);
                caps.put("prepare", true);
                caps.put("pause", false);
                caps.put("resume", false);
                caps.put("retry", true);
                caps.put("launch", true);
                obj.put("capabilities", caps);
                return obj.toString();
            } catch (Throwable t) {
                return "{\"status\":\"idle\",\"progress\":null,\"message\":\"Ready\",\"capabilities\":{\"selectFiles\":true,\"prepare\":true,\"launch\":true}}";
            }
        }

        @JavascriptInterface public boolean invoke(String action) {
            if ("launch".equals(action)) {
                MAIN.post(() -> {
                    PayloadBootstrapActivity act = activityRef.get();
                    if (act != null) act.launchUnityDirectly();
                });
                return true;
            }
            if ("selectFiles".equals(action) || "prepare".equals(action) || "retry".equals(action)) {
                MAIN.post(() -> {
                    PayloadBootstrapActivity act = activityRef.get();
                    if (act != null) act.openZipPicker();
                });
                return true;
            }
            return true;
        }
    }
}
