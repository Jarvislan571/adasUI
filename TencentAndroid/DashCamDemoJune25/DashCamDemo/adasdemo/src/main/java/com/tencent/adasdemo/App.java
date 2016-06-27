package com.tencent.adasdemo;

import android.app.Application;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

/**
 * @author xiaojunzhou
 * @date 16/6/21
 */
public class App extends Application {

    private static TextToSpeech mTTS;

    @Override
    public void onCreate() {
        super.onCreate();
        mTTS = new TextToSpeech(getApplicationContext(), null);
    }

    public static void speak(String txt) {
        TextToSpeech tts = mTTS;
        if (mTTS != null && !TextUtils.isEmpty(txt)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(txt, TextToSpeech.QUEUE_ADD, null);
            } else {
                tts.speak(txt, TextToSpeech.QUEUE_ADD, null, null);
            }
        }
    }
}
