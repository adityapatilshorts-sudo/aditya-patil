package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String = "current_user",
    val email: String,
    val fullName: String,
    val mobileNumber: String = "",
    val vehicleNumber: String = "",
    val vehicleType: String = "Bus", // Bus or Van
    val profilePhotoUri: String? = null,
    val selectedRole: String? = null, // DRIVER, PARENT, STUDENT
    val isLoggedIn: Boolean = false
)

@Entity(tableName = "trip_histories")
data class TripHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val vehicleNumber: String,
    val vehicleType: String
)

@Entity(tableName = "live_locations")
data class LiveLocationEntity(
    @PrimaryKey val vehicleId: String = "active_vehicle",
    val driverName: String,
    val vehicleNumber: String,
    val vehicleType: String,
    val latitude: Double,
    val longitude: Double,
    val isActive: Boolean,
    val lastUpdated: Long
)

// Top-level representing geographic landmarks on our bus routing canvas
data class RoutePoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isStop: Boolean
)

