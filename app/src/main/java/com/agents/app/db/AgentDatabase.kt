package com.agents.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agents.app.models.Agent
import com.agents.app.models.ChatSessionEntity
import com.agents.app.models.Message
import com.agents.app.models.ProjectEntity

@Database(
    entities = [Agent::class, Message::class, ProjectEntity::class, ChatSessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao
    abstract fun messageDao(): MessageDao
    abstract fun projectDao(): ProjectDao
    abstract fun chatSessionDao(): ChatSessionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Greenfield: alte Tabellen loeschen, neue werden automatisch erstellt
                db.execSQL("DROP TABLE IF EXISTS messages")
                db.execSQL("DROP TABLE IF EXISTS agents")
            }
        }

        private var database: AgentDatabase? = null

        fun getDatabase(context: Context): AgentDatabase {
            return database ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AgentDatabase::class.java,
                    "agents_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { database = it }
            }
        }
    }
}
