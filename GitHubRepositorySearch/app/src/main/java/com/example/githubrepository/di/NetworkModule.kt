package com.example.githubrepository.di

import com.example.githubrepository.data.di.ConnectivityNetworkMonitor
import com.example.githubrepository.data.di.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module responsible for providing network-related dependencies.
 *
 * This module binds the concrete implementation of [ConnectivityNetworkMonitor]
 * to the [NetworkMonitor] interface. Using an abstract class with `@Binds`
 * is preferred when the implementation is already available and does not
 * require construction logic.
 *
 * All dependencies here are installed into the [SingletonComponent],
 * meaning they will live as long as the application.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    /**
     * Provides a singleton binding for the network monitoring system.
     *
     * Whenever Hilt needs an instance of [NetworkMonitor], it will supply
     * an instance of [ConnectivityNetworkMonitor], ensuring a single source
     * of truth for network state across the application.
     *
     * @param connectivityNetworkMonitor The concrete implementation injected by Hilt.
     * @return The bound [NetworkMonitor] interface instance.
     */
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        connectivityNetworkMonitor: ConnectivityNetworkMonitor
    ): NetworkMonitor
}