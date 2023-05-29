package com.ravenpos.ravendspreadpos.utils

import android.view.View
import com.ravenpos.ravendspreadpos.model.BluetoothModel
import java.util.ArrayList

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

    fun sortNonPos( bluetoothModel : ArrayList<BluetoothModel>) :List<BluetoothModel>{
        return  bluetoothModel.filter { it.title.contains("MPOS") }
    }
}