package com.cuentas.app.service

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

class LocationService(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Intenta obtener la ubicación actual del dispositivo.
     * Requiere que el permiso ACCESS_FINE_LOCATION ya haya sido concedido.
     * Retorna null si falla o no hay permiso.
     */
    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): LocationData? =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val address = resolveAddress(location.latitude, location.longitude)
                        cont.resume(
                            LocationData(location.latitude, location.longitude, address)
                        )
                    } else {
                        // Fallback: última ubicación conocida
                        fusedClient.lastLocation.addOnSuccessListener { last ->
                            if (last != null) {
                                val address = resolveAddress(last.latitude, last.longitude)
                                cont.resume(LocationData(last.latitude, last.longitude, address))
                            } else {
                                cont.resume(null)
                            }
                        }.addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener { cont.resume(null) }
        }

    private fun resolveAddress(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("es", "PE"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                buildString {
                    addr.thoroughfare?.let { append(it) }
                    addr.subLocality?.let { if (isNotEmpty()) append(", "); append(it) }
                    addr.locality?.let { if (isNotEmpty()) append(", "); append(it) }
                }
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}
