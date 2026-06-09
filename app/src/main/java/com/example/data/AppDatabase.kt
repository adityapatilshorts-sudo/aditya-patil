package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    // Profiling Operations
    @Query("SELECT * FROM user_profiles WHERE id = 'current_user' LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = 'current_user' LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = 'current_user'")
    suspend fun deleteProfile()

    // Trip History Operations
    @Query("SELECT * FROM trip_histories ORDER BY id DESC")
    fun getAllTripsFlow(): Flow<List<TripHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripHistoryEntity)

    // Real-Time Tracker Operations
    @Query("SELECT * FROM live_locations WHERE vehicleId = 'active_vehicle' LIMIT 1")
    fun getLiveLocationFlow(): Flow<LiveLocationEntity?>

    @Query("SELECT * FROM live_locations WHERE vehicleId = 'active_vehicle' LIMIT 1")
    suspend fun getLiveLocationDirect(): LiveLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveLocation(location: LiveLocationEntity)

    @Query("UPDATE live_locations SET isActive = :isActive WHERE vehicleId = 'active_vehicle'")
    suspend fun updateLiveLocationStatus(isActive: Boolean)

    @Query("DELETE FROM live_locations")
    suspend fun deleteLiveLocations()
}

@Database(entities = [UserProfileEntity::class, TripHistoryEntity::class, LiveLocationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackingDao(): TrackingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tracking_database"
                )
                .fallbackToDestructiveMigrationOnDowngrade() // Handle safe version changes
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
