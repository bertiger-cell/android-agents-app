package com.agents.app.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.agents.app.models.MessageAttachment
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageAttachmentDao {
    @Insert
    suspend fun insertAttachment(attachment: MessageAttachment)

    @Delete
    suspend fun deleteAttachment(attachment: MessageAttachment)

    @Query("SELECT * FROM message_attachments WHERE messageId = :messageId ORDER BY displayName")
    fun getAttachmentsByMessage(messageId: String): Flow<List<MessageAttachment>>

    @Query("SELECT * FROM message_attachments WHERE sessionId = :sessionId ORDER BY displayName")
    suspend fun getAttachmentsBySession(sessionId: String): List<MessageAttachment>
}
