package com.ravenpos.ravendspreadpos.pos

import com.ravenpos.ravendspreadpos.model.BluetoothModel

interface IBluetooth {
    fun getSelectedDevice(model: BluetoothModel)
}