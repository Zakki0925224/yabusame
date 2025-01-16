package io.github.zakki0925224.yabusame

import android.content.Context
import android.speech.tts.*
import android.util.Log
import java.util.Locale

class VoiceGuide(context: Context) : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false
    private var lastSpokeText = ""
    var isVoiceGuideEnabled = true

    init {
        this.textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e("VoiceGuide", "Failed to initialize TextToSpeech")
            return
        }

        val locale = Locale.JAPAN
        when (this.textToSpeech?.setLanguage(locale)) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                Log.e("VoiceGuide", "Language is not available")
            }
            else -> {
                this.isInitialized = true
            }
        }
    }

    fun speak(text: String) {
        if (!this.isInitialized) {
            Log.e("VoiceGuide", "TextToSpeech is not initialized")
            return
        }

        if (!this.isVoiceGuideEnabled || this.isSpeaking || this.lastSpokeText == text)
            return

        this.isSpeaking = true
        this.lastSpokeText = text
        val utteranceId = this.hashCode().toString()
        this.textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        this.textToSpeech?.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            override fun onError(utteranceId: String?) {
                Log.e("VoiceGuide", "Failed to speak text")
            }
        })
    }

    fun speakWithBoundingBoxes(boxes: List<BoundingBox>) {
        val uniqueClasses = boxes.map { it.cls }.toSet()
        val voiceMessages = uniqueClasses
            .map { convertClsToVoiceGuideMessage(it) }
            .filter(String::isNotEmpty)

        for (msg in voiceMessages) {
            this.speak(msg)
        }
    }

    fun destroy() {
        if (this.textToSpeech != null) {
            this.textToSpeech!!.stop()
            this.textToSpeech!!.shutdown()
            this.textToSpeech = null
        }
    }
}