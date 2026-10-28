package com.mexiti.costogasolina.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mexiti.costogasolina.R
import com.mexiti.costogasolina.domain.calcularMonto
import com.mexiti.costogasolina.model.GasolineTransaction
import com.mexiti.costogasolina.ui.components.AddOptionalData
import com.mexiti.costogasolina.ui.components.EditNumberField
import com.mexiti.costogasolina.ui.components.MainTitle
import com.mexiti.costogasolina.viewmodel.GasolineViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditView(
    transactionId: Int,
    navController: NavController,
    viewModel: GasolineViewModel,
    modifier: Modifier = Modifier
) {
    var transaction by remember(transactionId) {
        mutableStateOf<GasolineTransaction?>(null)
    }

    var isLoading by remember(transactionId) {
        mutableStateOf(true)
    }

    // Recupera de Room el registro seleccionado por su ID.
    LaunchedEffect(transactionId) {
        transaction = viewModel.getTransactionById(transactionId)
        isLoading = false
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MainTitle(title = "Editar Registro")
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->

        if (isLoading) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (transaction != null) {

            EditContent(
                transaction = transaction!!,
                onUpdateTransaction = { updatedTransaction ->
                    viewModel.updateTransaction(updatedTransaction)

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Registro actualizado correctamente"
                        )
                        delay(800)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )

        } else {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Registro no encontrado")
            }
        }
    }
}

@Composable
fun EditContent(
    transaction: GasolineTransaction,
    onUpdateTransaction: (GasolineTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Los campos comienzan con los valores recuperados de Room.
    var precioLitroEntrada by remember(transaction.id) {
        mutableStateOf(transaction.pricePerLiter.toString())
    }

    var cantLitrosEntrada by remember(transaction.id) {
        mutableStateOf(transaction.liters.toString())
    }

    var propinaEntrada by remember(transaction.id) {
        mutableStateOf(
            if (transaction.includesTip) {
                transaction.tip.toString()
            } else {
                ""
            }
        )
    }

    var darPropina by remember(transaction.id) {
        mutableStateOf(transaction.includesTip)
    }

    var guardarKilometros by remember(transaction.id) {
        mutableStateOf(transaction.kilometers > 0)
    }

    var kilometrosEntrada by remember(transaction.id) {
        mutableStateOf(
            if (transaction.kilometers > 0) {
                transaction.kilometers.toString()
            } else {
                ""
            }
        )
    }

    // Contiene los bytes de la imagen recuperada o seleccionada.
    var receiptImage by remember(transaction.id) {
        mutableStateOf(transaction.receiptImage)
    }

    var isLoadingReceipt by remember(transaction.id) {
        mutableStateOf(false)
    }

    var receiptError by remember(transaction.id) {
        mutableStateOf<String?>(null)
    }

    // Abre el selector de imágenes de Android.
    val receiptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoadingReceipt = true
                receiptError = null

                try {
                    val imageBytes = withContext(Dispatchers.IO) {
                        readReceiptImage(context, uri)
                    }

                    receiptImage = imageBytes
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    receiptError =
                        "No se pudo cargar la imagen. " +
                                "Prueba con un archivo JPG o PNG más pequeño."
                } finally {
                    isLoadingReceipt = false
                }
            }
        }
    }

    // Convierte los bytes en una imagen para mostrarla.
    val receiptPreview = remember(receiptImage) {
        receiptImage?.let { bytes ->
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size
            )?.asImageBitmap()
        }
    }

    val precioLitro = precioLitroEntrada.toDoubleOrNull() ?: 0.0
    val cantLitros = cantLitrosEntrada.toDoubleOrNull() ?: 0.0

    val propina = if (darPropina) {
        propinaEntrada.toDoubleOrNull() ?: 0.0
    } else {
        0.0
    }

    val kilometraje = if (guardarKilometros) {
        kilometrosEntrada.toDoubleOrNull() ?: 0.0
    } else {
        0.0
    }

    val total = calcularMonto(
        precio = precioLitro,
        cantLitros = cantLitros,
        darPropina = darPropina,
        propina = propina
    )

    val isFormValid = precioLitro > 0.0 && cantLitros > 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registro #${transaction.id}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Fecha: ${transaction.date}",
            style = MaterialTheme.typography.bodyMedium
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditNumberField(
                    label = R.string.ingresa_gasolina,
                    leadingIcon = R.drawable.money_gas,
                    keyboardsOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    value = precioLitroEntrada,
                    onValueChanged = { precioLitroEntrada = it }
                )

                EditNumberField(
                    label = R.string.litros,
                    leadingIcon = R.drawable.gasolina,
                    keyboardsOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    value = cantLitrosEntrada,
                    onValueChanged = { cantLitrosEntrada = it }
                )

                AddOptionalData(
                    flagData = darPropina,
                    onflagCheckedChange = { darPropina = it }
                )

                AnimatedVisibility(
                    visible = darPropina,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    EditNumberField(
                        label = R.string.propina,
                        leadingIcon = R.drawable.outline_18_up_rating_24,
                        keyboardsOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        value = propinaEntrada,
                        onValueChanged = { propinaEntrada = it }
                    )
                }

                AddOptionalData(
                    flagData = guardarKilometros,
                    onflagCheckedChange = { guardarKilometros = it },
                    question = stringResource(R.string.guardar_kilometraje)
                )

                AnimatedVisibility(
                    visible = guardarKilometros,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    EditNumberField(
                        label = R.string.kilometros,
                        leadingIcon = R.drawable.car__kilometer,
                        keyboardsOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        value = kilometrosEntrada,
                        onValueChanged = { kilometrosEntrada = it }
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Comprobante",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (receiptPreview != null) {
                    Image(
                        bitmap = receiptPreview,
                        contentDescription =
                            "Comprobante del registro ${transaction.id}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(text = "Este registro no tiene una imagen.")
                }

                if (isLoadingReceipt) {
                    Text(text = "Preparando imagen...")
                }

                receiptError?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        receiptLauncher.launch("image/*")
                    },
                    enabled = !isLoadingReceipt,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (receiptImage == null) {
                            "Seleccionar imagen"
                        } else {
                            "Cambiar imagen"
                        }
                    )
                }

                Text(
                    text = "Pulsa Guardar Cambios para guardar el comprobante.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.monto_total, total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = {
                // Conserva el ID y la fecha; incluye los bytes de la imagen.
                val updatedTransaction = transaction.copy(
                    pricePerLiter = precioLitro,
                    liters = cantLitros,
                    tip = propina,
                    kilometers = kilometraje,
                    includesTip = darPropina,
                    receiptImage = receiptImage
                )

                onUpdateTransaction(updatedTransaction)
            },
            enabled = isFormValid && !isLoadingReceipt,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Guardar Cambios")
        }
    }
}

