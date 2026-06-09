package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackingRepository(private val trackingDao: TrackingDao) {

    val currentProfile: Flow<UserProfileEntity?> = trackingDao.getUserProfileFlow()
    val allTrips: Flow<List<TripHistoryEntity>> = trackingDao.getAllTripsFlow()
    val liveLocation: Flow<LiveLocationEntity?> = trackingDao.getLiveLocationFlow()

    // Active trip simulation job
    private var simulationJob: Job? = null
    private val appScope = CoroutineScope(Dispatchers.Default)

    // Predefined Route around an elite school area
    val schoolRoute = listOf(
        RoutePoint("Pinecrest Estates", 37.7710, -122.4220, true),
        RoutePoint("Oakwood Corners", 37.7725, -122.4208, false),
        RoutePoint("Wellington Suburbs (Stop 1)", 37.7740, -122.4195, true),
        RoutePoint("Broadway Intersection", 37.7752, -122.4180, false),
        RoutePoint("Greenway Crossing", 37.7765, -122.4168, false),
        RoutePoint("St. Mary's Private Academy (Destination)", 37.7780, -122.4150, true),
        RoutePoint("Beacon Library Square (Stop 2)", 37.7760, -122.4140, true),
        RoutePoint("Lakeside Parkway", 37.7745, -122.4155, false),
        RoutePoint("Valley Heights Avenue", 37.7728, -122.4172, false),
        RoutePoint("Transit Central Depot", 37.7715, -122.4190, true)
    )

    // Simulate Google Sign-In
    suspend fun loginWithGoogle(email: String, name: String, photoUrl: String?) = withContext(Dispatchers.IO) {
        val existing = trackingDao.getUserProfileDirect()
        val profile = UserProfileEntity(
            email = email,
            fullName = name,
            mobileNumber = existing?.mobileNumber ?: "",
            vehicleNumber = existing?.vehicleNumber ?: "",
            vehicleType = existing?.vehicleType ?: "Bus",
            profilePhotoUri = photoUrl ?: existing?.profilePhotoUri,
            selectedRole = existing?.selectedRole, // preserve role selection if any
            isLoggedIn = true
        )
        trackingDao.insertProfile(profile)
    }

    // Save Selected Role
    suspend fun saveRole(role: String) = withContext(Dispatchers.IO) {
        val profile = trackingDao.getUserProfileDirect()
        if (profile != null) {
            trackingDao.insertProfile(profile.copy(selectedRole = role))
        }
    }

    // Update Driver Profile
    suspend fun updateDriverProfile(
        name: String,
        mobile: String,
        vehicleNo: String,
        vehicleType: String,
        photoUri: String?
    ) = withContext(Dispatchers.IO) {
        val profile = trackingDao.getUserProfileDirect()
        if (profile != null) {
            trackingDao.insertProfile(
                profile.copy(
                    fullName = name,
                    mobileNumber = mobile,
                    vehicleNumber = vehicleNo,
                    vehicleType = vehicleType,
                    profilePhotoUri = photoUri
                )
            )
        }
    }

    // Start Tracker Trip
    suspend fun startTrip() = withContext(Dispatchers.IO) {
        val profile = trackingDao.getUserProfileDirect() ?: return@withContext
        val driverName = profile.fullName.ifEmpty { "Elite Driver" }
        val vehicleNo = profile.vehicleNumber.ifEmpty { "SCH-2026-BUS" }
        val vType = profile.vehicleType

        // Insert initial location at first index
        val initialPoint = schoolRoute.first()
        val location = LiveLocationEntity(
            driverName = driverName,
            vehicleNumber = vehicleNo,
            vehicleType = vType,
            latitude = initialPoint.latitude,
            longitude = initialPoint.longitude,
            isActive = true,
            lastUpdated = System.currentTimeMillis()
        )
        trackingDao.insertLiveLocation(location)

        // Reset and launch active background GPS simulation
        simulationJob?.cancel()
        simulationJob = appScope.launch {
            var index = 0
            val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val startTimeVal = timeFormat.format(Date())
            val dateVal = dateFormat.format(Date())
            val startTimeMs = System.currentTimeMillis()

            while (true) {
                delay(2000) // update location every 2 seconds
                index = (index + 1) % schoolRoute.size
                val currentPoint = schoolRoute[index]

                val updatedLocation = LiveLocationEntity(
                    driverName = driverName,
                    vehicleNumber = vehicleNo,
                    vehicleType = vType,
                    latitude = currentPoint.latitude,
                    longitude = currentPoint.longitude,
                    isActive = true,
                    lastUpdated = System.currentTimeMillis()
                )
                trackingDao.insertLiveLocation(updatedLocation)
            }
        }
    }

    // Stop Tracker Trip
    suspend fun stopTrip() = withContext(Dispatchers.IO) {
        simulationJob?.cancel()
        simulationJob = null

        val liveLoc = trackingDao.getLiveLocationDirect()
        val profile = trackingDao.getUserProfileDirect()

        if (liveLoc != null && liveLoc.isActive) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val dateVal = dateFormat.format(Date())
            val endTimeVal = timeFormat.format(Date())

            // Start time is recorded minus a simulated 12 mins trip, or calculated
            val startTimeMs = liveLoc.lastUpdated - (15 * 60 * 1000) // Simulating 15 minutes of transit
            val startTimeVal = timeFormat.format(Date(startTimeMs))

            val durationStr = "15m"

            val trip = TripHistoryEntity(
                date = dateVal,
                startTime = startTimeVal,
                endTime = endTimeVal,
                duration = durationStr,
                vehicleNumber = liveLoc.vehicleNumber,
                vehicleType = liveLoc.vehicleType
            )
            trackingDao.insertTrip(trip)
        }

        // Set status offline but keep coordinates
        val activeLoc = trackingDao.getLiveLocationDirect()
        if (activeLoc != null) {
            trackingDao.insertLiveLocation(activeLoc.copy(isActive = false, lastUpdated = System.currentTimeMillis()))
        }
    }

    // Sign Out
    suspend fun logout() = withContext(Dispatchers.IO) {
        simulationJob?.cancel()
        simulationJob = null
        trackingDao.deleteProfile()
        trackingDao.deleteLiveLocations()
    }
}
