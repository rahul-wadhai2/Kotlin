package com.jejecomms.realtimechatfeature.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility object to monitor network connectivity.
 */
object NetworkMonitor {

    private lateinit var applicationContext: Context

    /**
     * Initializes the NetworkMonitor with the application context.
     *
     * @param context The application context.
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * Checks if there is an active internet connection.
     *
     * @return True if connected to the internet, false otherwise.
     */
    fun isOnline(): Boolean {
        val connectivityManager = applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}