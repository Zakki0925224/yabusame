package io.github.zakki0925224.yabusame

import android.content.Context
import android.speech.tts.*
import android.util.Log
import java.util.Locale

class VoiceGuide(context: Context) : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var onInitListener: (() -> Unit)? = null

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
                this.onInitListener?.invoke()
            }
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!this.isInitialized) {
            Log.e("VoiceGuide", "TextToSpeech is not initialized")
            return
        }

        val utteranceId = this.hashCode().toString()
        this.textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        this.textToSpeech?.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onComplete?.invoke()
            }

            override fun onError(utteranceId: String?) {
                Log.e("VoiceGuide", "Failed to speak text")
            }
        })
    }

    fun setOnInitListener(listener: () -> Unit) {
        this.onInitListener = listener
    }

    fun destroy() {
        if (this.textToSpeech != null) {
            this.textToSpeech!!.stop()
            this.textToSpeech!!.shutdown()
            this.textToSpeech = null
        }
    }
}