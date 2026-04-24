package com.youravapp.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity(tableName = "scan_history")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val threatName: String,
    val action: String,
    val timestamp: Long
)

@Dao
interface ScanRecordDao {
    @Insert
    suspend fun insert(record: ScanRecord)
}

@Database(entities = [ScanRecord::class], version = 1)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanRecordDao(): ScanRecordDao
}
