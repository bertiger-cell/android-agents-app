package com.agents.app.automation

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.agents.app.AgentsApplication
import com.agents.app.MainActivity
import com.agents.app.AgentRepository
import com.agents.app.models.*
import kotlinx.coroutines.*

class AgentForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var repository: AgentRepository? = null
    private var currentJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as AgentsApplication
        repository = AgentRepository(app.database)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val agentId = intent.getStringExtra(EXTRA_AGENT_ID) ?: return START_NOT_STICKY
                startAgent(agentId)
            }
            ACTION_STOP -> {
                stopAgent()
            }
        }
        return START_STICKY
    }

    private fun startAgent(agentId: String) {
        currentJob = serviceScope.launch {
            showNotification("Agent is running...")

            repository?.let { repo ->
                val agent = repo.getAgentById(agentId)
                if (agent != null) {
                    // Execute agent task
                    val result = repo.executeAgentTask(
                        agent = agent,
                        apiKey = "", // TODO: Get from settings
                        baseUrl = "http://localhost:11434"
                    )

                    showNotification(
                        if (result.success) "Agent completed" else "Agent failed: ${result.error}"
                    )
                }
            }

            stopSelf()
        }
    }

    private fun stopAgent() {
        currentJob?.cancel()
        showNotification("Agent stopped")
        stopSelf()
    }

    private fun showNotification(text: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AgentsApplication.CHANNEL_ID)
            .setContentTitle("Android Agents")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.agents.app.START_AGENT"
        const val ACTION_STOP = "com.agents.app.STOP_AGENT"
        const val EXTRA_AGENT_ID = "agent_id"
    }
}
