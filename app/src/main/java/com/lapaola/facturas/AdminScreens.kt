@file:OptIn(ExperimentalMaterial3Api::class)

package com.lapaola.facturas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------- Base de datos de productos ----------------

@Composable
fun ProductsScreen(vm: MainViewModel) {
    val products by vm.products.collectAsState()
    var q by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Product?>(null) }
    val list = products.filter { q.isBlank() || it.name.contains(q, ignoreCase = true) }

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SectionTitle(Icons.Filled.Storage, "Base de Datos - Productos") }
        item {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, placeholder = { Text("Buscar producto…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Panel {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Nombre del Producto") },
                    leadingIcon = { Icon(Icons.Filled.Sell, null) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price, onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Valor en Pesos Colombianos (COP)") },
                    leadingIcon = { Icon(Icons.Filled.AttachMoney, null) },
                    supportingText = { price.toLongOrNull()?.let { Text(cop(it)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Button({
                    val v = price.toLongOrNull()
                    if (name.isBlank() || v == null || v <= 0) vm.say("Escribe el nombre y el valor")
                    else { vm.addProduct(name.trim(), v); name = ""; price = ""; vm.say("Producto guardado") }
                }, Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Agregar Producto", fontSize = 16.sp)
                }
            }
        }
        items(list, key = { it.id }) { p ->
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(cop(p.price), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton({ toDelete = p }) { Icon(Icons.Filled.Delete, "Eliminar") }
                }
            }
        }
    }

    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar «${p.name}» de la base de datos?") },
            confirmButton = { TextButton({ vm.deleteProduct(p); toDelete = null }) { Text("Eliminar") } },
            dismissButton = { TextButton({ toDelete = null }) { Text("Cancelar") } }
        )
    }
}

// ---------------- Configuración ----------------

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var ip by remember { mutableStateOf(vm.printerIp) }
    var mm by remember { mutableStateOf(vm.paperMm) }
    var noAcc by remember { mutableStateOf(vm.noAccents) }
    var openPrinter by remember { mutableStateOf(false) }
    var openPaper by remember { mutableStateOf(false) }
    val found by vm.found.collectAsState()
    LaunchedEffect(Unit) { vm.scan() }
    val options = (found + listOfNotNull(ip.takeIf { it.isNotBlank() })).distinct()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(Icons.Filled.Print, "Impresora Térmica en Red")
        ExposedDropdownMenuBox(expanded = openPrinter, onExpandedChange = { openPrinter = it }) {
            OutlinedTextField(
                value = if (ip.isBlank()) "" else "Impresora térmica ($ip)",
                onValueChange = {}, readOnly = true,
                placeholder = { Text("Selecciona una impresora") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(openPrinter) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = openPrinter, onDismissRequest = { openPrinter = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text("Impresora térmica ($o)") }, onClick = { ip = o; openPrinter = false })
                }
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(if (vm.scanning) "Buscando…" else "No se encontraron impresoras") },
                        onClick = { openPrinter = false }, enabled = false
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (vm.scanning) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Buscando impresoras en la red…", fontSize = 14.sp)
            } else {
                Icon(Icons.Filled.Search, null)
                TextButton({ vm.scan() }) { Text("Buscar impresoras de nuevo") }
            }
        }
        OutlinedTextField(
            value = ip, onValueChange = { ip = it.trim() },
            label = { Text("O escribe la IP manualmente") }, placeholder = { Text("192.168.1.15") },
            supportingText = { Text("Puerto RAW 9100 (por defecto). Celular e impresora en la misma red Wi-Fi.") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()
        SectionTitle(Icons.Filled.Description, "Tamaño de Papel")
        ExposedDropdownMenuBox(expanded = openPaper, onExpandedChange = { openPaper = it }) {
            OutlinedTextField(
                value = "$mm mm", onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(openPaper) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = openPaper, onDismissRequest = { openPaper = false }) {
                listOf(80, 57).forEach { o ->
                    DropdownMenuItem(text = { Text("$o mm") }, onClick = { mm = o; openPaper = false })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = noAcc, onCheckedChange = { noAcc = it })
            Text("Imprimir sin tildes (si tu impresora muestra símbolos raros)", fontSize = 14.sp)
        }

        Button({ vm.saveSettings(ip, mm, noAcc) }, Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Save, null); Spacer(Modifier.width(8.dp)); Text("Guardar Cambios", fontSize = 17.sp)
        }
        OutlinedButton({ vm.testPrint(ip, mm, noAcc) }, Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Filled.Print, null); Spacer(Modifier.width(8.dp)); Text("Imprimir prueba")
        }
    }
}
