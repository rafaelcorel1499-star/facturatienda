package com.lapaola.facturas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.nio.charset.Charset
import java.text.Normalizer

/** Impresión ESC/POS directa por red (puerto RAW 9100). */
object Printer {
    private const val PORT = 9100

    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    fun stripAccents(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("¡", "")
            .replace("¿", "")

    fun build(lines: List<RLine>, noAccents: Boolean): ByteArray {
        val out = ByteArrayOutputStream()
        val cs: Charset = if (noAccents) Charsets.US_ASCII
        else runCatching { Charset.forName("IBM850") }.getOrDefault(Charsets.US_ASCII)

        out.write(b(0x1B, 0x40))                       // inicializar
        if (!noAccents) out.write(b(0x1B, 0x74, 0x02)) // tabla de caracteres PC850 (ñ, tildes)
        for (l in lines) {
            out.write(b(0x1B, 0x61, if (l.center) 1 else 0))
            out.write(b(0x1B, 0x45, if (l.bold || l.big) 1 else 0))
            out.write(b(0x1D, 0x21, if (l.big) 0x11 else 0))
            val t = if (noAccents) stripAccents(l.text) else l.text
            out.write(t.toByteArray(cs))
            out.write(0x0A)
        }
        out.write(b(0x1B, 0x61, 0))
        out.write(b(0x1B, 0x45, 0))
        out.write(b(0x1D, 0x21, 0))
        out.write(b(0x1B, 0x64, 4))              // avanzar 4 líneas
        out.write(b(0x1D, 0x56, 0x42, 0x00))     // corte parcial
        return out.toByteArray()
    }

    suspend fun send(address: String, data: ByteArray) = withContext(Dispatchers.IO) {
        val parts = address.trim().split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: PORT
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), 4000)
            s.soTimeout = 4000
            val o = s.getOutputStream()
            o.write(data)
            o.flush()
            Thread.sleep(400)
        }
    }

    private fun subnet(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress?.substringBeforeLast('.')
    }.getOrNull()

    private fun isOpen(ip: String): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(ip, PORT), 350); true }
    } catch (e: Exception) {
        false
    }

    /** Recorre la red local (x.x.x.1-254) buscando equipos con el puerto 9100 abierto. */
    suspend fun discover(onFound: (String) -> Unit) {
        coroutineScope {
            val base = subnet() ?: return@coroutineScope
            val sem = Semaphore(80)
            (1..254).map { i ->
                async(Dispatchers.IO) {
                    sem.withPermit {
                        val ip = "$base.$i"
                        if (isOpen(ip)) onFound(ip)
                    }
                }
            }.awaitAll()
        }
    }
}
