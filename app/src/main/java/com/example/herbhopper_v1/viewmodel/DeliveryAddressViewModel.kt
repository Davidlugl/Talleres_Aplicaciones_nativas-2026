package com.example.herbhopper_v1.viewmodel

import android.app.Application
import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.DeliveryAddress
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel para la gestión de Direcciones de Entrega.
 *
 * Expone operaciones CRUD sobre [DeliveryAddress] y encapsula la lógica de
 * obtención de la ubicación actual mediante [FusedLocationProviderClient].
 * La geocodificación inversa convierte las coordenadas GPS en una dirección
 * de texto legible para pre-rellenar el formulario.
 */
class DeliveryAddressViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).deliveryAddressDao()
    private val fusedLocation: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    /** Estado observable de la ubicación GPS en curso: null = sin resultado todavía. */
    private val _gpsState = MutableStateFlow<GpsState>(GpsState.Idle)
    val gpsState: StateFlow<GpsState> = _gpsState

    // ── CRUD ────────────────────────────────────────────────────────────────────

    /** Devuelve un Flow reactivo con todas las direcciones del usuario. */
    fun observeAddresses(uid: String): Flow<List<DeliveryAddress>> =
        dao.observeAddresses(uid)

    /** Guarda (insert o update) una dirección. Si [address.isDefault] es true, limpia
     *  primero el flag de las demás direcciones del mismo usuario. */
    fun saveAddress(address: DeliveryAddress) = viewModelScope.launch {
        if (address.isDefault) dao.clearDefault(address.uid)
        dao.insertAddress(address)
    }

    /** Elimina una dirección por su id. */
    fun deleteAddress(id: Int) = viewModelScope.launch { dao.deleteAddress(id) }

    /** Establece una dirección como la predeterminada. */
    fun setDefault(uid: String, id: Int) = viewModelScope.launch {
        dao.clearDefault(uid)
        dao.setDefault(id)
    }

    // ── GPS ─────────────────────────────────────────────────────────────────────

    /**
     * Solicita la ubicación actual del dispositivo usando [FusedLocationProviderClient].
     * Requiere que el permiso ACCESS_FINE_LOCATION ya haya sido concedido en la UI.
     * El resultado se emite en [gpsState]: [GpsState.Loading] → [GpsState.Success] o [GpsState.Error].
     */
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        _gpsState.value = GpsState.Loading

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2_000L)
            .setMaxUpdates(1)
            .build()

        fusedLocation.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocation.removeLocationUpdates(this)
                    val loc = result.lastLocation ?: run {
                        _gpsState.value = GpsState.Error("No se pudo obtener la ubicación")
                        return
                    }
                    val address = reverseGeocode(loc.latitude, loc.longitude)
                    _gpsState.value = GpsState.Success(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        addressText = address
                    )
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        _gpsState.value = GpsState.Error("GPS no disponible. Actívalo e inténtalo de nuevo.")
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    /** Resetea el estado GPS a Idle (útil al cerrar el diálogo). */
    fun resetGpsState() { _gpsState.value = GpsState.Idle }

    // ── Geocodificación inversa ──────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(getApplication(), Locale("es", "CO"))
            val results = geocoder.getFromLocation(lat, lon, 1)
            if (!results.isNullOrEmpty()) {
                val addr = results[0]
                buildString {
                    addr.thoroughfare?.let { append(it) }
                    addr.subThoroughfare?.let { append(" $it") }
                    addr.locality?.let { append(", $it") }
                    addr.adminArea?.let { append(", $it") }
                }
            } else {
                "Lat: ${"%.5f".format(lat)}, Lon: ${"%.5f".format(lon)}"
            }
        } catch (e: Exception) {
            "Lat: ${"%.5f".format(lat)}, Lon: ${"%.5f".format(lon)}"
        }
    }
}

/** Estados posibles durante la obtención de la ubicación GPS. */
sealed class GpsState {
    object Idle : GpsState()
    object Loading : GpsState()
    data class Success(val latitude: Double, val longitude: Double, val addressText: String) : GpsState()
    data class Error(val message: String) : GpsState()
}
