package com.jannikklein47.batterysync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun isInternetAvailable(context: Context): Boolean {
    // Get the ConnectivityManager system service
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Get the currently active network, return false if there isn't one
    val activeNetwork = connectivityManager.activeNetwork ?: return false

    // Get the capabilities of the active network
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

    return when {
        // Checks if the network has internet capability
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                // Checks if the network is validated (actually has a working internet connection, not just connected to a router)
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> true

        else -> false
    }
}