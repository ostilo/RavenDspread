package com.ravenpos.ravendspreadpos.pos

import androidx.annotation.Keep
import java.io.ByteArrayOutputStream
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
@Keep
class KSNUtilities {

    var latestKsn: String = "";
    var latestIpek: String = "";

    //= "9F8011E7E71E483B",  = "0000000006DDDDE01500"
        fun getWorkingKey(IPEK: String  , KSN: String ): String {
        var initialIPEK: String = IPEK
        println("The expected value of the initial IPEK $initialIPEK")
        val ksn = KSN.padStart(20, '0')
        println("The expected value of the ksn $ksn")
        var sessionkey = ""
        //Get ksn with a zero counter by ANDing it with 0000FFFFFFFFFFE00000

        val newKSN = XORorANDorORfunction(ksn, "0000FFFFFFFFFFE00000", "&")
        println("The expected value of the new KSN is $newKSN")
        val counterKSN = ksn.substring(ksn.length - 5).padStart(16, '0')
        println("The expected value of the counter KSN is $counterKSN")
        //get the number of binary associated with the counterKSN number
        var newKSNtoleft16 = newKSN.substring(newKSN.length - 16)
        println("The expected value of the new KSN to left 16 $newKSNtoleft16")
        val counterKSNbin = Integer.toBinaryString(counterKSN.toInt())
        println("The expected value of the counter KSN Bin $counterKSNbin")
        var binarycount = counterKSNbin
        for (i in 0 until counterKSNbin.length) {
            val len: Int = binarycount.length
            var result = ""
            if (binarycount.substring(0, 1) == "1") {
                result = "1".padEnd(len, '0')
                println("The expected value of the result is $result")
                binarycount = binarycount.substring(1)
                println("The value of the new binary count is $binarycount")
            } else {
                binarycount = binarycount.substring(1)
                println("The value of the new binary count is $binarycount")
                continue
            }
            val counterKSN2 = Integer.toHexString(Integer.parseInt(result, 2)).toUpperCase().padStart(16, '0')
            println("The expected value of the counter ksn 2 is $counterKSN2")
            val newKSN2 = XORorANDorORfunction(newKSNtoleft16, counterKSN2, "|")
            println("The expected value of the new ksn 2 is $newKSN2")
            sessionkey = BlackBoxLogic(newKSN2, initialIPEK)
            //Call Blackbox here
            println("The expected value of the session key here is $sessionkey")
            newKSNtoleft16 = newKSN2
            initialIPEK = sessionkey
        }
        var checkWorkingKey  = XORorANDorORfunction(
            sessionkey, "00000000000000FF00000000000000FF", "^"
        )
        println("**********The value of the working key is $checkWorkingKey")
        return XORorANDorORfunction(sessionkey, "00000000000000FF00000000000000FF", "^")
    }


    fun BlackBoxLogic(ksn: String, iPek: String): String {
        if (iPek.length < 32) {
            println("The expected value IPEK $iPek and IKSN is $ksn")
            latestIpek = iPek ;
            latestKsn(ksn)
            val msg = XORorANDorORfunction(iPek, ksn, "^")
            println("The expected value of the msg is $msg")
            val desreslt = desEncrypt(msg, iPek)
            println("The expected value of the desresult is $desreslt")
            val rsesskey = XORorANDorORfunction(desreslt, iPek, "^")
            println("The expected value of the session key during BBL is $rsesskey")
            return rsesskey
        }
        val current_sk = iPek
        val ksn_mod = ksn
        val leftIpek = XORorANDorORfunction(current_sk, "FFFFFFFFFFFFFFFF0000000000000000", "&").substring(16)
        val rightIpek = XORorANDorORfunction(current_sk, "0000000000000000FFFFFFFFFFFFFFFF", "&").substring(16)
        val message = XORorANDorORfunction(rightIpek, ksn_mod, "^")
        val desresult = desEncrypt(message, leftIpek)
        val rightSessionKey = XORorANDorORfunction(desresult, rightIpek, "^")
        val resultCurrent_sk = XORorANDorORfunction(current_sk, "C0C0C0C000000000C0C0C0C000000000", "^")
        val leftIpek2 = XORorANDorORfunction(resultCurrent_sk, "FFFFFFFFFFFFFFFF0000000000000000", "&").substring(0, 16)
        val rightIpek2 = XORorANDorORfunction(resultCurrent_sk, "0000000000000000FFFFFFFFFFFFFFFF", "&").substring(16)
        val message2 = XORorANDorORfunction(rightIpek2, ksn_mod, "^")
        val desresult2 = desEncrypt(message2, leftIpek2)
        val leftSessionKey = XORorANDorORfunction(desresult2, rightIpek2, "^")
        return leftSessionKey + rightSessionKey
    }

