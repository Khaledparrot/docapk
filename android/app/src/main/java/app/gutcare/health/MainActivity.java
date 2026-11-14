package app.gutcare.health;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GutAlarmPlugin.class);
        registerPlugin(GutStepsPlugin.class);
        super.onCreate(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    if (bridge != null && bridge.getWebView() != null) {
                        bridge.getWebView().evaluateJavascript(
                                "(window.gutcareBack && window.gutcareBack()) ? 'true' : 'false'",
                                value -> {
                                    String v = value == null ? "" : value.replace("\"", "");
                                    if (!"true".equals(v)) finish();
                                });
                        return;
                    }
                } catch (Exception e) {
                }
                finish();
            }
        });
    }
}
