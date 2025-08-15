package com.jejecomms.realtimechatfeature.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A singleton object to monitor network connectivity status.
 *
 * It uses the modern NetworkCallback API and provides the status via a Flow.
 */
object NetworkMonitor {

    private lateinit var applicationContext: Context
    private val isInitialized = AtomicBoolean(false)

    /**
     * Initializes the NetworkMonitor with the application context.
     * Must be called before any other methods are used.
     *
     * @param context The application context.
     */
    fun init(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            applicationContext = context.applicationContext
        }
    }

    /**
     * Provides a Flow that emits the current network connectivity status.
     * The Flow will emit a new value whenever the network state changes.
     *
     * @return A Flow<Boolean> where true means online and false means offline.
     */
    fun isOnline(): Flow<Boolean> = callbackFlow {
        if (!isInitialized.get()) {
            throw IllegalStateException("NetworkMonitor must be " +
                    "initialized via init(context) before use.")
        }

        val connectivityManager = applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // The callback logic for modern Android versions
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

            override fun onCapabilitiesChanged(network: Network
                                               ,networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities
                    .hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && networkCapabilities
                        .hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    networks.add(network)
                } else {
                    networks.remove(network)
                }
                updateNetworkStatus()
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // **Crucial for fixing the issue**: Emit initial network status
        val initialStatus = connectivityManager.activeNetwork?.let {
            connectivityManager
                .getNetworkCapabilities(it)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
        trySend(initialStatus)

        // Ensure the Flow is closed and the callback is unregistered.
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}