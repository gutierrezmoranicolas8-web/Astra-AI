package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponses(responses: List<ModelResponseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: ModelResponseEntity): Long

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, createdAt DESC")
    fun getAllConversationsWithResponses(): Flow<List<ConversationWithResponses>>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun getConversationWithResponses(id: Long): Flow<ConversationWithResponses?>

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY createdAt DESC")
    suspend fun getAllConversationsSnapshot(): List<ConversationWithResponses>

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateResponse(response: ModelResponseEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    @Query("UPDATE model_responses SET isStarred = :isStarred WHERE id = :id")
    suspend fun setResponseStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :id")
    suspend fun setConversationPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE model_responses SET isWinner = (id = :winningResponseId) WHERE conversationId = :conversationId")
    suspend fun setWinnerForConversation(conversationId: Long, winningResponseId: Long)

    @Query("SELECT COUNT(*) FROM conversations")
    fun getConversationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM model_responses")
    fun getResponseCount(): Flow<Int>
}
