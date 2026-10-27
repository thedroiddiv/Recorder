package com.example.voicenotes.audio

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.min

object WavAmplitudeReader {

    fun read(file: File, pointCount: Int = 180): List<Int> {
        if (!file.exists() || pointCount <= 0) return emptyList()

        return runCatching {
            RandomAccessFile(file, "r").use { access ->
                val riff = ByteArray(4)
                val wave = ByteArray(4)
                access.readFully(riff)
                access.skipBytes(4)
                access.readFully(wave)
                if (String(riff) != "RIFF" || String(wave) != "WAVE") return@use emptyList()

                var channels = 1
                var bitsPerSample = 16
                var dataOffset = -1L
                var dataSize = 0L
                var offset = 12L

                while (offset + 8 <= access.length()) {
                    access.seek(offset)
                    val idBytes = ByteArray(4)
                    access.readFully(idBytes)
                    val chunkSize = readLittleEndianInt(access).toLong().coerceAtLeast(0L)
                    val chunkDataOffset = offset + 8
                    val id = String(idBytes)

                    when (id) {
                        "fmt " -> {
                            access.seek(chunkDataOffset)
                            access.skipBytes(2)
                            channels = readLittleEndianShort(access).coerceAtLeast(1)
                            access.skipBytes(4)
                            access.skipBytes(4)
                            access.skipBytes(2)
                            bitsPerSample = readLittleEndianShort(access)
                        }

                        "data" -> {
                            dataOffset = chunkDataOffset
                            dataSize = min(chunkSize, access.length() - chunkDataOffset)
                            break
                        }
                    }

                    offset = chunkDataOffset + chunkSize + chunkSize % 2
                }

                if (dataOffset < 0 || dataSize < 2 || bitsPerSample != 16) return@use emptyList()

                val bytesPerFrame = 2 * channels
                val frameCount = (dataSize / bytesPerFrame).toInt()
                if (frameCount <= 0) return@use emptyList()

                val framesPerPoint = (frameCount / pointCount).coerceAtLeast(1)
                val buffer = ByteArray(framesPerPoint * bytesPerFrame)
                val amplitudes = ArrayList<Int>(pointCount)
                var framesRead = 0

                access.seek(dataOffset)
                while (framesRead < frameCount && amplitudes.size < pointCount) {
                    val framesToRead = min(framesPerPoint, frameCount - framesRead)
                    val bytesToRead = framesToRead * bytesPerFrame
                    access.readFully(buffer, 0, bytesToRead)

                    var peak = 0
                    for (frame in 0 until framesToRead) {
                        val index = frame * bytesPerFrame
                        val sample = ((buffer[index + 1].toInt() and 0xFF) shl 8) or
                            (buffer[index].toInt() and 0xFF)
                        val signed = if (sample > Short.MAX_VALUE) sample - 65_536 else sample
                        peak = maxOf(peak, abs(signed))
                    }

                    amplitudes += peak
                    framesRead += framesToRead
                }

                amplitudes
            }
        }.getOrDefault(emptyList())
    }

    private fun readLittleEndianInt(access: RandomAccessFile): Int {
        val b0 = access.read()
        val b1 = access.read()
        val b2 = access.read()
        val b3 = access.read()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun readLittleEndianShort(access: RandomAccessFile): Int {
        val b0 = access.read()
        val b1 = access.read()
        return b0 or (b1 shl 8)
    }
}
