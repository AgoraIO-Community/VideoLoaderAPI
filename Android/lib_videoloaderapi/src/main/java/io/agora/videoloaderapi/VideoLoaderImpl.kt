package io.agora.videoloaderapi

import android.view.TextureView
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.agora.rtc2.*
import io.agora.rtc2.IRtcEngineEventHandler.VideoRenderingTracingInfo
import io.agora.rtc2.video.VideoCanvas
import org.json.JSONObject
import java.util.*

class VideoLoaderImpl constructor(private val rtcEngine: RtcEngineEx) : VideoLoader {
    private val tag = "VideoLoaderTag"
    private val anchorStateMap = Collections.synchronizedMap(mutableMapOf<RtcConnectionWrap, AnchorState>())
    private val remoteVideoCanvasList = Collections.synchronizedList(mutableListOf<RemoteVideoCanvasWrap>())

    override fun cleanCache() {
        VideoLoader.reportCallScenarioApi("cleanCache", JSONObject())
        anchorStateMap.forEach {
            innerSwitchAnchorState(AnchorState.IDLE, 0, it.key, null, null)
        }
        anchorStateMap.clear()
    }

    override fun preloadAnchor(anchorList: List<VideoLoader.AnchorInfo>, uid: Int) {
        VideoLoader.reportCallScenarioApi("cleanCache", JSONObject().put("anchorList", anchorList).put("uid", uid))
        anchorList.forEach {
            rtcEngine.preloadChannel(it.token, it.channelId, uid)
        }
    }

    override fun switchAnchorState(
        newState: AnchorState,
        anchorInfo: VideoLoader.AnchorInfo,
        localUid: Int,
        mediaOptions: ChannelMediaOptions?
    ) {
        VideoLoader.reportCallScenarioApi("switchAnchorState", JSONObject().put("newState", newState).put("anchorInfo", anchorInfo).put("uid", localUid))
        innerSwitchAnchorState(newState, anchorInfo.anchorUid, RtcConnection(anchorInfo.channelId, localUid), anchorInfo.token, mediaOptions)
    }

    override fun getRoomState(channelId: String, localUid: Int): AnchorState? {
        anchorStateMap.forEach {
            if (it.key.isSameChannel(RtcConnection(channelId, localUid))) {
                return it.value
            }
        }
        return null
    }

    override fun renderVideo(anchorInfo: VideoLoader.AnchorInfo, localUid: Int, container: VideoLoader.VideoCanvasContainer) {
        VideoLoader.reportCallScenarioApi("renderVideo", JSONObject().put("anchorInfo", anchorInfo).put("localUid", localUid).put("container", container))
        remoteVideoCanvasList.firstOrNull {
            it.connection.channelId == anchorInfo.channelId && it.uid == container.uid && it.renderMode == container.renderMode && it.lifecycleOwner == container.lifecycleOwner
        }?.let {
            val videoView = it.view
            val viewIndex = container.container.indexOfChild(videoView)

            if (viewIndex == container.viewIndex) {
                rtcEngine.setupRemoteVideoEx(
                    it,
                    it.connection
                )
                return
            }
        }

        var videoView = container.container.getChildAt(container.viewIndex)
        if (videoView !is TextureView) {
            videoView = TextureView(container.container.context)
            container.container.addView(videoView, container.viewIndex)
        } else {
            container.container.removeViewInLayout(videoView)
            videoView = TextureView(container.container.context)
            container.container.addView(videoView, container.viewIndex)
        }

        val connection = RtcConnection(anchorInfo.channelId, localUid)
        anchorStateMap.forEach {
            if (it.key.isSameChannel(connection)) {
                val connectionWrap = it.key
                val remoteVideoCanvasWrap = RemoteVideoCanvasWrap(
                    connectionWrap,
                    container.lifecycleOwner,
                    videoView,
                    container.renderMode,
                    container.uid
                )
                rtcEngine.setupRemoteVideoEx(
                    remoteVideoCanvasWrap,
                    connectionWrap
                )
                return
            }
        }

        val remoteVideoCanvasWrap = RemoteVideoCanvasWrap(
            connection,
            container.lifecycleOwner,
            videoView,
            container.renderMode,
            container.uid
        )
        rtcEngine.setupRemoteVideoEx(
            remoteVideoCanvasWrap,
            connection
        )
    }

