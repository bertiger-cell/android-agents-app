package com.agents.app.db

import androidx.room.*
import com.agents.app.models.Agent
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Insert
    suspend fun insertAgent(agent: Agent)

    @Update
    suspend fun updateAgent(agent: Agent)

    @Delete
    suspend fun deleteAgent(agent: Agent)

    @Query("DELETE FROM agents WHERE id = :agentId")
    suspend fun deleteAgentById(agentId: String)
 
    @Query("SELECT * FROM agents WHERE projectId = :projectId ORDER BY name")
    fun getAgentsByProject(projectId: String): Flow<List<Agent>>

    @Query("SELECT * FROM agents WHERE id = :agentId")
    suspend fun getAgentById(agentId: String): Agent?
}
