package com.inchat.localmedia

import java.io.File

object MediaValidation {
    fun verify(file: File) {
        val head = ByteArray(12)
        val count = file.inputStream().use { it.read(head) }
        val valid = when (file.extension) {
            "mp4" -> count >= 12 && String(head, 4, 4, Charsets.US_ASCII) == "ftyp"
            "mp3" -> count >= 3 && (String(head, 0, 3, Charsets.US_ASCII) == "ID3" ||
                ((head[0].toInt() and 255) == 255 && (head[1].toInt() and 224) == 224))
            else -> false
        }
        check(valid) { "Die heruntergeladene Datei ist keine gültige ${file.extension.uppercase()}-Datei. Möglicherweise wurde eine Fehlerseite statt Medien geliefert." }
    }
}
