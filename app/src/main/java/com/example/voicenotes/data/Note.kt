package com.example.voicenotes.data

data class Note(
    val id: String,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long
)
