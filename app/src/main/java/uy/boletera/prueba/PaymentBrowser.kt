package uy.boletera.prueba

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.net.InetAddress
import java.net.ServerSocket
import java.security.SecureRandom
import kotlin.concurrent.thread

/** Chrome handles all bank/card credentials. The one-shot POST relay stays on loopback. */
class PaymentBrowser(private val context: Context) {
    private var relay: ServerSocket? = null
    fun available(): Boolean = try {
        context.packageManager.getApplicationInfo("com.android.chrome", 0).enabled
    } catch (_: Exception) { false }

    private fun launch(url: String): Boolean = try {
        val extras = Bundle().apply { putBinder("android.support.customtabs.extra.SESSION", null) }
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.android.chrome")
            putExtras(extras)
            putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", 0xFF101D2A.toInt())
        })
        true
    } catch (_: Exception) { false }

    fun openPrex(url: String): Boolean = PaymentPolicy.prexLink(url) && available() && launch(url)

    fun openBrou(action: String, fields: Map<String, String>, cents: Long): Boolean {
        if (!available() || !PaymentPolicy.validBrou(action, fields, cents)) return false
        return try {
            close()
            val server = ServerSocket(0, 2, InetAddress.getByName("127.0.0.1")).apply { soTimeout = 60000 }
            relay = server
            val nonce = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
            val payload = buildString {
                append("<!doctype html><meta charset=\"utf-8\"><title>Abriendo eBROU</title><form method=\"post\" action=\"")
                append(PaymentPolicy.BROU_ACTION); append("\">")
                fields.forEach { (name, value) -> append("<input type=\"hidden\" name=\"${escape(name)}\" value=\"${escape(value)}\">") }
                append("</form><script nonce=\"$nonce\">document.forms[0].submit()</script>")
            }.toByteArray(Charsets.UTF_8)
            thread(name = "payment-handoff", isDaemon = true) {
                val deadline = System.currentTimeMillis() + 60000
                try {
                    while (!server.isClosed && System.currentTimeMillis() < deadline) {
                        server.soTimeout = (deadline - System.currentTimeMillis()).coerceIn(1, 60000).toInt()
                        var served = false
                        server.accept().use { socket ->
                            socket.soTimeout = 2000
                            val input = socket.getInputStream()
                            val request = StringBuilder()
                            while (request.length < 2048) {
                                val char = input.read()
                                if (char < 0 || char == 10) break
                                if (char != 13) request.append(char.toChar())
                            }
                            val parts = request.toString().split(' ')
                            val headersRead = StringBuilder()
                            while (headersRead.length < 8192 && !headersRead.endsWith("\r\n\r\n") && !headersRead.endsWith("\n\n")) {
                                val char = input.read()
                                if (char < 0) break
                                headersRead.append(char.toChar())
                                if (headersRead.toString() == "\r\n" || headersRead.toString() == "\n") break
                            }
                            val output = socket.getOutputStream()
                            if (parts.size != 3 || parts[0] != "GET" || parts[1] != "/$nonce" || headersRead.length >= 8192) {
                                output.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                            } else {
                                val headers = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${payload.size}\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; script-src 'nonce-$nonce'; form-action https://ebanking.brou.com.uy; base-uri 'none'; frame-ancestors 'none'\r\nConnection: close\r\n\r\n"
                                output.write(headers.toByteArray()); output.write(payload); output.flush()
                                served = true
                            }
                        }
                        if (served) break
                    }
                } catch (_: Exception) { /* No retries or logging of payment data. */ }
                finally { payload.fill(0); try { server.close() } catch (_: Exception) {} }
            }
            launch("http://127.0.0.1:${server.localPort}/$nonce").also { if (!it) close() }
        } catch (_: Exception) { close(); false }
    }
    fun close() { try { relay?.close() } catch (_: Exception) {}; relay = null }
    private fun escape(value: String) = value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
}
