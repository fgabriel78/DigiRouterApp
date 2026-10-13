package es.routerapp.protocol.fingerprint

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

data class MdnsRecord(
    val name: String,
    val type: Int,
    val ttl: Long,
    val ptrTarget: String? = null,
    val txtEntries: Map<String, String> = emptyMap(),
    val srvPort: Int? = null,
    val srvTarget: String? = null,
)

data class MdnsPacket(
    val transactionId: Int,
    val isResponse: Boolean,
    val questions: List<String>,
    val records: List<MdnsRecord>,
) {
    /**
     * Extracts consolidated key-value TXT metadata across all TXT records in this packet.
     */
    fun allTxtAttributes(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (rec in records) {
            if (rec.type == MdnsRecordParser.TYPE_TXT) {
                map.putAll(rec.txtEntries)
            }
        }
        return map
    }

    /**
     * Searches for common model or device metadata attributes in TXT records.
     * e.g., 'md' (Chromecast), 'model' (Apple / Hue / Sonos), 'am' (Apple AirPlay model)
     */
    fun findModelIdentifier(): String? {
        val attrs = allTxtAttributes()
        return attrs["model"]
            ?: attrs["md"]
            ?: attrs["am"]
            ?: attrs["modelid"]
    }
}

object MdnsRecordParser {
    const val TYPE_A = 1
    const val TYPE_PTR = 12
    const val TYPE_TXT = 16
    const val TYPE_AAAA = 28
    const val TYPE_SRV = 33

    /**
     * Builds an mDNS multicast query packet for the given service names.
     */
    fun buildQuery(serviceTypes: List<String>): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        dos.writeShort(0x0000) // Transaction ID 0
        dos.writeShort(0x0000) // Standard query
        dos.writeShort(serviceTypes.size) // QDCOUNT
        dos.writeShort(0) // ANCOUNT
        dos.writeShort(0) // NSCOUNT
        dos.writeShort(0) // ARCOUNT

        for (st in serviceTypes) {
            writeDomainName(dos, st)
            dos.writeShort(TYPE_PTR)
            dos.writeShort(0x0001) // Class IN
        }
        return bos.toByteArray()
    }

    private fun writeDomainName(dos: DataOutputStream, name: String) {
        val labels = name.split('.').filter { it.isNotEmpty() }
        for (label in labels) {
            val bytes = label.toByteArray(StandardCharsets.UTF_8)
            dos.writeByte(bytes.size)
            dos.write(bytes)
        }
        dos.writeByte(0) // Root label
    }

    /**
     * Parses an mDNS UDP payload into structured DNS questions and Resource Records.
     */
    fun parse(data: ByteArray, length: Int = data.size): MdnsPacket? {
        if (length < 12) return null
        val buffer = if (length == data.size) data else data.copyOf(length)

        return runCatching {
            val dis = DataInputStream(ByteArrayInputStream(buffer))
            val txId = dis.readUnsignedShort()
            val flags = dis.readUnsignedShort()
            val isResponse = (flags and 0x8000) != 0

            val qdCount = dis.readUnsignedShort()
            val anCount = dis.readUnsignedShort()
            val nsCount = dis.readUnsignedShort()
            val arCount = dis.readUnsignedShort()

            var offset = 12
            val questions = mutableListOf<String>()
            for (i in 0 until qdCount) {
                val (qName, newOffset) = readName(buffer, offset)
                offset = newOffset + 4 // skip type (2) and class (2)
                questions.add(qName)
            }

            val records = mutableListOf<MdnsRecord>()
            val totalRrs = anCount + nsCount + arCount
            for (i in 0 until totalRrs) {
                if (offset >= buffer.size) break
                val (rrName, afterName) = readName(buffer, offset)
                offset = afterName
                if (offset + 10 > buffer.size) break

                val rrType = readShort(buffer, offset)
                val rrClass = readShort(buffer, offset + 2) and 0x7FFF // strip cache-flush bit
                val ttl = readInt(buffer, offset + 4)
                val rdLength = readShort(buffer, offset + 8)
                val rdataStart = offset + 10
                offset = rdataStart + rdLength

                var ptrTarget: String? = null
                var txtEntries = emptyMap<String, String>()
                var srvPort: Int? = null
                var srvTarget: String? = null

                when (rrType) {
                    TYPE_PTR -> {
                        ptrTarget = readName(buffer, rdataStart).first
                    }
                    TYPE_TXT -> {
                        txtEntries = parseTxtRdata(buffer, rdataStart, rdLength)
                    }
                    TYPE_SRV -> {
                        if (rdLength >= 6) {
                            srvPort = readShort(buffer, rdataStart + 4)
                            srvTarget = readName(buffer, rdataStart + 6).first
                        }
                    }
                }

                records.add(
                    MdnsRecord(
                        name = rrName,
                        type = rrType,
                        ttl = ttl,
                        ptrTarget = ptrTarget,
                        txtEntries = txtEntries,
                        srvPort = srvPort,
                        srvTarget = srvTarget,
                    ),
                )
            }

            MdnsPacket(
                transactionId = txId,
                isResponse = isResponse,
                questions = questions,
                records = records,
            )
        }.getOrNull()
    }

    private fun parseTxtRdata(buffer: ByteArray, start: Int, length: Int): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var cur = start
        val end = start + length
        while (cur < end) {
            val len = buffer[cur].toInt() and 0xFF
            cur++
            if (len == 0 || cur + len > end) continue
            val entry = String(buffer, cur, len, StandardCharsets.UTF_8)
            cur += len
            val eq = entry.indexOf('=')
            if (eq >= 0) {
                val k = entry.substring(0, eq).trim()
                val v = entry.substring(eq + 1).trim()
                map[k] = v
            } else {
                map[entry.trim()] = ""
            }
        }
        return map
    }

    private fun readName(buffer: ByteArray, initialOffset: Int): Pair<String, Int> {
        val labels = mutableListOf<String>()
        var cur = initialOffset
        var jumped = false
        var offsetAfterFirstJump = initialOffset

        var safety = 0
        while (cur < buffer.size && safety++ < 64) {
            val len = buffer[cur].toInt() and 0xFF
            if (len == 0) {
                if (!jumped) offsetAfterFirstJump = cur + 1
                break
            }
            if ((len and 0xC0) == 0xC0) {
                // Compression pointer
                if (cur + 1 >= buffer.size) break
                val b2 = buffer[cur + 1].toInt() and 0xFF
                val pointer = ((len and 0x3F) shl 8) or b2
                if (!jumped) {
                    offsetAfterFirstJump = cur + 2
                    jumped = true
                }
                cur = pointer
            } else {
                cur++
                if (cur + len > buffer.size) break
                val label = String(buffer, cur, len, StandardCharsets.UTF_8)
                labels.add(label)
                cur += len
                if (!jumped) offsetAfterFirstJump = cur
            }
        }

        return labels.joinToString(".") to offsetAfterFirstJump
    }

    private fun readShort(buffer: ByteArray, offset: Int): Int {
        val b1 = buffer[offset].toInt() and 0xFF
        val b2 = buffer[offset + 1].toInt() and 0xFF
        return (b1 shl 8) or b2
    }

    private fun readInt(buffer: ByteArray, offset: Int): Long {
        val b1 = (buffer[offset].toLong() and 0xFF) shl 24
        val b2 = (buffer[offset + 1].toLong() and 0xFF) shl 16
        val b3 = (buffer[offset + 2].toLong() and 0xFF) shl 8
        val b4 = buffer[offset + 3].toLong() and 0xFF
        return b1 or b2 or b3 or b4
    }
}
