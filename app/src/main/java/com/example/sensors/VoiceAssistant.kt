package com.example.sensors

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceAssistant(context: Context) : TextToSpeech.OnInitListener {

  private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
  private var isInitialized = false

  private val _isVoiceEnabled = MutableStateFlow(true)
  val isVoiceEnabled = _isVoiceEnabled.asStateFlow()

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking = _isSpeaking.asStateFlow()

  private val _lastSpokenText = MutableStateFlow("음성 안내가 준비되었습니다.")
  val lastSpokenText = _lastSpokenText.asStateFlow()

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val result = tts?.setLanguage(Locale.KOREAN)
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        tts?.setLanguage(Locale.ENGLISH)
      }
      isInitialized = true
    }
  }

  fun toggleVoice() {
    _isVoiceEnabled.value = !_isVoiceEnabled.value
    if (!_isVoiceEnabled.value) {
      tts?.stop()
      _isSpeaking.value = false
    }
  }

  fun setVoiceEnabled(enabled: Boolean) {
    _isVoiceEnabled.value = enabled
    if (!enabled) {
      tts?.stop()
      _isSpeaking.value = false
    }
  }

  fun speak(text: String, priority: Boolean = false) {
    _lastSpokenText.value = text
    if (!_isVoiceEnabled.value || !isInitialized) return

    val queueMode = if (priority) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
    tts?.speak(text, queueMode, null, "SpaceScanUtteranceId")
    _isSpeaking.value = true
  }

  fun stop() {
    tts?.stop()
    _isSpeaking.value = false
  }

  fun release() {
    tts?.stop()
    tts?.shutdown()
    tts = null
  }
}
