package com.example.githubrepository.data.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Defines the contract for monitoring network connectivity.
 */
interface NetworkMonitor {
    /**
     * Provides a Flow that emits the current network connectivity status.
     * @return A Flow<Boolean> where true means online and false means offline.
     */
    fun isOnline(): Flow<Boolean>
}

/**
 * Implementation of NetworkMonitor using the modern ConnectivityManager.NetworkCallback API.
 * Hilt injects the ApplicationContext directly into the constructor.
 */
@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {

    override fun isOnline(): Flow<Boolean> = callbackFlow {
        // Use Application Context to get system services
        val connectivityManager = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // The set of available, validated networks
        val networks = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            private fun updateNetworkStatus() {
                // Send true if at least one validated network exists
                trySend(networks.isNotEmpty())
            }

            override fun onAvailable(network: Network) {
                // Check capabilities when available for a more accurate status
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) {
                    networks.add(network)
                    updateNetworkStatus()
                }
            }

            override fun onLost(network: Network) {
                if (networks.remove(network)) {
                    updateNetworkStatus()
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (validated) {
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

        // Register the callback
        connectivityManager.registerNetworkCallback(request, callback)

        //Emit initial network status
        val initialStatus = connectivityManager.activeNetwork?.let {
            connectivityManager
                .getNetworkCapabilities(it)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
        trySend(initialStatus)


        // The cleanup block: unregister the callback when the collector stops
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}