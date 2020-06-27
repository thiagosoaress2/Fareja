package com.petcare.petcare.Utils

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings.Global.getString
import androidx.appcompat.app.AppCompatActivity
import com.petcare.petcare.R

class mySharedPrefs (val context: Context) {

    private val PREFS_NAME = "SharedFarejador"
    val sharedPref: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setValue(field: String, value: String){

        val editor = sharedPref.edit()
        editor.putString(field, value)
        editor.apply()
    }

    fun getValue(field: String): String {

        return sharedPref.getString(field, "nao").toString()
    }

    fun clearSharedPreference() {

        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.clear()
        editor.apply()
    }

    //remove specific value
    fun removeValue(KEY_NAME: String) {

        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.remove(KEY_NAME)
        editor.apply()
    }

}