package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "digitized_papers")
data class DigitizedPaper(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val needsVerificationText: String, // Stringified JSON or bulleted text
    val extractedDiagramsText: String, // Stringified JSON or bulleted text
    val transcribedText: String,       // Full exact transcribed LaTeX text
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val modelUsed: String = "gemini-3.5-flash",
    val isVerified: Boolean = false
) : Serializable