    fun latestKsn(ksn: String) {
        latestKsn = ksn;
    }

    fun hexStringToByteArray(key: String): ByteArray {
        var result: ByteArray = ByteArray(0)
        for (i in 0 until key.length step 2) {
            result += Integer.parseInt(key.substring(i, (i + 2)), 16).toByte()
        }
        return result
    }

    fun byteArrayToHexString(key: ByteArray): String {
        var st = ""
        for (b in key) {
            st += String.format("%02X", b)
        }
        return st
    }

     fun desEncrypt(desData: String, key: String): String {
        val keyData = hexStringToByteArray(key)
        val bout = ByteArrayOutputStream()
        try {
            val keySpec: KeySpec = DESKeySpec(keyData)
            val key: SecretKey = SecretKeyFactory.getInstance("DES").generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val rr = cipher.doFinal(hexStringToByteArray(desData))
            bout.write(cipher.doFinal(hexStringToByteArray(desData)))
        } catch (e: Exception) {
            print("Exception DES Encryption.. " + e.printStackTrace())
        }
        return byteArrayToHexString(bout.toByteArray()).substring(0, 16)
    }


    fun encryptPinBlock(pan: String, pin: String): String {
        val pan = pan.substring(pan.length - 13).take(12).padStart(16, '0')
        println("The expected value of the encrypted pan is $pan")
        val pin = '0' + pin.length.toString(16) + pin.padEnd(16, 'F')
        println("The expected value of the clear pin is $pin")
        return XORorANDorORfunction(pan, pin, "^")
        //the clear pinblock is returned here
    }

    private fun XORorANDorORfunction(valueA: String, valueB: String, symbol: String = "|"): String {
        val a = valueA.toCharArray();
        val b = valueB.toCharArray()
        var result = ""
        for (i in 0 until a.lastIndex + 1) {
            if (symbol === "|") {
                result += (Integer.parseInt(a[i].toString(), 16).or(Integer.parseInt(b[i].toString(), 16)).toString(16).toUpperCase())
            } else if (symbol === "^") {
                result += (Integer.parseInt(a[i].toString(), 16).xor
                    (Integer.parseInt(b[i].toString(), 16)).toString(16).toUpperCase())
            } else {
                result += (Integer.parseInt(a[i].toString(), 16).and
                    (Integer.parseInt(b[i].toString(), 16))).toString(16).toUpperCase()
            }
        }
        return result
    }

    fun DesEncryptDukpt(workingKey: String, pan: String, clearPin: String): String {
        val pinBlock =
            //"04319DCBB86B7B6E"
           XORorANDorORfunction(workingKey, encryptPinBlock(pan, clearPin), "^")
        val keyData = hexStringToByteArray(workingKey)
        val bout = ByteArrayOutputStream()
        try {
            val keySpec: KeySpec = DESKeySpec(keyData)
            val key: SecretKey = SecretKeyFactory.getInstance("DES").generateSecret(keySpec)
            val cipher: Cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            bout.write(cipher.doFinal(hexStringToByteArray(pinBlock)))
            //DES  Encryption
        } catch (e: Exception) {
            println("Exception .. " + e.message)
        }
        return XORorANDorORfunction(
            workingKey, byteArrayToHexString(bout.toByteArray()).substring(
                0,
                16
            ), "^")
    }


}