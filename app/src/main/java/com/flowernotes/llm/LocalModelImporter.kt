package com.flowernotes.llm

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copia il file modello (.task) scelto dall'utente via Storage Access
 * Framework dentro filesDir/models: MediaPipe richiede un percorso sul
 * filesystem, non un content Uri. I file sono grandi (~550 MB), quindi la
 * copia riporta il progresso.
 */
class LocalModelImporter(private val context: Context) {

    /** Copia il modello e ritorna il percorso di destinazione */
    suspend fun import(uri: Uri, onProgress: (Float) -> Unit): String =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            } ?: "model.task"
            val totalBytes = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L

            val dir = File(context.filesDir, "models").apply { mkdirs() }
            // Un solo modello alla volta: i file sono enormi, niente accumulo
            dir.listFiles()?.forEach { it.delete() }
            val dest = File(dir, name)

            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(1 shl 20)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (totalBytes > 0) onProgress(copied.toFloat() / totalBytes)
                    }
                }
            } ?: throw IllegalStateException("cannot open $uri")
            onProgress(1f)
            dest.absolutePath
        }
}
