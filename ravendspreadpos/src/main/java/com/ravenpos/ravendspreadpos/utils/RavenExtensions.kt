package com.ravenpos.ravendspreadpos.utils

import android.view.View

object RavenExtensions {
    fun View.gone(){
        this.visibility = View.GONE
    }
    fun View.invisible(){
        this.visibility = View.INVISIBLE
    }

    fun View.visible(){
        this.visibility = View.VISIBLE
    }
}