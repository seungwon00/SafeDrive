package com.safedrive.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

/**
 * Text-to-Speech 헬퍼
 * - 음성 안내 기능
 * - 음성 완료 콜백 지원
 */
class TextToSpeechHelper(private val context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.KOREAN)
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                                result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }
    
    /**
     * 텍스트를 음성으로 읽기
     */
    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    /**
     * 텍스트를 음성으로 읽기 (완료 콜백 포함)
     */
    fun speak(text: String, onComplete: () -> Unit) {
        if (isInitialized) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                
                override fun onDone(utteranceId: String?) {
                    onComplete()
                }
                
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onComplete()
                }
            })
            
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "emergency_alert")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "emergency_alert")
        } else {
            // TTS가 초기화되지 않았으면 즉시 콜백 호출
            onComplete()
        }
    }
    
    /**
     * 음성 정지
     */
    fun stop() {
        tts?.stop()
    }
    
    /**
     * 리소스 해제
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

