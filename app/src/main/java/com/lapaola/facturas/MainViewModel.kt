package com.lapaola.facturas

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { Home, Invoices, New, Voice, Products, Settings }
enum class Source { Db, Voice }

class ClientState {
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var address by mutableStateOf("")
    fun clear() { name = ""; phone = ""; address = "" }
}

/** source = null -> factura ya guardada (solo se puede imprimir). */
data class PreviewState(val data: ReceiptData, val source: Source?)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.build(app).dao()
    val settings = Settings(app)

    val products = dao.products().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Product>())
    val invoices = dao.invoices().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Invoice>())
    val clients = dao.clients().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Invoice>())

    var screen by mutableStateOf(Screen.Home)
    var preview by mutableStateOf<PreviewState?>(null)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    fun say(m: String) { messages.tryEmit(m) }
    fun go(s: Screen) { preview = null; screen = s }

    val dbClient = ClientState()
    val voiceClient = ClientState()
    val cart = mutableStateListOf<CartLine>()
    val voiceLines = mutableStateListOf<CartLine>()

    // ----- configuración (valores guardados) -----
    var printerIp by mutableStateOf(settings.printerIp)
    var paperMm by mutableStateOf(settings.paperMm)
    var noAccents by mutableStateOf(settings.noAccents)
    val found = MutableStateFlow<List<String>>(emptyList())
    var scanning by mutableStateOf(false)

    fun scan() {
        if (scanning) return
        scanning = true
        found.value = emptyList()
        viewModelScope.launch {
            runCatching { Printer.discover { ip -> found.update { (it + ip).distinct().sorted() } } }
            scanning = false
        }
    }

    fun saveSettings(ip: String, mm: Int, noAcc: Boolean) {
        settings.printerIp = ip.trim(); settings.paperMm = mm; settings.noAccents = noAcc
        printerIp = ip.trim(); paperMm = mm; noAccents = noAcc
        say("Cambios guardados")
    }

    fun testPrint(ip: String, mm: Int, noAcc: Boolean) {
        val d = ReceiptData(
            1, System.currentTimeMillis(), "Prueba de impresión", "3001234567", "Calle 10 #5-23",
            listOf(CartLine("Pan de Piña", 2, 2500), CartLine("Banano Maduro", 1, 600))
        )
        viewModelScope.launch { doPrint(d, ip.trim(), mm, noAcc) }
    }

    private suspend fun doPrint(
        d: ReceiptData,
        ip: String = settings.printerIp,
        mm: Int = settings.paperMm,
        noAcc: Boolean = settings.noAccents
    ) {
        if (ip.isBlank()) { say("Primero selecciona una impresora en Configuración"); return }
        val bytes = Printer.build(buildReceipt(d, if (mm == 80) 48 else 32), noAcc)
        runCatching { Printer.send(ip, bytes) }
            .onSuccess { say("Enviado a la impresora") }
            .onFailure { say("No se pudo imprimir: ${it.message ?: "revisa la conexión"}") }
    }

    // ----- productos -----
    fun addProduct(name: String, price: Long) { viewModelScope.launch { dao.insertProduct(Product(name = name, price = price)) } }
    fun deleteProduct(p: Product) { viewModelScope.launch { dao.deleteProduct(p.id) } }

    // ----- carrito (base de datos) -----
    fun addToCart(p: Product) {
        val i = cart.indexOfFirst { it.name == p.name && it.unitPrice == p.price }
        if (i >= 0) cart[i] = cart[i].copy(qty = cart[i].qty + 1) else cart.add(CartLine(p.name, 1, p.price))
    }

    fun changeQty(i: Int, d: Int) {
        val n = cart[i].qty + d
        if (n <= 0) cart.removeAt(i) else cart[i] = cart[i].copy(qty = n)
    }

    // ----- voz -----
    fun addVoice(text: String): Boolean {
        val l = parseDictation(text)
        if (l == null) {
            say("No entendí «$text». Di por ejemplo: 4 panes de piña valor 2500 pesos")
            return false
        }
        voiceLines.add(l)
        say("Agregado: ${l.qty} x ${l.name} a ${cop(l.unitPrice)}")
        return true
    }

    // ----- facturas -----
    fun openPreview(source: Source) {
        val c = if (source == Source.Db) dbClient else voiceClient
        val lines = (if (source == Source.Db) cart else voiceLines).toList()
        if (lines.isEmpty()) { say("Agrega al menos un producto"); return }
        preview = PreviewState(
            ReceiptData(
                null, System.currentTimeMillis(), c.name.trim().ifBlank { "Cliente general" },
                c.phone.trim(), c.address.trim(), lines
            ), source
        )
    }

    fun confirm(print: Boolean) {
        val p = preview ?: return
        val src = p.source ?: return
        viewModelScope.launch {
            val d = p.data
            val id = dao.saveInvoice(
                Invoice(dateMillis = d.dateMillis, clientName = d.client, phone = d.phone, address = d.address, total = d.total),
                d.lines.map { InvoiceItem(invoiceId = 0, name = it.name, qty = it.qty, unitPrice = it.unitPrice) }
            )
            if (src == Source.Db) { cart.clear(); dbClient.clear() } else { voiceLines.clear(); voiceClient.clear() }
            preview = null
            screen = Screen.Invoices
            say("Factura #${invNumber(id)} guardada")
            if (print) doPrint(d.copy(number = id))
        }
    }

    private suspend fun dataOf(inv: Invoice): ReceiptData {
        val items = dao.itemsOf(inv.id)
        return ReceiptData(
            inv.id, inv.dateMillis, inv.clientName, inv.phone, inv.address,
            items.map { CartLine(it.name, it.qty, it.unitPrice) }
        )
    }

    fun openSaved(inv: Invoice) { viewModelScope.launch { preview = PreviewState(dataOf(inv), null) } }
    fun printSaved(inv: Invoice) { viewModelScope.launch { doPrint(dataOf(inv)) } }
    fun printPreview() { val p = preview ?: return; viewModelScope.launch { doPrint(p.data) } }
    fun deleteInvoice(inv: Invoice) { viewModelScope.launch { dao.deleteInvoice(inv.id); say("Factura eliminada") } }
}
