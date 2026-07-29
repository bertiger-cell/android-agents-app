package com.agents.app

import android.app.Application
import com.agents.app.db.AgentDatabase

class AgentsApplication : Application() {
    lateinit var database: AgentDatabase

    override fun onCreate() {
        super.onCreate()
        database = AgentDatabase.getDatabase(this)
    }
}
