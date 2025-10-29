package com.ezt.priv.shortvideodownloader.receiver.activities

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.ezt.priv.shortvideodownloader.R
import com.ezt.priv.shortvideodownloader.database.viewmodel.CommandTemplateViewModel
import com.ezt.priv.shortvideodownloader.databinding.ActivityShareBinding
import com.ezt.priv.shortvideodownloader.ui.BaseActivity
import com.ezt.priv.shortvideodownloader.ui.BaseActivity2
import com.ezt.priv.shortvideodownloader.util.ThemeUtil
import com.ezt.priv.shortvideodownloader.util.UiUtil

//no use
class TransparentActivity : BaseActivity2<ActivityShareBinding>(ActivityShareBinding::inflate) {

    lateinit var context: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.updateTheme(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
        if (Settings.canDrawOverlays(this)){
            window.addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )

            val params = window.attributes
            params.alpha = 0f
            window.attributes = params
            setContentView(R.layout.activity_share)

        }else{
            window.run {
                setBackgroundDrawable(ColorDrawable(0))
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                } else {
                    setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
                }
            }

            setContentView(R.layout.activity_share)
        }

        when(intent.getStringExtra("action")){
            "NEW_TEMPLATE" -> newTemplate()
        }
    }

    private fun newTemplate(){
        val viewmodel =  ViewModelProvider(this)[CommandTemplateViewModel::class.java]
        UiUtil.showCommandTemplateCreationOrUpdatingSheet(
            null, this, this, viewmodel,
            newTemplate = {
                Toast.makeText(this, R.string.ok,Toast.LENGTH_SHORT).show()
                this.finishAffinity()
            },
            dismissed = {
                this.finishAffinity()
            }
        )
    }
}