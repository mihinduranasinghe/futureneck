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

    Button btnTranslate, btnReadText, btnDetectObject, btnProcessWithAi, btnQRScan, btnWeatherNow;
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
        btnQRScan = findViewById(R.id.btnQRScan);
        btnWeatherNow = findViewById(R.id.btnWeatherNow);
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
        btnQRScan.setOnClickListener(v -> performAction("futureneckQRScanner"));
        btnWeatherNow.setOnClickListener(v -> performAction("iot_TellstickDuo/script"));
    }

    private void performAction(String action) {
        System.out.println("Button clicked: " + action);
        String command = "export OPENAI_API_KEY=\"<OpenAIAPIKey>\" && python3 /home/pi/futureneck/" + action + ".py";
        new SSHCommandExecutor(action.equals("futureneckProcessWithAI")).execute(command);
    }


    private class SSHCommandExecutor extends AsyncTask<String, Void, String> {
        private final boolean isProcessWithAi;

        public SSHCommandExecutor(boolean isProcessWithAi) {
            this.isProcessWithAi = isProcessWithAi;
        }
        private static final String HOSTNAME = "192.168.0.112"; //Change this whenever you change the IP address of the Raspberry Pi
        private static final String USERNAME = "pi";
        private static final String PASSWORD = "<Password>";
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
            StringBuilder output = new StringBuilder();

            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(USERNAME, HOSTNAME, PORT);
                session.setPassword(PASSWORD);

                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect();

                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                InputStream in = channel.getInputStream();
                channel.connect();

                byte[] buffer = new byte[1024];
                int i;
                while ((i = in.read(buffer)) != -1) {
                    output.append(new String(buffer, 0, i));
                }

                channel.disconnect();
                session.disconnect();

                Log.d("SSHCommandExecutor", "Command execution completed");
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }

            return output.toString();
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
