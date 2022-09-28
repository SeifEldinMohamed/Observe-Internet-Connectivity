package com.seif.observeinternetwithconnectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkConnectivityObserver(
    private val context: Context
): ConnectivityObserver {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    override fun observe(): Flow<ConnectivityObserver.Status> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch {
                        send(ConnectivityObserver.Status.Available)
                    }
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    launch {
                        send(ConnectivityObserver.Status.Losing)
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    launch {
                        send(ConnectivityObserver.Status.Lost)
                    }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    launch {
                        send(ConnectivityObserver.Status.UnAvailable)
                    }
                }
            }
            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose { // will suspend this callback flow until the current coroutine scope it was launched in is cancelled
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()// will make sure that if we have two emissions of the same type that come one after another then this won't trigger
    }
}
// the better way to do this is in the viewModel (or repository) to do something with that connectivity state ( show some specific sections of your app or don't show them)
// if we did this in viewModel then we will need viewModelFactory on daggerHilt to inject this instance with context

/**
 * The observe() function registers a new network callback every time we try to observe it and returns a cold flow. We can prevent this by converting the cold flow into a hot flow using the `shareIn` or `stateIn` operators. We can add a property to the `class NetworkConnectivityObserver` like this:

val status: Flow<Status> = observe().stateIn(scope, WhileSubscribed(), Status.Unavailable) // Injected scope
Or creating a `class NetworkStatusRepository(CoroutineScope, ConnectivityObserver)` which creates a property like above will be better.
 * **/