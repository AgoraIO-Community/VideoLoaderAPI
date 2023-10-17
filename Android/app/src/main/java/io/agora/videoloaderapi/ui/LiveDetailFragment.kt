package io.agora.videoloaderapi.ui

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcConnection
import io.agora.videoloaderapi.AnchorState
import io.agora.videoloaderapi.OnPageScrollEventHandler
import io.agora.videoloaderapi.R
import io.agora.videoloaderapi.VideoLoader
import io.agora.videoloaderapi.databinding.ShowLiveDetailFragmentBinding
import io.agora.videoloaderapi.rtc.RtcEngineInstance
import io.agora.videoloaderapi.service.ShowRoomDetailModel
import io.agora.videoloaderapi.service.ShowServiceProtocol

class LiveDetailFragment : Fragment() {
    private val TAG = this.toString()

    companion object {

        private const val EXTRA_ROOM_DETAIL_INFO = "roomDetailInfo"

        fun newInstance(roomDetail: ShowRoomDetailModel, handler: OnPageScrollEventHandler, position: Int) = LiveDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(EXTRA_ROOM_DETAIL_INFO, roomDetail)
            }
            mHandler = handler
            mPosition = position
        }

    }

    val mRoomInfo by lazy { (arguments?.getParcelable(EXTRA_ROOM_DETAIL_INFO) as? ShowRoomDetailModel)!! }
    private lateinit var mHandler: OnPageScrollEventHandler
    private var mPosition: Int = 0
    private val mBinding by lazy {
        ShowLiveDetailFragmentBinding.inflate(LayoutInflater.from(requireContext())
        )
    }
    private val mService by lazy { ShowServiceProtocol.getImplInstance() }
    private val isRoomOwner by lazy { mRoomInfo.ownerId == RtcEngineInstance.localUid().toString() }


    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }
    private val mRtcVideoSwitcher by lazy { VideoLoader.getImplInstance(mRtcEngine) }

    private var isPageLoaded = false
    private val mMainRtcConnection by lazy { RtcConnection(mRoomInfo.roomId, RtcEngineInstance.localUid()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Fragment Lifecycle: onCreateView")
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Fragment Lifecycle: onViewCreated")
        initView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "Fragment Lifecycle: onAttach")
        onMeLinkingListener = (activity as? LiveDetailActivity)
        if (isPageLoaded) {
            startLoadPage()
        }
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "Fragment Lifecycle: onDetach")
    }

    private fun runOnUiThread(run: Runnable) {
        val activity = activity ?: return
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            run.run()
        } else {
            activity.runOnUiThread(run)
        }
    }

    fun startLoadPageSafely(){
        isPageLoaded = true
        activity ?: return
        startLoadPage()
    }

    fun onPageLoaded() {
        //updatePKingMode()
    }

    private fun startLoadPage(){
        Log.d(TAG, "Fragment PageLoad start load, roomId=${mRoomInfo.roomId}")
        isPageLoaded = true

        if (mRoomInfo.isRobotRoom()) {
            joinRtcChannel {}
            //initServiceWithJoinRoom()
        }
    }

    fun stopLoadPage(isScrolling: Boolean){
        Log.d(TAG, "Fragment PageLoad stop load, roomId=${mRoomInfo.roomId}")
        isPageLoaded = false
        destroy(isScrolling) // 切页或activity销毁
    }

    private fun destroy(isScrolling: Boolean): Boolean {
        mBinding.root.postDelayed({
            mBinding.videoLinkingLayout.videoContainer.removeAllViews()
        }, 200)
        return destroyRtcEngine(isScrolling)
    }

    private fun onBackPressed() {
        activity?.finish()
    }

    //================== UI Operation ===============

    private fun initView() {
        activity?.let {
            mRtcVideoSwitcher.renderVideo(
                VideoLoader.AnchorInfo(
                    mRoomInfo.roomId,
                    mRoomInfo.ownerId.toInt(),
                    RtcEngineInstance.generalToken()
                ),
                RtcEngineInstance.localUid(),
                VideoLoader.VideoCanvasContainer(
                    it,
                    mBinding.videoLinkingLayout.videoContainer,
                    mRoomInfo.ownerId.toInt()
                )
            )
        }

        initVideoView()
        initTopLayout()
    }

    fun initVideoView() : VideoLoader.VideoCanvasContainer?{
        activity?.let {
            return VideoLoader.VideoCanvasContainer(
                it,
                mBinding.videoLinkingLayout.videoContainer,
                mRoomInfo.ownerId.toInt()
            )
        }
        return null
    }

    fun initAnchorVideoView(info: VideoLoader.AnchorInfo) : VideoLoader.VideoCanvasContainer? {
        activity?.let {
            return VideoLoader.VideoCanvasContainer(
                it,
                mBinding.videoLinkingLayout.videoContainer,
                mRoomInfo.ownerId.toInt()
            )
        }
        return null
    }

    private fun initTopLayout() {
        val topLayout = mBinding.topLayout
        Glide.with(this)
            .load(mRoomInfo.ownerAvatar)
            .error(R.mipmap.show_default_avatar)
            .into(topLayout.ivOwnerAvatar)
        topLayout.tvRoomName.text = mRoomInfo.roomName
        topLayout.tvRoomId.text = getString(R.string.show_room_id, mRoomInfo.roomId)
        topLayout.ivClose.setOnClickListener { onBackPressed() }
    }

    //================== RTC Operation ===================

    private fun joinRtcChannel(onJoinChannelSuccess: () -> Unit) {
        if (activity is LiveDetailActivity){
            (activity as LiveDetailActivity).toggleSelfVideo(false, callback = {
                // Render host video
                mRtcEngine.addHandlerEx(object : IRtcEngineEventHandler() {}, mMainRtcConnection)
                initVideoView()
                //mRtcEngine.adjustUserPlaybackSignalVolumeEx(mRoomInfo.ownerId.toInt(), 100, mMainRtcConnection)
            })
            (activity as LiveDetailActivity).toggleSelfAudio(false, callback = {
              // nothing
            })
        }
    }


    private fun destroyRtcEngine(isScrolling: Boolean): Boolean {
        if (!isRoomOwner) return true;
        mRtcEngine.stopPreview()
        mRtcVideoSwitcher.switchAnchorState(if (isScrolling) AnchorState.PRE_JOINED else AnchorState.IDLE,
            VideoLoader.AnchorInfo(
                mRoomInfo.roomId,
                mRoomInfo.ownerId.toInt(),
                RtcEngineInstance.generalToken()
            ), RtcEngineInstance.localUid(), context)
        return true
    }

    private var onMeLinkingListener: OnMeLinkingListener? = null

    interface OnMeLinkingListener {
        fun onMeLinking(isLinking: Boolean)
    }
}