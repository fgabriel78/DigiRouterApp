package es.digirouter.app

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

interface VpnProfileStore {
    fun saveProfile(content: String, filename: String = "client.ovpn"): File
    fun getProfileFile(filename: String = "client.ovpn"): File?
    fun getShareableUri(file: File, context: Context): Uri?
    fun clearProfiles()
}

class FileVpnProfileStore(
    private val directory: File,
) : VpnProfileStore {

    constructor(context: Context) : this(
        directory = File(context.cacheDir, "vpn")
    )

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    override fun saveProfile(content: String, filename: String): File {
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, filename)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    override fun getProfileFile(filename: String): File? {
        val file = File(directory, filename)
        return if (file.exists()) file else null
    }

    override fun getShareableUri(file: File, context: Context): Uri? = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    override fun clearProfiles() {
        directory.listFiles()?.forEach { it.delete() }
    }
}
