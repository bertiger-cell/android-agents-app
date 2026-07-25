package com.agents.app

import android.app.Application
import com.agents.app.db.AgentDatabase

class AgentsApplication : Application() {
    val database by lazy { AgentDatabase.getDatabase(this) }
}
