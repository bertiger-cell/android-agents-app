package com.agents.app.db

import androidx.room.*
import com.agents.app.models.Agent
import com.agents.app.models.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY createdAt DESC")
    fun getAllAgents(): Flow<List<Agent>>

    @Query("SELECT * FROM agents WHERE id = :agentId")
    suspend fun getAgentById(agentId: String): Agent?

    @Query("SELECT * FROM agents WHERE isActive = 1")
    fun getActiveAgents(): Flow<List<Agent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: Agent)

    @Update
    suspend fun updateAgent(agent: Agent)

    @Delete
    suspend fun deleteAgent(agent: Agent)

    @Query("DELETE FROM agents WHERE id = :agentId")
    suspend fun deleteAgentById(agentId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE agentId = :agentId ORDER BY timestamp ASC")
    fun getMessagesByAgent(agentId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE agentId = :agentId")
    suspend fun deleteMessagesByAgent(agentId: String)
}
