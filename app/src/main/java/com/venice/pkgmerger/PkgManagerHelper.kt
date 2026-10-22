package com.venice.pkgmerger

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

object PkgManagerHelper {

    // Découpage universel (PS3, PS4, PS5)
    fun splitPkg(
        context: Context,
        sourceUri: Uri,
        destinationFolderUri: Uri,
        chunkSizeMB: Long = 4000,
        onProgress: (percent: Int) -> Unit
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            val inputStream: InputStream = resolver.openInputStream(sourceUri) ?: return false
            val totalSize = inputStream.available().toLong()
            val chunkSizeBytes = chunkSizeMB * 1024 * 1024

            val buffer = ByteArray(1024 * 1024)
            var bytesRead: Int
            var currentChunkIndex = 0
            var totalBytesProcessed = 0L

            while (true) {
                val chunkFileName = "part_${String.format("%03d", currentChunkIndex + 1)}.pkg"
                
                // Création directe via le ContentResolver pour éviter tout problème de dépendance
                val chunkUri = android.provider.DocumentsContract.createDocument(
                    resolver,
                    destinationFolderUri,
                    "application/octet-stream",
                    chunkFileName
                ) ?: break

                val outputStream: OutputStream = resolver.openOutputStream(chunkUri) ?: break
                var currentChunkBytes = 0L

                while (currentChunkBytes < chunkSizeBytes) {
                    val toRead = minOf(buffer.size.toLong(), chunkSizeBytes - currentChunkBytes).toInt()
                    bytesRead = inputStream.read(buffer, 0, toRead)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    currentChunkBytes += bytesRead
                    totalBytesProcessed += bytesRead

                    val progress = ((totalBytesProcessed.toDouble() / totalSize) * 100).toInt()
                    onProgress(progress)
                }

                outputStream.flush()
                outputStream.close()

                if (bytesRead == -1) break
                currentChunkIndex++
            }

            inputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fusion universelle (PS3, PS4, PS5)
    fun mergePkg(
        context: Context,
        sourceUris: List<Uri>,
        destinationFolderUri: Uri,
        outputFileName: String,
        onProgress: (percent: Int) -> Unit
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            
            val outputUri = android.provider.DocumentsContract.createDocument(
                resolver,
                destinationFolderUri,
                "application/octet-stream",
                outputFileName
            ) ?: return false

            val outputStream: OutputStream = resolver.openOutputStream(outputUri) ?: return false
            val buffer = ByteArray(1024 * 1024)

            var totalSize = 0L
            sourceUris.forEach { uri ->
                resolver.openFileDescriptor(uri, "r")?.use { fd ->
                    totalSize += fd.statSize
                }
            }

            var totalBytesProcessed = 0L

            for (uri in sourceUris) {
                val inputStream = resolver.openInputStream(uri) ?: continue
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesProcessed += bytesRead
                    val progress = ((totalBytesProcessed.toDouble() / totalSize) * 100).toInt()
                    onProgress(progress)
                }

                inputStream.close()
            }

            outputStream.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Vérification SHA-256
    fun calculateSha256(context: Context, fileUri: Uri): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream = context.contentResolver.openInputStream(fileUri) ?: return ""
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }

            inputStream.close()
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
