package io.agora.videoloaderapi.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import io.agora.videoloaderapi.OnPageScrollEventHandler
import io.agora.videoloaderapi.R
import io.agora.videoloaderapi.VideoLoader
import io.agora.videoloaderapi.databinding.ShowLiveDetailFragmentBinding
import io.agora.videoloaderapi.rtc.RtcEngineInstance
import io.agora.videoloaderapi.service.ShowInteractionStatus
import io.agora.videoloaderapi.service.ShowRoomDetailModel

class LiveViewPagerFragment : Fragment() {
    private val TAG = this.toString()

    companion object {

        private const val EXTRA_ROOM_DETAIL_INFO = "roomDetailInfo"

        fun newInstance(roomDetail: ShowRoomDetailModel, handler: OnPageScrollEventHandler, position: Int) = LiveViewPagerFragment().apply {
            arguments = Bundle().apply {
                putParcelable(EXTRA_ROOM_DETAIL_INFO, roomDetail)
            }
            mHandler = handler
            mPosition = position
        }
    }

    private val mRoomInfo by lazy { (arguments?.getParcelable(EXTRA_ROOM_DETAIL_INFO) as? ShowRoomDetailModel)!! }
    private lateinit var mHandler: OnPageScrollEventHandler
    private var mPosition: Int = 0
    private val mBinding by lazy {
        ShowLiveDetailFragmentBinding.inflate(LayoutInflater.from(requireContext())
        )
    }

    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }
    private val mRtcVideoSwitcher by lazy { VideoLoader.getImplInstance(mRtcEngine) }

    private var isPageLoaded = false
    private var needRender = false

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
        onMeLinkingListener = (activity as? LiveViewPagerActivity)
        if (isPageLoaded) {
            startLoadPage()
        }
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "Fragment Lifecycle: onDetach")
    }

    fun startLoadPageSafely() {
        // TODO 页面创建
    }

    fun onPageLoaded() {
        // TODO 页面加载完成
    }

    private fun startLoadPage(){
        Log.d(TAG, "Fragment PageLoad start load, roomId=${mRoomInfo.roomId}")
        isPageLoaded = true
    }

    fun stopLoadPage(isScrolling: Boolean){
        Log.d(TAG, "Fragment PageLoad stop load, roomId=${mRoomInfo.roomId}")
        isPageLoaded = false
        mBinding.root.postDelayed({
            mBinding.videoLinkingLayout.videoContainer.removeAllViews()
            mBinding.videoPKLayout.iBroadcasterAView.removeAllViews()
            mBinding.videoPKLayout.iBroadcasterBView.removeAllViews()
        }, 200)
    }

    private fun onBackPressed() {
        activity?.finish()
    }

    //================== UI Operation ===============

    private fun initView() {
        if (needRender) {
            needRender = false
            initVideoView()
        }
        initTopLayout()
        refreshViewDetailLayout(mRoomInfo.interactStatus)
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

    private fun initVideoView() {
        activity?.let {
            if ((mRoomInfo.interactStatus == ShowInteractionStatus.pking.value)) {
                mRtcVideoSwitcher.renderVideo(
                    VideoLoader.AnchorInfo(
                        mRoomInfo.roomId,
                        mRoomInfo.ownerId.toInt(),
                        RtcEngineInstance.generalToken()
                    ),
                    RtcEngineInstance.localUid(),
                    VideoLoader.VideoCanvasContainer(
                        viewLifecycleOwner,
                        mBinding.videoPKLayout.iBroadcasterAView,
                        mRoomInfo.ownerId.toInt()
                    )
                )
                mRtcVideoSwitcher.renderVideo(
                    VideoLoader.AnchorInfo(
                        mRoomInfo.interactRoomName,
                        mRoomInfo.interactOwnerId.toInt(),
                        RtcEngineInstance.generalToken()
                    ),
                    RtcEngineInstance.localUid(),
                    VideoLoader.VideoCanvasContainer(
                        viewLifecycleOwner,
                        mBinding.videoPKLayout.iBroadcasterBView,
                        mRoomInfo.interactOwnerId.toInt()
                    )
                )
            } else {
                mRtcVideoSwitcher.renderVideo(
                    VideoLoader.AnchorInfo(
                        mRoomInfo.roomId,
                        mRoomInfo.ownerId.toInt(),
                        RtcEngineInstance.generalToken()
                    ),
                    RtcEngineInstance.localUid(),
                    VideoLoader.VideoCanvasContainer(
                        viewLifecycleOwner,
                        mBinding.videoLinkingLayout.videoContainer,
                        mRoomInfo.ownerId.toInt()
                    )
                )
            }
        }
    }

    private fun refreshViewDetailLayout(status: Int) {
        when (status) {
            ShowInteractionStatus.idle.value -> {
                mBinding.videoPKLayout.root.isVisible = false
                mBinding.videoLinkingLayout.root.isVisible = true
            }
            ShowInteractionStatus.pking.value -> {
                mBinding.topLayout.root.bringToFront()
                mBinding.videoLinkingLayout.root.isVisible = false
                mBinding.videoPKLayout.root.isVisible = true
            }
        }
    }

    fun initAnchorVideoView(info: VideoLoader.AnchorInfo) : VideoLoader.VideoCanvasContainer? {
        // 判断是否此时view还没有创建，即在View创建后第一时间渲染视频
        needRender = activity == null
        activity?.let {
            if ((mRoomInfo.interactStatus == ShowInteractionStatus.pking.value)) {
                if (info.channelId == mRoomInfo.roomId) {
                    return VideoLoader.VideoCanvasContainer(
                        viewLifecycleOwner,
                        mBinding.videoPKLayout.iBroadcasterAView,
                        mRoomInfo.ownerId.toInt()
                    )
                } else if (info.channelId == mRoomInfo.interactRoomName) {
                    return VideoLoader.VideoCanvasContainer(
                        viewLifecycleOwner,
                        mBinding.videoPKLayout.iBroadcasterBView,
                        mRoomInfo.interactOwnerId.toInt()
                    )
                }
            } else {
                return VideoLoader.VideoCanvasContainer(
                    viewLifecycleOwner,
                    mBinding.videoLinkingLayout.videoContainer,
                    mRoomInfo.ownerId.toInt()
                )
            }
        }
        return null
    }

    //================== RTC Operation ===================

    private var onMeLinkingListener: OnMeLinkingListener? = null

    interface OnMeLinkingListener {
        fun onMeLinking(isLinking: Boolean)
    }
}