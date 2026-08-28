package com.chiranjeevi.voicetr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale

private data class Language(val name: String, val speechTag: String, val translateCode: String)

private val LANGUAGES = listOf(
    Language("English", "en-IN", "en"),
    Language("Hindi", "hi-IN", "hi"),
    Language("Tamil", "ta-IN", "ta")
)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(this, "Text-to-speech is unavailable", Toast.LENGTH_SHORT).show()
            }
        }
        setContent { VoiceTranslatorApp() }
    }

    fun speak(text: String, lang: Language) {
        val locale = when (lang.translateCode) {
            "hi" -> Locale("hi", "IN")
            "ta" -> Locale("ta", "IN")
            else -> Locale("en", "IN")
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voicetr-output")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTranslatorApp() {
    val context = LocalContext.current
    val activity = context as MainActivity
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf(LANGUAGES[0]) }
    var target by remember { mutableStateOf(LANGUAGES[1]) }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) isListening = true
        else Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_LONG).show()
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(Unit) {
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() { }
            override fun onRmsChanged(rmsdB: Float) { }
            override fun onBufferReceived(buffer: ByteArray?) { }
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(context, "Could not recognize speech. Try again.", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) inputText = matches.first()
            }
            override fun onPartialResults(partialResults: Bundle?) { }
            override fun onEvent(eventType: Int, params: Bundle?) { }
            override fun onListenerSuspend() { isListening = false }
            override fun onListenerResume() { isListening = true }
        })
        onDispose { speechRecognizer?.destroy() }
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, source.speechTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    fun translate() {
        if (inputText.isBlank()) {
            Toast.makeText(context, "Enter or speak some text first", Toast.LENGTH_SHORT).show()
            return
        }
        isTranslating = true
        errorMessage = null
        scope.launch {
            try {
                outputText = TranslationService.translate(inputText, source.translateCode, target.translateCode)
            } catch (e: Exception) {
                errorMessage = "Translation failed: ${e.message ?: "Check your network or API setup."}"
            } finally {
                isTranslating = false
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Voice Translator", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("English • Hindi • Tamil", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Speak in one language and translate it into another, then listen to the result.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LanguageDropdown("From", source, Modifier.weight(1f)) { source = it }
                    LanguageDropdown("To", target, Modifier.weight(1f)) { target = it }
                }

                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Your speech", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            placeholder = { Text("Tap the microphone and start speaking…") }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { startListening() }) {
                                Text(if (isListening) "Listening…" else "🎙 Speak")
                            }
                        }
                    }
                }

                Button(
                    onClick = { translate() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isTranslating
                ) {
                    Text(if (isTranslating) "Translating…" else "Translate")
                }

                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Translation", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = outputText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            placeholder = { Text("Your translation will appear here") }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = { if (outputText.isNotBlank()) activity.speak(outputText, target) },
                                enabled = outputText.isNotBlank()
                            ) { Text("🔊 Listen") }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                Text("Tip: Keep your device online for cloud translation.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    label: String,
    selected: Language,
    modifier: Modifier,
    onSelected: (Language) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LANGUAGES.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.name) },
                    onClick = { onSelected(language); expanded = false }
                )
            }
        }
    }
}
