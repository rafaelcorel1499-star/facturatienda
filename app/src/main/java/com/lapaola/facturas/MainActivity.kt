@file:OptIn(ExperimentalMaterial3Api::class)

package com.lapaola.facturas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

val Brown = Color(0xFF7A4B2A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaPaolaTheme {
                val vm: MainViewModel = viewModel()
                App(vm)
            }
        }
    }
}

@Composable
fun LaPaolaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Brown,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF4E3D0),
            onPrimaryContainer = Color(0xFF3B2312),
            secondary = Color(0xFFB5772F),
            secondaryContainer = Color(0xFFF4E3D0),
            background = Color(0xFFFFFAF4),
            surface = Color(0xFFFFFAF4),
            surfaceVariant = Color(0xFFF3E9DE)
        ),
        content = content
    )
}

@Composable
fun App(vm: MainViewModel) {
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }
    BackHandler(enabled = vm.preview != null || vm.screen != Screen.Home) {
        if (vm.preview != null) vm.preview = null else vm.screen = Screen.Home
    }
    Scaffold(
        topBar = { AppHeader() },
        bottomBar = { BottomBar(vm) },
        snackbarHost = { SnackbarHost(snack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val p = vm.preview
            if (p != null) PreviewScreen(vm, p) else when (vm.screen) {
                Screen.Home -> HomeScreen(vm)
                Screen.Invoices -> InvoicesScreen(vm)
                Screen.New -> NewInvoiceScreen(vm)
                Screen.Voice -> VoiceInvoiceScreen(vm)
                Screen.Products -> ProductsScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
fun AppHeader() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Storefront, null, Modifier.size(44.dp), tint = Brown)
            Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(44.dp).background(Color(0xFFBBA89A)))
            Column {
                Text("Panadería", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("La Paola", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    NavigationBarItem(
        selected = selected, onClick = onClick, icon = icon,
        label = { Text(label, fontSize = 10.sp, maxLines = 1) }, alwaysShowLabel = true
    )
}

@Composable
fun BottomBar(vm: MainViewModel) {
    val s = vm.screen
    val inv = vm.preview != null
    NavigationBar(containerColor = Color.White) {
        NavItem("Inicio", (s == Screen.Home || s == Screen.Voice) && !inv, { vm.go(Screen.Home) }) { Icon(Icons.Filled.Home, null) }
        NavItem("Facturas", s == Screen.Invoices, { vm.go(Screen.Invoices) }) { Icon(Icons.Filled.Description, null) }
        NavItem("Nueva Factura", s == Screen.New && !inv, { vm.go(Screen.New) }) {
            Box(Modifier.size(34.dp).background(Brown, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, null, tint = Color.White)
            }
        }
        NavItem("Base de Datos", s == Screen.Products, { vm.go(Screen.Products) }) { Icon(Icons.Filled.Storage, null) }
        NavItem("Configuración", s == Screen.Settings, { vm.go(Screen.Settings) }) { Icon(Icons.Filled.Settings, null) }
    }
}

// ---------------- Inicio ----------------

@Composable
fun HomeScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BigButton("Crear Factura", Icons.Filled.Receipt) { vm.go(Screen.New) }
        BigButton("Factura por Voz", Icons.Filled.Mic) { vm.go(Screen.Voice) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Productos", Icons.Filled.Inventory2, Modifier.weight(1f)) { vm.go(Screen.Products) }
            Tile("Facturas Generadas", Icons.Filled.Description, Modifier.weight(1f)) { vm.go(Screen.Invoices) }
            Tile("Configuración", Icons.Filled.Settings, Modifier.weight(1f)) { vm.go(Screen.Settings) }
        }
    }
}

@Composable
private fun BigButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().height(84.dp), shape = RoundedCornerShape(22.dp)) {
        Icon(icon, null, Modifier.size(38.dp))
        Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(44.dp).background(Color.White.copy(alpha = 0.5f)))
        Text(text, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Icon(Icons.Filled.KeyboardArrowRight, null)
    }
}

@Composable
private fun Tile(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.height(110.dp), shape = RoundedCornerShape(16.dp),
        color = Color.White, border = BorderStroke(1.dp, Color(0xFFD9C9BA))
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(36.dp), tint = Brown)
            Spacer(Modifier.height(8.dp))
            Text(text, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// ---------------- Componentes compartidos ----------------

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun SectionTitle(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Brown)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Nombre con autocompletado de clientes anteriores + teléfono + dirección. */
@Composable
fun ClientForm(state: ClientState, suggestions: List<Invoice>) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = suggestions.filter {
        state.name.isNotBlank() && it.clientName.contains(state.name, ignoreCase = true) &&
            !it.clientName.equals(state.name, ignoreCase = true)
    }.take(5)
    Panel {
        SectionTitle(Icons.Filled.Person, "Datos del Cliente")
        ExposedDropdownMenuBox(expanded = expanded && filtered.isNotEmpty(), onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { state.name = it; expanded = true },
                label = { Text("Nombre del Cliente") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                trailingIcon = {
                    if (state.name.isNotEmpty()) IconButton({ state.name = "" }) { Icon(Icons.Filled.Close, null) }
                },
                singleLine = true,
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded && filtered.isNotEmpty(), onDismissRequest = { expanded = false }) {
                filtered.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.clientName + if (c.phone.isNotBlank()) "  ·  ${c.phone}" else "") },
                        onClick = { state.name = c.clientName; state.phone = c.phone; state.address = c.address; expanded = false }
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.phone, onValueChange = { state.phone = it },
            label = { Text("Teléfono") }, leadingIcon = { Icon(Icons.Filled.Phone, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.address, onValueChange = { state.address = it },
            label = { Text("Dirección") }, leadingIcon = { Icon(Icons.Filled.Home, null) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
    }
}
