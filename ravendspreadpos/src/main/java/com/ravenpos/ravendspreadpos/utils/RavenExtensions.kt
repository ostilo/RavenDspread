package com.ravenpos.ravendspreadpos.utils

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import androidx.core.graphics.ColorUtils
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.SimpleLottieValueCallback
import com.ravenpos.ravendspreadpos.model.BluetoothModel
import java.util.ArrayList
import kotlin.math.roundToInt

object RavenExtensions {
    fun View.gone(){
        this.visibility = View.GONE
    }
    fun View.invisible(){
        this.visibility = View.INVISIBLE
    }

    fun generateTransparentColor(color: Int, alpha: Double?): Int {
        val defaultAlpha = 255 // (0 - Invisible / 255 - Max visibility)
        val colorAlpha = alpha?.times(defaultAlpha)?.roundToInt() ?: defaultAlpha
        return ColorUtils.setAlphaComponent(color, colorAlpha)
    }
    fun View.visible(){
        this.visibility = View.VISIBLE
    }

     fun resetAnimationView(mainSpace:LottieAnimationView) {
        //currentAnimationFrame = 0
        mainSpace.addValueCallback(
            KeyPath("**"), LottieProperty.COLOR_FILTER,
            SimpleLottieValueCallback<ColorFilter?> { null }
        )
    }

     fun addAnimationView(mainSpace:LottieAnimationView,color:Int,pathToChange:String) {
        mainSpace.addValueCallback(
            KeyPath(pathToChange, "**"),
            LottieProperty.COLOR_FILTER,
            SimpleLottieValueCallback<ColorFilter?> {
                PorterDuffColorFilter(
                    color,
                    PorterDuff.Mode.SRC_ATOP
                )
            }
        )
    }

    fun sortNonPos( bluetoothModel : ArrayList<BluetoothModel>) :List<BluetoothModel>{
        return  bluetoothModel.filter { it.title.contains("MPOS") }
    }
}