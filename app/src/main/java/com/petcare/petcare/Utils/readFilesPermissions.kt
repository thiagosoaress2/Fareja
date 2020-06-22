package com.petcare.petcare.Utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object readFilesPermissions {

    fun hasPermissions(activity : Activity): Boolean{
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED
    }

    fun checkPermission (activity : Activity, code: Int){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){

        } else {
            requestPermission(activity, code)
        }

    }

    fun requestPermission(activity: Activity, code: Int){
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), code)
    }

    fun handlePermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, code: Int) {
        when(requestCode){

            code -> {
                if( grantResults[0] == PackageManager.PERMISSION_GRANTED )
                    Log.i("teste","Agree microphone permission")
                else
                    Log.i("teste","Not agree microphone permission")
            }
        }
    }


}
