package es.routerapp.protocol.fingerprint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

class MdnsRecordParserTest {

    @Test
    fun testBuildQuery() {
        val queryBytes = MdnsRecordParser.buildQuery(listOf("_googlecast._tcp.local", "_airplay._tcp.local"))
        val packet = MdnsRecordParser.parse(queryBytes)
        assertNotNull(packet)
        assertEquals(2, packet!!.questions.size)
        assertEquals("_googlecast._tcp.local", packet.questions[0])
        assertEquals("_airplay._tcp.local", packet.questions[1])
        assertEquals(false, packet.isResponse)
    }

    @Test
    fun testParseMdnsResponseWithPtrAndTxt() {
        // Construct a synthetic DNS response packet with PTR and TXT records
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        dos.writeShort(0x0000) // Transaction ID
        dos.writeShort(0x8400) // Flags: Response, Authoritative
        dos.writeShort(0x0000) // QDCOUNT
        dos.writeShort(0x0002) // ANCOUNT (2 Answers: PTR + TXT)
        dos.writeShort(0x0000) // NSCOUNT
        dos.writeShort(0x0000) // ARCOUNT

        // 1. Answer 1: PTR record for "_googlecast._tcp.local" -> "Living Room._googlecast._tcp.local"
        writeLabel(dos, "_googlecast")
        writeLabel(dos, "_tcp")
        writeLabel(dos, "local")
        dos.writeByte(0)
        dos.writeShort(MdnsRecordParser.TYPE_PTR)
        dos.writeShort(0x8001) // Class IN + cache flush
        dos.writeInt(120) // TTL

        val ptrTargetBos = ByteArrayOutputStream()
        val ptrTargetDos = DataOutputStream(ptrTargetBos)
        writeLabel(ptrTargetDos, "Living Room")
        writeLabel(ptrTargetDos, "_googlecast")
        writeLabel(ptrTargetDos, "_tcp")
        writeLabel(ptrTargetDos, "local")
        ptrTargetDos.writeByte(0)
        val ptrRdata = ptrTargetBos.toByteArray()

        dos.writeShort(ptrRdata.size)
        dos.write(ptrRdata)

        // 2. Answer 2: TXT record for "Living Room._googlecast._tcp.local"
        writeLabel(dos, "Living Room")
        writeLabel(dos, "_googlecast")
        writeLabel(dos, "_tcp")
        writeLabel(dos, "local")
        dos.writeByte(0)
        dos.writeShort(MdnsRecordParser.TYPE_TXT)
        dos.writeShort(0x8001)
        dos.writeInt(120)

        val txtBos = ByteArrayOutputStream()
        val txtDos = DataOutputStream(txtBos)
        writeTxtItem(txtDos, "md=Chromecast with Google TV")
        writeTxtItem(txtDos, "fn=Living Room")
        writeTxtItem(txtDos, "id=123456789")
        val txtRdata = txtBos.toByteArray()

        dos.writeShort(txtRdata.size)
        dos.write(txtRdata)

        val bytes = bos.toByteArray()
        val packet = MdnsRecordParser.parse(bytes)

        assertNotNull(packet)
        assertTrue(packet!!.isResponse)
        assertEquals(2, packet.records.size)

        val ptr = packet.records.first { it.type == MdnsRecordParser.TYPE_PTR }
        assertEquals("Living Room._googlecast._tcp.local", ptr.ptrTarget)

        val txt = packet.records.first { it.type == MdnsRecordParser.TYPE_TXT }
        assertEquals("Chromecast with Google TV", txt.txtEntries["md"])
        assertEquals("Living Room", txt.txtEntries["fn"])
        assertEquals("Chromecast with Google TV", packet.findModelIdentifier())
    }

    private fun writeLabel(dos: DataOutputStream, label: String) {
        val b = label.toByteArray(StandardCharsets.UTF_8)
        dos.writeByte(b.size)
        dos.write(b)
    }

    private fun writeTxtItem(dos: DataOutputStream, item: String) {
        val b = item.toByteArray(StandardCharsets.UTF_8)
        dos.writeByte(b.size)
        dos.write(b)
    }
}
