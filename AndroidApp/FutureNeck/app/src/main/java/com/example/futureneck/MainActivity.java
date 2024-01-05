package com.example.futureneck;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private ProgressDialog progressDialog;

    Button btnTranslate, btnReadText, btnDetectObject, btnProcessWithAi;
    TextView textViewOutput;
    TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.show();
        progressDialog.setContentView(R.layout.custom_progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        progressDialog.setCancelable(false);
        progressDialog.dismiss();

        // Initialize buttons and TextView
        btnTranslate = findViewById(R.id.btnTranslate);
        btnReadText = findViewById(R.id.btnReadText);
        btnDetectObject = findViewById(R.id.btnDetectObject);
        btnProcessWithAi = findViewById(R.id.btnProcessAI);
        textViewOutput = findViewById(R.id.textViewOutput);
        textViewOutput.setMovementMethod(new ScrollingMovementMethod());

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set click listeners
        setButtonListeners();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TextToSpeech", "This Language is not supported");
            }
        } else {
            Log.e("TextToSpeech", "Initialization Failed!");
        }
    }

    private void setButtonListeners() {
        btnTranslate.setOnClickListener(v -> performAction("futureneckTranslation"));
        btnReadText.setOnClickListener(v -> performAction("futureneckTextDetection"));
        btnDetectObject.setOnClickListener(v -> performAction("futureneckObjectDetection"));
        btnProcessWithAi.setOnClickListener(v -> performAction("futureneckProcessWithAI"));
    }

    private void performAction(String action) {
        System.out.println("Button clicked: " + action);
        String command = "export OPENAI_API_KEY=\"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\" && python3 /home/pi/futureneck/" + action + ".py";
        new SSHCommandExecutor(action.equals("futureneckProcessWithAI")).execute(command);
    }


    private class SSHCommandExecutor extends AsyncTask<String, Void, String> {
        private final boolean isProcessWithAi;

        public SSHCommandExecutor(boolean isProcessWithAi) {
            this.isProcessWithAi = isProcessWithAi;
        }
        private static final String HOSTNAME = "192.168.100.105";
        private static final String USERNAME = "pi";
        private static final String PASSWORD = "IoT@2021";
        private static final int PORT = 22;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... commands) {
            String command = commands[0];
            Log.d("SSHCommandExecutor", "Executing command: " + command);
            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(USERNAME, HOSTNAME, PORT);
                session.setPassword(PASSWORD);

                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect();

                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(commands[0]);
                InputStream in = channel.getInputStream();
                channel.connect();

                StringBuilder output = new StringBuilder();
                byte[] buffer = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(buffer, 0, 1024);
                        if (i < 0) break;
                        output.append(new String(buffer, 0, i));
                    }
                    if (channel.isClosed()) {
                        break;
                    }
                    Thread.sleep(1000);
                }
                channel.disconnect();
                session.disconnect();

                Log.d("SSHCommandExecutor", "Command execution completed");
                return output.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d("SSHCommandExecutor", "Result: " + result);

            String content;
            if (isProcessWithAi) {
                content = "Let's see this through Futureneck AI, " + extractContentFromResponse(result);
            } else {
                content = result;
            }
            textViewOutput.setText(content);

            if (!content.isEmpty()) {
                increaseVolume();
                textToSpeech.speak(content, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }

        private void increaseVolume() {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        }

        private String extractContentFromResponse(String response) {
            String contentPrefix = "content=\"";
            String contentSuffix = "\", role='assistant'";
            int startIndex = response.indexOf(contentPrefix);
            if (startIndex != -1) {
                startIndex += contentPrefix.length();
                int endIndex = response.indexOf(contentSuffix, startIndex);
                if (endIndex != -1) {
                    return response.substring(startIndex, endIndex);
                }
            } else {
                contentPrefix = "content=\'";
                contentSuffix = "\', role='assistant'";
                startIndex = response.indexOf(contentPrefix);
                startIndex += contentPrefix.length();
                if (startIndex != -1) {
                    startIndex += contentPrefix.length();
                    int endIndex = response.indexOf(contentSuffix, startIndex);
                    if (endIndex != -1) {
                        return response.substring(startIndex, endIndex);
                    }
                }
            }
            return "Content not found in the response";
        }
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // Shutdown TextToSpeech to release resources
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
