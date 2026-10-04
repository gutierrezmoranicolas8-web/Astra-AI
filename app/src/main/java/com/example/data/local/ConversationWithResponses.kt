package com.example.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ConversationWithResponses(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val responses: List<ModelResponseEntity>
)
