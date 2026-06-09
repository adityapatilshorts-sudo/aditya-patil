package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LiveLocationEntity
import com.example.data.TrackingRepository
import com.example.data.TripHistoryEntity
import com.example.data.UserProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TrackingRepository

    val currentProfile: StateFlow<UserProfileEntity?>
    val allTrips: StateFlow<List<TripHistoryEntity>>
    val liveLocation: StateFlow<LiveLocationEntity?>
    val schoolRoute: List<com.example.data.RoutePoint>

    // Manual view overrides for split-screen testing or role switching simulation
    private val _simulatedScreenRole = MutableStateFlow<String?>(null)
    val simulatedScreenRole = _simulatedScreenRole.asStateFlow()

    // Google Sign-In Dialog UI Simulator state
    private val _isGoogleAccountChooserOpen = MutableStateFlow(false)
    val isGoogleAccountChooserOpen = _isGoogleAccountChooserOpen.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TrackingRepository(database.trackingDao())
        schoolRoute = repository.schoolRoute

        currentProfile = repository.currentProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allTrips = repository.allTrips.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        liveLocation = repository.liveLocation.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    // Google Login Sim
    fun loginWithGoogle(email: String, name: String, photoUrl: String?) {
        viewModelScope.launch {
            repository.loginWithGoogle(email, name, photoUrl)
            _isGoogleAccountChooserOpen.value = false
        }
    }

    fun openGoogleChooser() {
        _isGoogleAccountChooserOpen.value = true
    }

    fun closeGoogleChooser() {
        _isGoogleAccountChooserOpen.value = false
    }

    // Select Role
    fun selectRole(role: String) {
        viewModelScope.launch {
            repository.saveRole(role)
            _simulatedScreenRole.value = role
        }
    }

    // Update Driver Info
    fun updateDriverInfo(name: String, mobile: String, vehicleNo: String, vehicleType: String, photoUri: String?) {
        viewModelScope.launch {
            repository.updateDriverProfile(name, mobile, vehicleNo, vehicleType, photoUri)
        }
    }

    // Toggle active simulation view
    fun overrideRoleForSimulation(role: String?) {
        _simulatedScreenRole.value = role
    }

    // Driver Start Trip
    fun startTrip() {
        viewModelScope.launch {
            repository.startTrip()
        }
    }

    // Driver Stop Trip
    fun stopTrip() {
        viewModelScope.launch {
            repository.stopTrip()
        }
    }

    // Log Out
    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _simulatedScreenRole.value = null
        }
    }
}
