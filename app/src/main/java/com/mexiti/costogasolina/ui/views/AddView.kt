package com.mexiti.costogasolina.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mexiti.costogasolina.R
import com.mexiti.costogasolina.domain.calcularMonto
import com.mexiti.costogasolina.ui.components.AddOptionalData
import com.mexiti.costogasolina.ui.components.EditNumberField
import com.mexiti.costogasolina.ui.components.MainTitle
import com.mexiti.costogasolina.ui.theme.CostoGasolinaTheme
import com.mexiti.costogasolina.viewmodel.GasolineViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddView(
    navController: NavController,
    viewModel: GasolineViewModel,
    modifier: Modifier = Modifier
) {
    CostGasLayout(
        navController = navController,
        onSaveTransaction = {
                price, liters, tip, date, kilometers, includesTip ->

            viewModel.addTransaction(
                price = price,
                liters = liters,
                tip = tip,
                date = date,
                kilometers = kilometers,
                includesTip = includesTip
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostGasLayout(
    navController: NavController,
    onSaveTransaction: (
        Double, Double, Double, String, Double, Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MainTitle(
                        title = stringResource(R.string.calcular_monto)
                    )
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

        CostGasContent(
            onSaveTransaction = {
                    price, liters, tip, date, kilometers, includesTip ->

                onSaveTransaction(
                    price,
                    liters,
                    tip,
                    date,
                    kilometers,
                    includesTip
                )

                scope.launch {
                    snackbarHostState.showSnackbar(
                        "✓ Registro guardado correctamente"
                    )
                    delay(800)
                    navController.popBackStack()
                }
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun CostGasContent(
    onSaveTransaction: (
        Double, Double, Double, String, Double, Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var precioLitroEntrada by remember { mutableStateOf("") }
    var cantLitrosEntrada by remember { mutableStateOf("") }
    var propinaEntrada by remember { mutableStateOf("") }
    var darPropina by remember { mutableStateOf(false) }
    var guardarKilometros by remember { mutableStateOf(false) }
    var kilometrsRecorridos by remember { mutableStateOf("") }

    // Convierte las entradas de texto a números.
    val precioLitro = precioLitroEntrada.toDoubleOrNull() ?: 0.0
    val cantLitros = cantLitrosEntrada.toDoubleOrNull() ?: 0.0

    val propina = if (darPropina) {
        propinaEntrada.toDoubleOrNull() ?: 0.0
    } else {
        0.0
    }

    val kilometraje = if (guardarKilometros) {
        kilometrsRecorridos.toDoubleOrNull() ?: 0.0
    } else {
        0.0
    }

    // Reutiliza el cálculo original del proyecto.
    val total = calcularMonto(
        precioLitro,
        cantLitros,
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
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Precio por litro.
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

                // Cantidad de litros.
                EditNumberField(
                    label = R.string.litros,
                    leadingIcon = R.drawable.gasolina,
                    keyboardsOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = if (darPropina) {
                            ImeAction.Next
                        } else {
                            ImeAction.Done
                        }
                    ),
                    value = cantLitrosEntrada,
                    onValueChanged = { cantLitrosEntrada = it }
                )

                // El estado de este interruptor se guardará en includesTip.
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

                // Interruptor para registrar kilometraje.
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
                        value = kilometrsRecorridos,
                        onValueChanged = { kilometrsRecorridos = it }
                    )
                }
            }
        }

        // Muestra el total calculado.
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
                val currentDate = SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(Date())

                // Envía los datos y el estado del interruptor de propina.
                onSaveTransaction(
                    precioLitro,
                    cantLitros,
                    propina,
                    currentDate,
                    kilometraje,
                    darPropina
                )
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Registro")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CostGasLayoutPreview() {
    CostoGasolinaTheme {
        CostGasContent(
            onSaveTransaction = { _, _, _, _, _, _ -> }
        )
    }
}