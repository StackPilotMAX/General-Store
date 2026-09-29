package com.stackpilotmax.rameshvegetableshop

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

private data class VoiceUiState(
    val listening: Boolean = false,
    val status: String = "Hindi • Marathi • Hinglish",
    val heard: String = ""
)

@Composable
internal fun VoiceVegetableButton(
    vegetables: List<VegetableOption>,
    onMatched: (VegetableOption) -> Unit
) {
    val context = LocalContext.current
    val latestVegetables = rememberUpdatedState(vegetables)
    val latestOnMatched = rememberUpdatedState(onMatched)
    var uiState by remember { mutableStateOf(VoiceUiState()) }

    val applyHypotheses: (List<String>) -> Unit = { rawHypotheses ->
        val hypotheses = rawHypotheses.filter { it.isNotBlank() }
        if (hypotheses.isEmpty()) {
            uiState = VoiceUiState(
                listening = false,
                status = "Voice result khaali tha — dobara boliye"
            )
        } else {
            val match = VoiceVegetableMatcher.bestMatch(
                hypotheses = hypotheses,
                vegetables = latestVegetables.value
            )
            if (match != null) {
                uiState = VoiceUiState(
                    listening = false,
                    status = "✓ ${match.vegetable.name} ka Rate & Qty menu khul gaya",
                    heard = match.heard
                )
                latestOnMatched.value(match.vegetable)
            } else {
                uiState = VoiceUiState(
                    listening = false,
                    status = "Sabzi match nahi hui — dobara dheere boliye",
                    heard = hypotheses.firstOrNull().orEmpty()
                )
            }
        }
    }

    val systemVoiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val hypotheses = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                .orEmpty()
            applyHypotheses(hypotheses)
        } else {
            uiState = VoiceUiState(
                listening = false,
                status = "Voice input cancel hua — mic dabakar dobara boliye"
            )
        }
    }

    val launchSystemVoice: (String) -> Unit = { reason ->
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sabzi ka naam boliye")
        }
        uiState = VoiceUiState(listening = true, status = reason)
        runCatching { systemVoiceLauncher.launch(intent) }
            .onFailure {
                uiState = VoiceUiState(
                    listening = false,
                    status = "Phone ka system voice input bhi available nahi hai"
                )
            }
    }

    val controller = remember(context) {
        VegetableSpeechController(context.applicationContext) { event ->
            when (event) {
                VoiceRecognitionEvent.Ready -> {
                    uiState = VoiceUiState(
                        listening = true,
                        status = "Sun raha hoon… sabzi ka naam boliye"
                    )
                }

                is VoiceRecognitionEvent.Info -> {
                    uiState = VoiceUiState(
                        listening = true,
                        status = event.message,
                        heard = uiState.heard
                    )
                }

                is VoiceRecognitionEvent.Partial -> {
                    uiState = VoiceUiState(
                        listening = true,
                        status = "Sun raha hoon…",
                        heard = event.text
                    )
                }

                is VoiceRecognitionEvent.Final -> applyHypotheses(event.hypotheses)

                is VoiceRecognitionEvent.UseSystemDialog -> launchSystemVoice(event.reason)

                is VoiceRecognitionEvent.Failure -> {
                    uiState = VoiceUiState(
                        listening = false,
                        status = event.message
                    )
                }

                VoiceRecognitionEvent.Cancelled -> {
                    uiState = VoiceUiState(
                        listening = false,
                        status = "Voice cancelled — mic dabakar dobara boliye"
                    )
                }
            }
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.destroy() }
    }

    val startListening = {
        if (controller.isAvailable) {
            controller.startListening()
        } else {
            launchSystemVoice("System voice input khol raha hoon…")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            uiState = VoiceUiState(
                status = "Mic permission allow karein, tab voice selection chalega"
            )
        }
    }

    Column(
        modifier = Modifier.widthIn(max = 210.dp),
        horizontalAlignment = Alignment.End
    ) {
        Button(
            onClick = {
                if (uiState.listening) {
                    controller.cancel()
                } else if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text(
                if (uiState.listening) "Sun raha…" else "Bolkar Sabzi",
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = uiState.heard.takeIf { it.isNotBlank() }
                ?.let { "“$it” • ${uiState.status}" }
                ?: uiState.status,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 3,
            textAlign = TextAlign.End,
            color = if (uiState.listening) LeafGreen else MutedText
        )
    }
}

private sealed interface VoiceRecognitionEvent {
    data object Ready : VoiceRecognitionEvent
    data class Info(val message: String) : VoiceRecognitionEvent
    data class Partial(val text: String) : VoiceRecognitionEvent
    data class Final(val hypotheses: List<String>) : VoiceRecognitionEvent
    data class UseSystemDialog(val reason: String) : VoiceRecognitionEvent
    data class Failure(val message: String) : VoiceRecognitionEvent
    data object Cancelled : VoiceRecognitionEvent
}

private enum class RecognizerMode {
    ON_DEVICE,
    SYSTEM
}

private class VegetableSpeechController(
    context: Context,
    private val onEvent: (VoiceRecognitionEvent) -> Unit
) : RecognitionListener {
    private val appContext = context.applicationContext
    private val onDeviceSupported =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)

    private var mode = if (onDeviceSupported) RecognizerMode.ON_DEVICE else RecognizerMode.SYSTEM
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var fallbackAttempted = false
    private var cancelledByUser = false

    init {
        recognizer = createRecognizer(mode)
        if (recognizer == null && mode == RecognizerMode.ON_DEVICE) {
            mode = RecognizerMode.SYSTEM
            recognizer = createRecognizer(mode)
        }
    }

    val isAvailable: Boolean
        get() = recognizer != null

    private fun createRecognizer(targetMode: RecognizerMode): SpeechRecognizer? = runCatching {
        when (targetMode) {
            RecognizerMode.ON_DEVICE -> {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
                ) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
                } else {
                    null
                }
            }

            RecognizerMode.SYSTEM -> {
                if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
                    SpeechRecognizer.createSpeechRecognizer(appContext)
                } else {
                    null
                }
            }
        }
    }.getOrNull()?.also { it.setRecognitionListener(this) }

    private fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, mode == RecognizerMode.ON_DEVICE)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Sabzi ka naam boliye")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            putStringArrayListExtra(
                RecognizerIntent.EXTRA_BIASING_STRINGS,
                ArrayList(VoiceVegetableMatcher.biasingPhrases())
            )
        }
    }

    fun startListening() {
        cancelledByUser = false
        fallbackAttempted = false
        startCurrentRecognizer()
    }

    private fun startCurrentRecognizer() {
        val service = recognizer ?: run {
            if (mode == RecognizerMode.ON_DEVICE && switchToSystemRecognizer()) {
                onEvent(VoiceRecognitionEvent.Info("Offline voice unavailable — Android voice try kar raha hoon…"))
                startCurrentRecognizer()
            } else {
                onEvent(
                    VoiceRecognitionEvent.UseSystemDialog(
                        "Speech service issue — system voice input khol raha hoon…"
                    )
                )
            }
            return
        }

        listening = true
        runCatching { service.startListening(recognitionIntent()) }
            .onFailure {
                listening = false
                if (
                    mode == RecognizerMode.ON_DEVICE &&
                    !fallbackAttempted &&
                    switchToSystemRecognizer()
                ) {
                    fallbackAttempted = true
                    onEvent(VoiceRecognitionEvent.Info("Offline Hindi model nahi mila — Android voice try kar raha hoon…"))
                    startCurrentRecognizer()
                } else {
                    onEvent(
                        VoiceRecognitionEvent.UseSystemDialog(
                            "Voice service start nahi hua — system voice input try kar raha hoon…"
                        )
                    )
                }
            }
    }

    private fun switchToSystemRecognizer(): Boolean {
        recognizer?.destroy()
        recognizer = null
        listening = false
        mode = RecognizerMode.SYSTEM
        recognizer = createRecognizer(mode)
        return recognizer != null
    }

    private fun shouldFallbackFromOnDevice(error: Int): Boolean = error in setOf(
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_CLIENT,
        11,
        12,
        13,
        14,
        15
    )

    private fun shouldUseSystemDialog(error: Int): Boolean = error in setOf(
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_CLIENT,
        11,
        12,
        13,
        14,
        15
    )

    fun cancel() {
        cancelledByUser = true
        recognizer?.cancel()
        listening = false
        onEvent(VoiceRecognitionEvent.Cancelled)
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        listening = false
    }

    override fun onReadyForSpeech(params: Bundle?) {
        listening = true
        onEvent(VoiceRecognitionEvent.Ready)
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        onEvent(VoiceRecognitionEvent.Info("Naam check kar raha hoon…"))
    }

    override fun onError(error: Int) {
        listening = false
        if (cancelledByUser) {
            cancelledByUser = false
            return
        }

        if (
            mode == RecognizerMode.ON_DEVICE &&
            !fallbackAttempted &&
            shouldFallbackFromOnDevice(error)
        ) {
            fallbackAttempted = true
            if (switchToSystemRecognizer()) {
                onEvent(VoiceRecognitionEvent.Info("Offline Hindi speech problem — Android voice try kar raha hoon…"))
                startCurrentRecognizer()
                return
            }
        }

        if (mode == RecognizerMode.SYSTEM && shouldUseSystemDialog(error)) {
            onEvent(
                VoiceRecognitionEvent.UseSystemDialog(
                    "Android speech service problem — system voice input try kar raha hoon…"
                )
            )
            return
        }

        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Mic audio error — dobara try karein"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission allow karein"
            SpeechRecognizer.ERROR_NO_MATCH -> "Naam clear nahi mila — sirf sabzi ka naam boliye"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer busy hai — ek second baad try karein"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Awaaz nahi mili — mic ke paas sabzi ka naam boliye"
            else -> "Voice error ($error) — dobara try karein"
        }
        onEvent(VoiceRecognitionEvent.Failure(message))
    }

    override fun onResults(results: Bundle?) {
        listening = false
        val hypotheses = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            .orEmpty()
            .filter { it.isNotBlank() }
        if (hypotheses.isEmpty()) {
            onEvent(VoiceRecognitionEvent.Failure("Voice result khaali tha — dobara boliye"))
        } else {
            onEvent(VoiceRecognitionEvent.Final(hypotheses))
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        if (partial.isNotBlank()) onEvent(VoiceRecognitionEvent.Partial(partial))
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

internal data class VoiceVegetableMatch(
    val vegetable: VegetableOption,
    val heard: String,
    val matchedAlias: String,
    val confidence: Double
)

internal object VoiceVegetableMatcher {
    private data class AliasRecord(
        val vegetableName: String,
        val original: String,
        val compact: String
    )

    private val aliasesByVegetable = linkedMapOf(
        "Methi" to listOf("methi", "मेथी", "मेथी भाजी"),
        "Palak" to listOf("palak", "पालक", "spinach"),
        "Soya" to listOf("soya", "सोया", "shepu", "शेपू", "dill"),
        "Hara Kanda" to listOf(
            "hara kanda", "हरा कांदा", "हिरवा कांदा", "green onion", "spring onion"
        ),
        "China Kothmir" to listOf(
            "china kothmir", "china kothimbir", "चाइना कोथिंबीर", "चायना कोथिंबीर",
            "china dhaniya", "चाइना धनिया", "china coriander"
        ),
        "Pudina" to listOf("pudina", "पुदीना", "mint"),
        "Chaulai" to listOf("chaulai", "चौलाई", "lal math", "लाल माठ", "माठ"),
        "Mooli ke Patte" to listOf(
            "mooli ke patte", "मूली के पत्ते", "muli ke patte", "मुळ्याची पाने",
            "radish leaves"
        ),
        "Desi Kothmir" to listOf(
            "desi kothmir", "desi kothimbir", "देसी कोथिंबीर", "देशी कोथिंबीर",
            "dhaniya", "धनिया", "kothimbir", "कोथिंबीर", "coriander"
        ),
        "Mooli" to listOf("mooli", "muli", "मूली", "मुळा", "radish"),
        "Hari Mirch" to listOf(
            "hari mirch", "हरी मिर्च", "mirchi", "मिर्ची", "hirvi mirchi",
            "हिरवी मिरची", "green chilli", "green chili"
        ),
        "Naya Adrak" to listOf(
            "naya adrak", "नया अदरक", "navin adrak", "नवीन अदरक", "fresh ginger"
        ),
        "Purana Adrak" to listOf(
            "purana adrak", "पुराना अदरक", "juna adrak", "जुना अदरक", "old ginger"
        ),
        "Kadi Patta" to listOf(
            "kadi patta", "kadhi patta", "कड़ी पत्ता", "करी पत्ता", "कढीपत्ता",
            "curry leaves", "curry leaf"
        ),
        "Nimbu" to listOf("nimbu", "नींबू", "limbu", "लिंबू", "lemon"),
        "Gobhi" to listOf("gobhi", "gobi", "गोभी", "फूलगोभी", "cauliflower"),
        "Shimla Mirch" to listOf(
            "shimla mirch", "शिमला मिर्च", "dhobli mirchi", "ढोबळी मिरची",
            "capsicum", "bell pepper"
        ),
        "Chota Lahsun" to listOf(
            "chota lahsun", "छोटा लहसुन", "choti lasun", "छोटी लसूण", "small garlic"
        ),
        "Bada Lahsun" to listOf(
            "bada lahsun", "बड़ा लहसुन", "mothi lasun", "मोठी लसूण", "big garlic"
        ),
        "Kakdi" to listOf(
            "kakdi", "ककड़ी", "kakadi", "काकडी", "kheera", "खीरा", "cucumber"
        )
    )

    private val aliasRecords: List<AliasRecord> = aliasesByVegetable
        .flatMap { (name, aliases) ->
            aliases.map { alias ->
                AliasRecord(
                    vegetableName = name,
                    original = alias,
                    compact = compact(alias)
                )
            }
        }
        .filter { it.compact.isNotBlank() }

    fun biasingPhrases(): List<String> = aliasesByVegetable.values.flatten().distinct()

    fun bestMatch(
        hypotheses: List<String>,
        vegetables: List<VegetableOption>
    ): VoiceVegetableMatch? {
        if (hypotheses.isEmpty() || vegetables.isEmpty()) return null
        val available = vegetables.associateBy { it.name }

        return hypotheses
            .asSequence()
            .filter { it.isNotBlank() }
            .flatMap { heard ->
                candidatesFor(heard)
                    .asSequence()
                    .mapNotNull { candidate ->
                        val vegetable = available[candidate.first.vegetableName] ?: return@mapNotNull null
                        VoiceVegetableMatch(
                            vegetable = vegetable,
                            heard = heard,
                            matchedAlias = candidate.first.original,
                            confidence = candidate.second
                        )
                    }
            }
            .maxByOrNull { it.confidence }
    }

    private fun candidatesFor(heard: String): List<Pair<AliasRecord, Double>> {
        val heardCompact = compact(heard)
        if (heardCompact.isBlank()) return emptyList()

        val windows = spokenWindows(heard)
        return aliasRecords.mapNotNull { alias ->
            val directMatch = heardCompact.contains(alias.compact)
            val bestDistance = windows.minOfOrNull { levenshtein(it, alias.compact) }
                ?: return@mapNotNull null
            val denominator = max(alias.compact.length, 1)
            val ratio = bestDistance.toDouble() / denominator.toDouble()
            val allowedDistance = max(1, alias.compact.length / 4)
            val fuzzyMatch = bestDistance <= allowedDistance && ratio <= 0.24
            if (!directMatch && !fuzzyMatch) return@mapNotNull null

            val coverage = alias.compact.length.toDouble() /
                max(heardCompact.length, alias.compact.length).toDouble()
            val accuracy = if (directMatch) 1.0 else 1.0 - ratio
            val wordSpecificity = minOf(3, spaced(alias.original).split(' ').size) * 0.02
            alias to (coverage * 0.65 + accuracy * 0.30 + wordSpecificity)
        }
    }

    private fun spokenWindows(value: String): Set<String> {
        val words = spaced(value).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptySet()
        val result = linkedSetOf(compact(value))
        val maximumWindow = minOf(4, words.size)
        for (size in 1..maximumWindow) {
            for (start in 0..words.size - size) {
                result += words.subList(start, start + size).joinToString(separator = "")
            }
        }
        return result.filterTo(linkedSetOf()) { it.isNotBlank() }
    }

    private fun compact(value: String): String = spaced(value).replace(" ", "")

    private fun spaced(value: String): String {
        val normalized = Normalizer.normalize(
            value.lowercase(Locale.ROOT),
            Normalizer.Form.NFKC
        )
        return buildString(normalized.length) {
            normalized.forEach { character ->
                append(if (character.isLetterOrDigit()) character else ' ')
            }
        }.trim().replace(Regex("\\s+"), " ")
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitution = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + substitution
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }
}
