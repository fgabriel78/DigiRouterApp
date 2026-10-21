package es.digirouter.app

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class BackupItem(
    val id: String,
    val filename: String,
    val timestampEpochMs: Long,
    val deviceModel: String = "",
    val firmwareVersion: String = "",
    val sizeBytes: Long = 0L,
) {
    val formattedDate: String
        get() {
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampEpochMs), ZoneId.systemDefault())
            return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        }

    val displayName: String
        get() = "Copia $formattedDate"
}

interface BackupStore {
    fun listBackups(): List<BackupItem>
    fun saveBackup(data: ByteArray, deviceModel: String = "", firmwareVersion: String = ""): BackupItem
    fun getBackupFile(item: BackupItem): File?
    fun deleteBackup(item: BackupItem): Boolean
    fun getShareableUri(item: BackupItem, context: Context): Uri?
    fun clearAll()
}

class FileBackupStore(
    private val directory: File,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true },
    private val maxBackups: Int = 5,
) : BackupStore {

    constructor(context: Context, maxBackups: Int = 5) : this(
        directory = File(context.filesDir, "backups"),
        maxBackups = maxBackups,
    )

    private val indexFile: File
        get() = File(directory, "backups_index.json")

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    @Synchronized
    override fun listBackups(): List<BackupItem> {
        val items = readIndex()
        val valid = items.filter { File(directory, it.filename).exists() }
        if (valid.size != items.size) {
            writeIndex(valid)
        }
        return valid.sortedByDescending { it.timestampEpochMs }
    }

    @Synchronized
    override fun saveBackup(data: ByteArray, deviceModel: String, firmwareVersion: String): BackupItem {
        var now = System.currentTimeMillis()
        var targetFile = File(directory, "backup_${now}.bin")
        while (targetFile.exists()) {
            Thread.sleep(2)
            now = System.currentTimeMillis()
            targetFile = File(directory, "backup_${now}.bin")
        }
        val filename = targetFile.name
        targetFile.writeBytes(data)

        val newItem = BackupItem(
            id = filename,
            filename = filename,
            timestampEpochMs = now,
            deviceModel = deviceModel,
            firmwareVersion = firmwareVersion,
            sizeBytes = data.size.toLong(),
        )

        val currentList = listBackups().toMutableList()
        currentList.add(0, newItem)

        // Strict 5-slot FIFO retention: prune oldest when exceeding maxBackups
        while (currentList.size > maxBackups) {
            val oldest = currentList.removeAt(currentList.lastIndex)
            val file = File(directory, oldest.filename)
            if (file.exists()) {
                file.delete()
            }
        }

        writeIndex(currentList)
        return newItem
    }

    @Synchronized
    override fun getBackupFile(item: BackupItem): File? {
        val file = File(directory, item.filename)
        return if (file.exists()) file else null
    }

    @Synchronized
    override fun deleteBackup(item: BackupItem): Boolean {
        val file = File(directory, item.filename)
        val deleted = if (file.exists()) file.delete() else true
        val currentList = listBackups().toMutableList()
        val removed = currentList.removeAll { it.id == item.id || it.filename == item.filename }
        if (removed) {
            writeIndex(currentList)
        }
        return deleted
    }

    override fun getShareableUri(item: BackupItem, context: Context): Uri? {
        val file = getBackupFile(item) ?: return null
        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    @Synchronized
    override fun clearAll() {
        directory.listFiles()?.forEach { it.delete() }
        writeIndex(emptyList())
    }

    private fun readIndex(): List<BackupItem> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<BackupItem>>(indexFile.readText())
        }.getOrDefault(emptyList())
    }

    private fun writeIndex(items: List<BackupItem>) {
        runCatching {
            indexFile.writeText(json.encodeToString(items))
        }
    }
}
