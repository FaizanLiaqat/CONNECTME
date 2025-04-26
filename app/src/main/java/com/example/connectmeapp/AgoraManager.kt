package com.example.connectmeapp

import android.content.Context
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine

object AgoraManager {

    private var rtcEngine: RtcEngine? = null

    /**
     * Initialize the Agora engine with context, appId, and event handler
     */
    fun initialize(context: Context, appId: String, handler: IRtcEngineEventHandler) {
        if (rtcEngine == null) {
            rtcEngine = RtcEngine.create(context.applicationContext, appId, handler)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        }
    }

    /**
     * Get the current RtcEngine instance
     */
    fun getEngine(): RtcEngine? = rtcEngine

    /**
     * Destroy the engine when you're done
     */
    fun destroy() {
        RtcEngine.destroy()
        rtcEngine = null
    }
}
