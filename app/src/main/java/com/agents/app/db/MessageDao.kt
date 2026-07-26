package com.agents.app.db

import androidx.room.*
import com.agents.app.models.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert
    suspend fun insertMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp")
    fun getMessagesBySession(sessionId: String): Flow<List<Message>>

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: String)
}
