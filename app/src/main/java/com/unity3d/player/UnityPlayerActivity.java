package com.unity3d.player;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import java.io.File;

public class UnityPlayerActivity extends Activity {
    static {
        try {
            System.loadLibrary("ror2_x64_bridge");
            System.loadLibrary("il2cpp");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File payloadDir = new File(getFilesDir(), "ror2_payload");
        String[] files = payloadDir.exists() ? payloadDir.list() : null;
        int count = files != null ? files.length : 0;

        TextView view = new TextView(this);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(0xFF182B31);
        view.setTextColor(0xFFF0F1DF);
        view.setTextSize(17f);
        view.setPadding(48, 48, 48, 48);
        String abi = (Build.SUPPORTED_64_BIT_ABIS != null && Build.SUPPORTED_64_BIT_ABIS.length > 0)
                ? Build.SUPPORTED_64_BIT_ABIS[0]
                : Build.CPU_ABI;
        view.setText("Risk of Rain 2 · Native Runtime (" + abi + ")\n\n"
                + "Файлов в ror2_payload/: " + count + "\n"
                + "Путь: " + payloadDir.getAbsolutePath());
        setContentView(view);
    }
}
