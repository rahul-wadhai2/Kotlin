package com.jejecomms.realtimechatfeature.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Utility object to monitor network connectivity using a Flow.
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
     * Provides a Flow that emits the current network connectivity status.
     * The Flow will emit a new value whenever the network state changes.
     *
     * @return A Flow<Boolean> where true means online and false means offline.
     */
    fun isOnline(): Flow<Boolean> = callbackFlow {
        val connectivityManager = applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()

            private fun updateNetworkStatus() {
                trySend(networks.isNotEmpty())
            }

            override fun onAvailable(network: Network) {
                networks.add(network)
                updateNetworkStatus()
            }

            override fun onLost(network: Network) {
                networks.remove(network)
                updateNetworkStatus()
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Ensure the Flow is closed and the callback is unregistered when the consumer is gone.
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}