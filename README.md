# Panadería y Tienda La Paola – App de facturación (Android)

App Android (Kotlin + Jetpack Compose + Room) para crear facturas tipo tirilla de supermercado e imprimirlas en impresoras térmicas de red (ESC/POS, puerto 9100).

## Compilar con GitHub
1. Crea un repositorio nuevo y sube **todo** el contenido de esta carpeta (incluida la carpeta oculta `.github`).
2. Ve a la pestaña **Actions** → "Compilar APK" (corre solo al hacer push, o con *Run workflow*).
3. Al terminar, descarga el artefacto **PanaderiaLaPaola-APK** → `app-debug.apk` e instálalo en el celular.

## Funciones
- **Configuración**: busca automáticamente impresoras térmicas en la red Wi-Fi (menú desplegable), también permite escribir la IP; tamaño de papel 80 mm / 57 mm; Guardar cambios e *Imprimir prueba*.
- **Base de Datos**: agregar / buscar / eliminar productos con valor en COP.
- **Crear Factura**: datos del cliente (autocompleta clientes anteriores) + búsqueda de productos de la base de datos + cantidades.
- **Factura por Voz**: dicta "banano maduro valor 600 pesos", "4 panes de piña valor 2.500 pesos"… y luego *Finalizar Factura*. El valor dictado es **por unidad** (4 x 2.500 = 10.000). También acepta "dos mil quinientos". Tiene campo de texto por si no se entiende la voz.
- **Vista previa** → *Imprimir y Guardar* o *Solo Guardar*.
- **Facturas Generadas**: buscar por cliente/fecha/número, ver, reimprimir y eliminar.

## Notas
- El celular y la impresora deben estar en la misma red. La impresora debe aceptar impresión RAW por el puerto 9100 (Epson TM-T20III, Xprinter XP-80C, Star TSP143 LAN, etc.).
- Si salen símbolos raros en vez de tildes/ñ, activa "Imprimir sin tildes" en Configuración.
- El dictado usa el reconocimiento de voz de Google (necesita internet) en español de Colombia.
