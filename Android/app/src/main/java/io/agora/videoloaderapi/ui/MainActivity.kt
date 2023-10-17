package io.agora.videoloaderapi.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
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
    }
}