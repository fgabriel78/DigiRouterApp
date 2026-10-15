package es.routerapp.protocol

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupEndpointTest {

    @Test
    fun testResolveBackupPathWithAdminDevInfo() {
        val client = RouterClient()
        val devInfo = mapOf(
            "modelName" to "XGB430v Pro",
            "X_TP_DevModelVersion" to "1.0",
            "X_TP_BuildDate" to "250808",
            "X_TP_BuildTime" to "72518",
        )
        val path = client.resolveBackupPath(devInfo, isAdmin = true)
        assertEquals("/cgi/XGB430vProV125080872518n.bin", path)
    }

    @Test
    fun testResolveBackupPathWithNonAdminDevInfo() {
        val client = RouterClient()
        val devInfo = mapOf(
            "modelName" to "XGB430v Pro",
            "X_TP_DevModelVersion" to "1.0",
            "X_TP_BuildDate" to "250808",
            "X_TP_BuildTime" to "72518",
        )
        val path = client.resolveBackupPath(devInfo, isAdmin = false)
        assertEquals("/cgi/XGB430vProV125080872518n_backupcfg.bin", path)
    }

    @Test
    fun testResolveBackupPathFallbackWhenMissingInfo() {
        val client = RouterClient()
        assertEquals("/cgi/conf.bin", client.resolveBackupPath(null))
        assertEquals("/cgi/conf.bin", client.resolveBackupPath(emptyMap()))
        assertEquals("/cgi/conf.bin", client.resolveBackupPath(mapOf("modelName" to "XGB430v Pro")))
    }

    @Test
    fun testDownloadBackupExecutesGetAndReturnsBytes() {
        val expectedBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x12, 0x34)
        var requestedPath: String? = null

        val interceptor = Interceptor { chain ->
            val req = chain.request()
            requestedPath = req.url.encodedPath
            Response.Builder()
                .request(req)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(expectedBytes.toResponseBody("application/octet-stream".toMediaType()))
                .build()
        }

        val http = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val client = RouterClient("http://192.168.1.1", http)

        val downloaded = client.downloadBackup()
        assertTrue(expectedBytes.contentEquals(downloaded))
        assertEquals("/cgi/conf.bin", requestedPath)
    }

    @Test
    fun testRestoreBackupExecutesMultipartUpload() {
        val configBytes = "fake-config-binary-data".toByteArray()
        var uploadedContentType: String? = null
        var uploadPath: String? = null

        val interceptor = Interceptor { chain ->
            val req = chain.request()
            val path = req.url.encodedPath
            if (path == "/cgi/confup") {
                uploadPath = path
                uploadedContentType = req.body?.contentType()?.toString()
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("OK".toResponseBody("text/plain".toMediaType()))
                    .build()
            } else {
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("$.ret=0;".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val http = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val client = RouterClient("http://192.168.1.1", http)

        client.restoreBackup(configBytes)
        assertEquals("/cgi/confup", uploadPath)
        assertTrue(uploadedContentType?.startsWith("multipart/form-data") == true)
    }
}