    // ------------------------------- inner private -------------------------------

    /**
     * 切换指定主播的状态
     * @param newState 目标状态
     * @param anchorUid 主播 uid
     * @param connection 对应频道的 RtcConnection
     * @param token 对应频道的 token
     * @param mediaOptions 自定义的 ChannelMediaOptions
     */
    private fun innerSwitchAnchorState(
        newState: AnchorState,
        anchorUid: Int,
        connection: RtcConnection,
        token: String?,
        mediaOptions: ChannelMediaOptions?
    ) {
        VideoLoader.videoLoaderApiLog(tag, "innerSwitchAnchorState, newState: $newState, connection: $connection, anchorStateMap: $anchorStateMap")
        // anchorStateMap 无当前主播记录
        if (anchorStateMap.none {it.key.isSameChannel(connection)}) {
            val rtcConnectionWrap = RtcConnectionWrap(connection)
            when (newState) {
                AnchorState.PRE_JOINED -> {
                    // 加入频道但不收流
                    val options = mediaOptions ?: ChannelMediaOptions().apply {
                        clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                        audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                        autoSubscribeVideo = false
                        autoSubscribeAudio = false
                    }
                    val ret = rtcEngine.joinChannelEx(token, connection, options, object : IRtcEngineEventHandler() {
                        override fun onVideoRenderingTracingResult(
                            uid: Int,
                            currentEvent: Constants.MEDIA_TRACE_EVENT?,
                            tracingInfo: VideoRenderingTracingInfo?
                        ) {
                            super.onVideoRenderingTracingResult(uid, currentEvent, tracingInfo)
                            VideoLoader.videoLoaderApiLog(tag, "onVideoRenderingTracingResult channel: ${connection.channelId} uid: $uid, currentEvent: $currentEvent, tracingInfo: ${printTracingInfo(tracingInfo)}")
                        }
                    })
                    VideoLoader.videoLoaderApiLog(tag, "joinChannel PRE_JOINED, connection:$connection, ret:$ret")
                }
                AnchorState.JOINED -> {
                    // 加入频道且收流
                    val options = mediaOptions ?: ChannelMediaOptions().apply {
                        // 加入频道且收流
                        clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                        audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                        autoSubscribeVideo = true
                        autoSubscribeAudio = true
                    }
                    val ret = rtcEngine.joinChannelEx(token, connection, options, object : IRtcEngineEventHandler() {
                        override fun onVideoRenderingTracingResult(
                            uid: Int,
                            currentEvent: Constants.MEDIA_TRACE_EVENT?,
                            tracingInfo: VideoRenderingTracingInfo?
                        ) {
                            super.onVideoRenderingTracingResult(uid, currentEvent, tracingInfo)
                            VideoLoader.videoLoaderApiLog(tag, "onVideoRenderingTracingResult channel: ${connection.channelId} uid: $uid, currentEvent: $currentEvent, tracingInfo: ${printTracingInfo(tracingInfo)}")
                        }
                    })
                    VideoLoader.videoLoaderApiLog(tag, "joinChannel JOINED, connection:$connection, ret:$ret")
                }
                AnchorState.JOINED_WITHOUT_AUDIO -> {
                    val options = mediaOptions ?: ChannelMediaOptions().apply {
                        clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                        audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                        autoSubscribeVideo = true
                        autoSubscribeAudio = true
                    }
                    val ret = rtcEngine.joinChannelEx(token, connection, options, object : IRtcEngineEventHandler() {
                        override fun onVideoRenderingTracingResult(
                            uid: Int,
                            currentEvent: Constants.MEDIA_TRACE_EVENT?,
                            tracingInfo: VideoRenderingTracingInfo?
                        ) {
                            super.onVideoRenderingTracingResult(uid, currentEvent, tracingInfo)
                            VideoLoader.videoLoaderApiLog(tag, "onVideoRenderingTracingResult channel: ${connection.channelId} uid: $uid, currentEvent: $currentEvent, tracingInfo: ${printTracingInfo(tracingInfo)}")
                        }
                    })
                    // 防止音画不同步， 我们采用先订阅再将播放调为0的方式
                    rtcEngine.adjustUserPlaybackSignalVolumeEx(anchorUid, 0, connection)
                    VideoLoader.videoLoaderApiLog(tag, "joinChannel JOINED_WITHOUT_AUDIO, connection:$connection, ret:$ret")
                }

                else -> {}
            }
            anchorStateMap[rtcConnectionWrap] = newState
            return
        }

        anchorStateMap.forEach {
            if (it.key.isSameChannel(connection)) {
                val oldState = it.value
                if (oldState == newState) {
                    VideoLoader.videoLoaderApiLogWarning(tag, "switchAnchorState is already this state")
                    return
                }
                anchorStateMap[it.key] = newState
                when {
                    oldState == AnchorState.IDLE && newState == AnchorState.PRE_JOINED -> {
                        // 加入频道但不收流
                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = false
                            autoSubscribeAudio = false
                        }
                        val ret = rtcEngine.joinChannelEx(token, connection, options, object : IRtcEngineEventHandler() {
                            override fun onVideoRenderingTracingResult(
                                uid: Int,
                                currentEvent: Constants.MEDIA_TRACE_EVENT?,
                                tracingInfo: VideoRenderingTracingInfo?
                            ) {
                                super.onVideoRenderingTracingResult(uid, currentEvent, tracingInfo)
                                VideoLoader.videoLoaderApiLog(tag, "onVideoRenderingTracingResult channel: ${connection.channelId} uid: $uid, currentEvent: $currentEvent, tracingInfo: ${printTracingInfo(tracingInfo)}")
                            }
                        })
                        VideoLoader.videoLoaderApiLog(tag, "joinChannel PRE_JOINED, connection:$connection, ret:$ret")
                    }
                    (oldState == AnchorState.PRE_JOINED || oldState == AnchorState.JOINED_WITHOUT_AUDIO) && newState == AnchorState.JOINED -> {
                        // 保持在频道内, 收流
                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = true
                            autoSubscribeAudio = true
                        }
                        val ret = rtcEngine.updateChannelMediaOptionsEx(options, connection)
                        VideoLoader.videoLoaderApiLog(tag, "updateChannelMediaOptionsEx, connection:$connection, ret:$ret")
                        rtcEngine.adjustUserPlaybackSignalVolumeEx(anchorUid, 100, connection)
                    }
                    (oldState == AnchorState.JOINED || oldState == AnchorState.JOINED_WITHOUT_AUDIO)  && newState == AnchorState.PRE_JOINED -> {
                        // 保持在频道内，不收流
                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = false
                            autoSubscribeAudio = false
                        }
                        val ret = rtcEngine.updateChannelMediaOptionsEx(options, connection)
                        remoteVideoCanvasList.filter { it.connection.channelId == connection.channelId }.forEach { it.release() }
                        VideoLoader.videoLoaderApiLog(tag, "updateChannelMediaOptionsEx, connection:$connection, ret:$ret")
                    }
                    oldState == AnchorState.IDLE && newState == AnchorState.JOINED -> {
                        // 加入频道，且收流
                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = true
                            autoSubscribeAudio = true
                        }
                        val ret = rtcEngine.joinChannelEx(token, connection, options, object : IRtcEngineEventHandler() {
                            override fun onVideoRenderingTracingResult(
                                uid: Int,
                                currentEvent: Constants.MEDIA_TRACE_EVENT?,
                                tracingInfo: VideoRenderingTracingInfo?
                            ) {
                                super.onVideoRenderingTracingResult(uid, currentEvent, tracingInfo)
                                VideoLoader.videoLoaderApiLog(tag, "onVideoRenderingTracingResult channel: ${connection.channelId} uid: $uid, currentEvent: $currentEvent, tracingInfo: ${printTracingInfo(tracingInfo)}")
                            }
                        })
                        VideoLoader.videoLoaderApiLog(tag, "joinChannelEx JOINED, connection:$connection, ret:$ret")
                    }
                    oldState == AnchorState.IDLE && newState == AnchorState.JOINED_WITHOUT_AUDIO -> {
                        // 加入频道，且收流
                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = true
                            autoSubscribeAudio = true
                        }
                        val ret = rtcEngine.joinChannelEx(token, connection, options, object : IRtcEngineEventHandler() {
                            override fun onVideoRenderingTracingResult(
                                uid: Int,
                                currentEvent: Constants.MEDIA_TRACE_EVENT?,
                                tracingInfo: VideoRenderingTracingInfo?
                            ) {
                                super.onVideoRenderingTracingResult(uid, currentEvent, tracingInfo)
                                VideoLoader.videoLoaderApiLog(tag, "onVideoRenderingTracingResult channel: ${connection.channelId}, uid: $uid, currentEvent: $currentEvent, tracingInfo: ${printTracingInfo(tracingInfo)}")
                            }
                        })
                        VideoLoader.videoLoaderApiLog(tag, "joinChannelEx JOINED_WITHOUT_AUDIO, connection:$connection, ret:$ret")
                        // 防止音画不同步， 我们采用先订阅再将播放调为0的方式
                        rtcEngine.adjustUserPlaybackSignalVolumeEx(anchorUid, 0, connection)
                    }
                    oldState == AnchorState.PRE_JOINED && newState == AnchorState.JOINED_WITHOUT_AUDIO -> {
                        // 保持在频道内, 收流
                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = true
                            autoSubscribeAudio = true
                        }
                        val ret = rtcEngine.updateChannelMediaOptionsEx(options, connection)
                        VideoLoader.videoLoaderApiLog(tag, "updateChannelMediaOptionsEx, connection:$connection, ret:$ret")
                        // 防止音画不同步， 我们采用先订阅再将播放调为0的方式
                        rtcEngine.adjustUserPlaybackSignalVolumeEx(anchorUid, 0, connection)
                    }
                    newState == AnchorState.IDLE -> {
                        // 退出频道
                        leaveRtcChannel(it.key)
                    }
                }
                return
            }
        }
    }

    private fun leaveRtcChannel(connection: RtcConnectionWrap) {
        val ret = rtcEngine.leaveChannelEx(connection)
        VideoLoader.videoLoaderApiLog(
            tag,
            "leaveChannel ret : connection=$connection, code=$ret, message=${RtcEngine.getErrorDescription(ret)}"
        )
        remoteVideoCanvasList.filter { it.connection.channelId == connection.channelId }.forEach { it.release() }
    }

    private fun printTracingInfo(tracingInfo: VideoRenderingTracingInfo?): String {
        val info = tracingInfo ?: return ""
        return "elapsedTime:${info.elapsedTime} start2JoinChannel:${info.start2JoinChannel} join2JoinSuccess:${info.join2JoinSuccess} joinSuccess2RemoteJoined:${info.joinSuccess2RemoteJoined} remoteJoined2SetView:${info.remoteJoined2SetView} remoteJoined2UnmuteVideo:${info.remoteJoined2UnmuteVideo} remoteJoined2PacketReceived:${info.remoteJoined2PacketReceived}"
    }

    inner class RtcConnectionWrap constructor(connection: RtcConnection) :
        RtcConnection(connection.channelId, connection.localUid) {

        fun isSameChannel(connection: RtcConnection?) =
            connection != null && channelId == connection.channelId && localUid == connection.localUid

        override fun toString(): String {
            return "{channelId=$channelId, localUid=$localUid}"
        }
    }

    inner class RemoteVideoCanvasWrap constructor(
        val connection: RtcConnection,
        val lifecycleOwner: LifecycleOwner,
        view: View,
        renderMode: Int,
        uid: Int
    ) : DefaultLifecycleObserver, VideoCanvas(view, renderMode, uid) {

        init {
            VideoLoader.videoLoaderApiLog(tag, "new video canvas $this")
            setupMode = VIEW_SETUP_MODE_ADD
            lifecycleOwner.lifecycle.addObserver(this)
            remoteVideoCanvasList.add(this)
        }

        override fun toString(): String {
            return "connection:$connection, lifecycleOwner:$lifecycleOwner, view:$view, renderMode:$renderMode, remoteUid:$uid"
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            if (lifecycleOwner == owner) {
                release()
            }
        }

        fun release() {
            lifecycleOwner.lifecycle.removeObserver(this)
            setupMode = VIEW_SETUP_MODE_REMOVE
            VideoLoader.videoLoaderApiLog(tag, "release video canvas $this")
            rtcEngine.setupRemoteVideoEx(this, connection)
            remoteVideoCanvasList.remove(this)
        }
    }
}