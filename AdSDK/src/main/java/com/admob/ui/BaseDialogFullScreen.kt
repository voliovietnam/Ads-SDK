package com.admob.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import com.admob.ads.R
import com.admob.screenHeight
import com.admob.screenWidth

 class BaseDialogFullScreen(
    context: Context,
    private val layoutRes: Int = R.layout.dialog_loading_inter,
) : Dialog(context) {


     fun onViewReady(){

     }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(layoutRes,null)

        setContentView(layoutRes)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        onViewReady()
    }

    final override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        onViewReady()
    }

    override fun show() {
        super.show()
//        val width = screenWidth * getWidthPercent()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawableResource(android.R.color.white)
//        window?.setGravity(gravity)
    }
}