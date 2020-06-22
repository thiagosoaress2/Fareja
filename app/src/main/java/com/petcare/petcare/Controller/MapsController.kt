package com.petcare.petcare.Controller

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import android.widget.Button
import androidx.annotation.NonNull
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task

object MapsController {

    fun requestToOpenGpsLikeWaze (activity: Activity){
        val locationRequest : LocationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(30*1000)
        locationRequest.setFastestInterval(5*1000)
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        builder.setAlwaysShow(true)

        val result: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build())
        result.addOnCompleteListener(object : OnCompleteListener<LocationSettingsResponse?> {
            override fun onComplete(@NonNull task: Task<LocationSettingsResponse?>) {

                try {
                    task.getResult(ApiException::class.java)
                } catch (exception: ApiException) {
                    when (exception.getStatusCode()) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val resolvable: ResolvableApiException =
                                exception as ResolvableApiException
                            resolvable.startResolutionForResult(activity, 100)
                        } catch (e: IntentSender.SendIntentException) {

                        } catch (e: ClassCastException) {
                            Log.d("testeGps", e.message)
                        }
                    }
                }
            }
        })
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var activeNetworkInfo: NetworkInfo? = null
        activeNetworkInfo = cm.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

}