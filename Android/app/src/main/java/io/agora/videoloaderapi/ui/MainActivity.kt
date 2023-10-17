package io.agora.videoloaderapi.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import io.agora.videoloaderapi.AGSlicingType
import io.agora.videoloaderapi.AgoraApplication
import io.agora.videoloaderapi.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val mViewBinding by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mViewBinding.root)

        mViewBinding.btEnter.setOnClickListener {
            AgoraApplication.the()?.let { application ->
                val intent = Intent()
                intent.setClass(application, RoomListActivity::class.java)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "startActivity failed: $e")
                }
            }
        }

        // 是否开启prejoin模式
        mViewBinding.cbSwitch.isChecked = AgoraApplication.the()?.needPreJoin == true
        mViewBinding.cbSwitch.setOnCheckedChangeListener { _, isChecked ->
            AgoraApplication.the()?.let {
                it.needPreJoin = isChecked
            }
        }

        // 选择视频出图模式
        when (AgoraApplication.the()?.sliceMode) {
            AGSlicingType.VISIABLE -> {
                mViewBinding.spSliceMode.setSelection(0)
            }
            AGSlicingType.END_DRAG -> {
                mViewBinding.spSliceMode.setSelection(1)
            }
            AGSlicingType.END_SCROLL -> {
                mViewBinding.spSliceMode.setSelection(2)
            }
            AGSlicingType.NEVER -> {
                mViewBinding.spSliceMode.setSelection(3)
            }
            else -> {}
        }
        mViewBinding.spSliceMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                when (p2) {
                    0 -> {
                        // hidden
                        AgoraApplication.the()?.sliceMode = AGSlicingType.VISIABLE
                    }
                    1 -> {
                        // fit
                        AgoraApplication.the()?.sliceMode = AGSlicingType.END_DRAG
                    }
                    2 -> {
                        // fit
                        AgoraApplication.the()?.sliceMode = AGSlicingType.END_SCROLL
                    }
                    3 -> {
                        // fit
                        AgoraApplication.the()?.sliceMode = AGSlicingType.NEVER
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }
    }
}