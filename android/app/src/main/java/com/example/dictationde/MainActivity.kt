package com.example.dictationde

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggle: Button
    private lateinit var tvRecognizedText: TextView
    private lateinit var tvError: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening: Boolean = false
    private var committedText: String = ""
    private var currentPartial: String = ""

    // Request RECORD_AUDIO runtime permission
    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                hideError()
                startDictation()
            } else {
                showError("Chyba: RECORD_AUDIO zamítnuto – Aplikace vyžaduje oprávnění k mikrofonu.")
                stopDictation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggle = findViewById(R.id.btnToggle)
        tvRecognizedText = findViewById(R.id.tvRecognizedText)
        tvError = findViewById(R.id.tvError)

        btnToggle.setOnClickListener {
            if (isListening) {
                stopDictation()
            } else {
                checkPermissionAndStart()
            }
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            hideError()
            startDictation()
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun initializeSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showError("Chyba: SpeechRecognizer není na tomto zařízení dostupný.")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // Ready to listen
                }

                override fun onBeginningOfSpeech() {
                    // User started speaking
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Ignored per requirements: no volume indicator
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer
                }

                override fun onEndOfSpeech() {
                    // Phrase ended; recognizer will produce onResults or onError
                }

                override fun onError(error: Int) {
                    val (codeName, meaning) = getErrorDetails(error)
                    // Display error code and meaning
                    showError("Chyba $error ($codeName): $meaning")
                    stopDictation()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val finalResult = matches?.firstOrNull()?.trim() ?: ""

                    currentPartial = ""
                    committedText = finalResult
                    tvRecognizedText.text = committedText

                    stopDictation()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text =
                        partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            ?: ""

                    currentPartial = text
                    val displayText = if (committedText.isNotEmpty() && currentPartial.isNotEmpty()) {
                        "$committedText $currentPartial"
                    } else if (committedText.isNotEmpty()) {
                        committedText
                    } else {
                        currentPartial
                    }
                    tvRecognizedText.text = displayText
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Additional events
                }
            })
        }
    }

    private fun startDictation() {
        isListening = true
        committedText = ""
        currentPartial = ""
        tvRecognizedText.text = ""
        btnToggle.text = getString(R.string.btn_stop)
        btnToggle.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F"))

        initializeSpeechRecognizer()
        listen()
    }

    private fun listen() {
        if (!isListening) return

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                // Explicitly German language: de-DE
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "de-DE")
                // Enable partial results
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Request speech recognition without external dialogs or sound prompts where supported
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            showError("Chyba při spuštění: ${e.localizedMessage}")
            stopDictation()
        }
    }

    private fun stopDictation() {
        isListening = false
        btnToggle.text = getString(R.string.btn_start)
        btnToggle.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor("#111111"))

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    private fun getErrorDetails(errorCode: Int): Pair<String, String> {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO ->
                "ERROR_AUDIO" to "Chyba nahrávání zvuku mikrofonem."
            SpeechRecognizer.ERROR_CLIENT ->
                "ERROR_CLIENT" to "Obecná klientská chyba rozpoznávače."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "ERROR_INSUFFICIENT_PERMISSIONS" to "Chybí oprávnění RECORD_AUDIO."
            SpeechRecognizer.ERROR_NETWORK ->
                "ERROR_NETWORK" to "Chyba síťové komunikace."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "ERROR_NETWORK_TIMEOUT" to "Vypršel časový limit sítě."
            SpeechRecognizer.ERROR_NO_MATCH ->
                "ERROR_NO_MATCH" to "Nebyl rozpoznán žádný německý text."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "ERROR_RECOGNIZER_BUSY" to "Služba rozpoznávání je zaneprázdněna."
            SpeechRecognizer.ERROR_SERVER ->
                "ERROR_SERVER" to "Chyba serveru Google řečových služeb."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "ERROR_SPEECH_TIMEOUT" to "Vypršel limit ticha (žádná řeč)."
            else ->
                "ERROR_UNKNOWN" to "Neznámý chybový kód."
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.text = ""
        tvError.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDictation()
    }
}