// Lee y comprime la imagen antes de guardarla como ByteArray.
private fun readReceiptImage(
    context: Context,
    uri: Uri
): ByteArray {
    val resolver = context.contentResolver

    // Primero obtiene las dimensiones sin cargar la imagen completa.
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    val boundsStream = resolver.openInputStream(uri)
        ?: throw IOException("No se pudo abrir la imagen.")

    boundsStream.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }

    if (options.outWidth <= 0 || options.outHeight <= 0) {
        throw IOException("El archivo no contiene una imagen compatible.")
    }

    // Reduce la resolución para limitar el consumo de memoria.
    var sampleSize = 1

    while (
        options.outWidth / sampleSize > 1024 ||
        options.outHeight / sampleSize > 1024
    ) {
        sampleSize *= 2
    }

    options.inJustDecodeBounds = false
    options.inSampleSize = sampleSize

    val imageStream = resolver.openInputStream(uri)
        ?: throw IOException("No se pudo leer la imagen.")

    val bitmap = imageStream.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    } ?: throw IOException("No se pudo convertir la imagen.")

    try {
        // El comprobante comprimido debe ocupar como máximo 256 KiB.
        val maxBytes = 256 * 1024

        for (quality in 85 downTo 25 step 15) {
            val bytes = ByteArrayOutputStream().use { output ->
                val compressed = bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    quality,
                    output
                )

                if (!compressed) {
                    throw IOException("No se pudo comprimir la imagen.")
                }

                output.toByteArray()
            }

            if (bytes.size <= maxBytes) {
                return bytes
            }
        }

        throw IOException("La imagen es demasiado grande.")
    } finally {
        bitmap.recycle()
    }
}

