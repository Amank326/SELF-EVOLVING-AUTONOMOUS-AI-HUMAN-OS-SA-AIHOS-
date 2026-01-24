package com.aihos.system.signals.impl.providers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Network connectivity state signal provider.
 *
 * Responsibilities:
 * - Listen to network connectivity changes
 * - Report whether any network (WiFi, mobile, etc.) is available
 * - Ensure callback is properly unregistered
 * - Provide current network state via StateFlow
 *
 * Safety:
 * - Uses NetworkCallback (not broadcast) - more reliable, no intent filter
 * - Callback is unregistered in unregister() to prevent leaks
 * - Thread-safe StateFlow
 *
 * Behavior:
 * - Emits immediately on first registration (queries current network state)
 * - Emits on onAvailable/onLost via NetworkCallback
 * - More reliable than broadcast-based approach
 *
 * Note: Requires ACCESS_NETWORK_STATE permission (already in AndroidManifest)
 */
class NetworkProvider(private val context: Context) {

    private val _value = MutableStateFlow(true) // Default: assume connected
    val value: StateFlow<Boolean> = _value.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var callback: NetworkStateCallback? = null
    private var isRegistered = false

    /**
     * Register network connectivity callback.
     * Safe: Checks if already registered.
     */
    fun register() {
        if (isRegistered || connectivityManager == null) {
            Timber.d("NetworkProvider: Already registered or ConnectivityManager unavailable")
            return
        }

        try {
            // Create network request for any network
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            // Create callback
            callback = NetworkStateCallback { isConnected ->
                _value.tryEmit(isConnected)
            }

            // Register callback
            connectivityManager.registerNetworkCallback(networkRequest, callback!!)
            isRegistered = true

            // Check current network state immediately
            val isCurrentlyConnected = isNetworkConnected()
            _value.tryEmit(isCurrentlyConnected)

            Timber.d("NetworkProvider: Registered successfully, current state: $isCurrentlyConnected")

        } catch (e: Exception) {
            Timber.e(e, "NetworkProvider: Failed to register network callback")
            callback = null
            isRegistered = false
            throw e
        }
    }

    /**
     * Unregister network connectivity callback.
     * Safe: Checks if registered before unregistering.
     * Critical: Prevents callback leaks.
     */
    fun unregister() {
        if (!isRegistered || callback == null || connectivityManager == null) {
            Timber.d("NetworkProvider: Not registered or callback unavailable")
            return
        }

        try {
            connectivityManager.unregisterNetworkCallback(callback!!)
            isRegistered = false
            callback = null
            Timber.d("NetworkProvider: Unregistered successfully")

        } catch (e: Exception) {
            Timber.w(e, "NetworkProvider: Failed to unregister (may not have been registered)")
            isRegistered = false
            callback = null
        }
    }

    /**
     * Check if any network with internet capability is currently connected.
     */
    private fun isNetworkConnected(): Boolean {
        return try {
            val activeNetwork = connectivityManager?.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Timber.w(e, "NetworkProvider: Error checking network status")
            false
        }
    }

    /**
     * Network state callback.
     * Emits on network availability changes.
     */
    private class NetworkStateCallback(
        private val onStateChange: (Boolean) -> Unit
    ) : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Timber.d("NetworkProvider: Network became available")
            onStateChange(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Timber.d("NetworkProvider: Network was lost")
            onStateChange(false)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)

            val hasInternet = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            Timber.v("NetworkProvider: Capabilities changed - hasInternet: $hasInternet")
        }
    }
}
