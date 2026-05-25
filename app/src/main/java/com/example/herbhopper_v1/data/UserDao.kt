package com.example.herbhopper_v1.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    fun observeUserProfile(uid: String): kotlinx.coroutines.flow.Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    suspend fun getUserProfile(uid: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE email = :email LIMIT 1")
    suspend fun getUserProfileByEmail(email: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)
}
