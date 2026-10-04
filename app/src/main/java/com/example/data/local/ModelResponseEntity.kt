package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "model_responses",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["modelId"])
    ]
)
data class ModelResponseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val modelId: String,
    val modelVersion: String,
    val responseText: String,
    val latencyMs: Long,
    val tokensEstimate: Int,
    val isWinner: Boolean = false,
    val isStarred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
