package com.agents.app.db

import androidx.room.*
import com.agents.app.models.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Insert
    suspend fun insertSession(session: ChatSessionEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_sessions WHERE projectId = :projectId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getSessionsByProject(projectId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE agentId = :agentId AND isArchived = 0 LIMIT 1")
    suspend fun getLatestSessionForAgent(agentId: String): ChatSessionEntity?
}
