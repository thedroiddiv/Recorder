package com.example.voicenotes.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class NotesRepository(context: Context) {

    private val gson = Gson()
    private val notesFile = File(context.filesDir, "notes.json")
    private val notesType = object : TypeToken<List<Note>>() {}.type
    private val lock = Any()

    fun loadNotes(): List<Note> = synchronized(lock) {
        if (!notesFile.exists()) return@synchronized emptyList()

        runCatching {
            gson.fromJson<List<Note>>(notesFile.readText(), notesType) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun addNote(note: Note): List<Note> = synchronized(lock) {
        val notes = loadNotes().filterNot { it.id == note.id }
        val updated = listOf(note) + notes
        notesFile.writeText(gson.toJson(updated, notesType))
        updated
    }
}
