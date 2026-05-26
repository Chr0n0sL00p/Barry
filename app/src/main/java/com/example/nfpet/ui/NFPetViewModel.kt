package com.example.nfpet.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfpet.data.model.PetReport
import com.example.nfpet.data.repository.PetRepository
import com.example.nfpet.location.LocationHelper
import com.example.nfpet.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NFPetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetRepository(application)
    private val locationHelper = LocationHelper(application)
    private val notificationHelper = NotificationHelper(application)

    // Raw reports list from repo
    val reports: StateFlow<List<PetReport>> = repository.reports

    // Current detected or simulated location of the user
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // Current detected or simulated city of the user
    private val _currentCity = MutableStateFlow("Santiago")
    val currentCity: StateFlow<String> = _currentCity.asStateFlow()

    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    init {
        // Initialize mock data relative to default Santiago coordinates
        repository.prepopulateMockData(-33.4489, -70.6693, "Santiago")
        
        // Attempt to auto-detect location on start
        detectLocation()
    }

    /**
     * Attempts to read the GPS location and update the repository mocks nearby
     */
    fun detectLocation() {
        viewModelScope.launch {
            _isDetectingLocation.value = true
            try {
                if (locationHelper.hasLocationPermissions()) {
                    val loc = locationHelper.getCurrentLocation()
                    if (loc != null) {
                        _userLocation.value = loc
                        val city = locationHelper.getCityName(loc.latitude, loc.longitude)
                        _currentCity.value = city
                        
                        // Dynamically update mock data coordinates to center around the user's real city!
                        repository.updateMockLocations(loc.latitude, loc.longitude, city)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isDetectingLocation.value = false
            }
        }
    }

    /**
     * Manually overrides the current city (for simulator testing)
     */
    fun setSimulatedCity(city: String) {
        _currentCity.value = city
    }

    /**
     * Posts a new pet report and triggers notification checks
     */
    fun publishReport(
        name: String,
        description: String,
        photoUri: String?,
        latitude: Double,
        longitude: Double,
        city: String,
        reporterName: String,
        phoneNumber: String,
        status: String
    ) {
        viewModelScope.launch {
            val report = PetReport(
                name = name,
                description = description,
                photoUri = photoUri,
                latitude = latitude,
                longitude = longitude,
                city = city,
                reporterName = reporterName,
                phoneNumber = phoneNumber,
                status = status
            )
            repository.addReport(report)
            
            // Check if the reported pet's city matches the user's current city
            // If so, trigger a notification! (In a real app, this happens via Firebase,
            // but we simulate it locally to demonstrate the immediate push notifications feature)
            if (city.trim().equals(_currentCity.value.trim(), ignoreCase = true) && status == "LOST") {
                notificationHelper.showLostPetNotification(
                    petName = name,
                    cityName = city,
                    petDescription = description
                )
            }
        }
    }

    /**
     * Simulates an incoming lost pet report posted by another user in a specified city
     */
    fun simulateIncomingReport(name: String, description: String, cityName: String, offsetLat: Double, offsetLng: Double) {
        viewModelScope.launch {
            // Determine coordinates: base on user's current coordinates if available, or default
            val baseLat = _userLocation.value?.latitude ?: -33.4489
            val baseLng = _userLocation.value?.longitude ?: -70.6693
            
            val report = PetReport(
                name = name,
                description = description,
                photoUri = "mock_simulated_${name.lowercase()}", // Special simulated photo flag
                latitude = baseLat + offsetLat,
                longitude = baseLng + offsetLng,
                city = cityName,
                reporterName = "Vecino Alerta",
                phoneNumber = "+56999888777",
                status = "LOST",
                timestamp = System.currentTimeMillis()
            )
            
            repository.addReport(report)
            
            // Trigger system notification if it matches the current city
            if (cityName.trim().equals(_currentCity.value.trim(), ignoreCase = true)) {
                notificationHelper.showLostPetNotification(
                    petName = name,
                    cityName = cityName,
                    petDescription = description
                )
            }
        }
    }

    /**
     * Helper to retrieve context-based notification helper for external checks
     */
    fun getNotificationHelper(): NotificationHelper {
        return notificationHelper
    }

    // --- NFC Pet ID Reader Support ---
    
    private val _nfcScannedPet = MutableStateFlow<PetReport?>(null)
    val nfcScannedPet: StateFlow<PetReport?> = _nfcScannedPet.asStateFlow()

    private val _showNfcDialog = MutableStateFlow(false)
    val showNfcDialog: StateFlow<Boolean> = _showNfcDialog.asStateFlow()

    /**
     * Called when a physical or simulated NFC collar tag is scanned.
     * Searches the local database for the pet ID or Name and launches the details modal.
     */
    fun onNfcTagScanned(petId: String) {
        viewModelScope.launch {
            // Find by matching ID or name (case-insensitive) to make testing and real tags extremely simple!
            val matchedPet = repository.reports.value.firstOrNull { 
                it.id.equals(petId, ignoreCase = true) || it.name.equals(petId, ignoreCase = true) 
            }
            
            if (matchedPet != null) {
                _nfcScannedPet.value = matchedPet
            } else {
                // If it's not in the database, create a generic mock read to show the Ndef read works!
                _nfcScannedPet.value = PetReport(
                    id = petId,
                    name = "Mascota Desconocida",
                    description = "Chip NFC detectado (ID: $petId), pero no está registrado en esta base de datos local.",
                    photoUri = null,
                    latitude = _userLocation.value?.latitude ?: -33.4489,
                    longitude = _userLocation.value?.longitude ?: -70.6693,
                    city = _currentCity.value,
                    reporterName = "Servicio NFC",
                    phoneNumber = "+56911112222",
                    status = "LOST"
                )
            }
            _showNfcDialog.value = true
        }
    }

    fun triggerNfcDialog(show: Boolean) {
        _showNfcDialog.value = show
        if (!show) {
            _nfcScannedPet.value = null
        }
    }

    fun closeNfcDialog() {
        _showNfcDialog.value = false
        _nfcScannedPet.value = null
    }
}
