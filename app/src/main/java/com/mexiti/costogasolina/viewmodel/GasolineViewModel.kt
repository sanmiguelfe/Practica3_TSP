package com.mexiti.costogasolina.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mexiti.costogasolina.model.GasolineTransaction
import com.mexiti.costogasolina.room.GasolineDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GasolineViewModel(
    private val gasolineDao: GasolineDao
) : ViewModel() {

    // Observa los registros y actualiza el historial cuando cambian.
    val transaction: StateFlow<List<GasolineTransaction>> =
        gasolineDao.getAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // Crea y guarda un registro con los datos recibidos.
    fun addTransaction(
        price: Double,
        liters: Double,
        tip: Double,
        date: String,
        kilometers: Double,
        includesTip: Boolean = tip > 0.0
    ) {
        viewModelScope.launch {

            val newTransaction = GasolineTransaction(
                pricePerLiter = price,
                liters = liters,
                tip = tip,
                date = date,
                kilometers = kilometers,
                includesTip = includesTip
            )

            gasolineDao.insertTransaction(newTransaction)
        }
    }

    // Consulta un registro específico por su identificador.
    suspend fun getTransactionById(id: Int): GasolineTransaction? {
        return gasolineDao.getTransactionById(id)
    }

    // Guarda los cambios de un registro existente.
    fun updateTransaction(transaction: GasolineTransaction) {
        viewModelScope.launch {
            gasolineDao.updateTransaction(transaction)
        }
    }

    // Elimina el registro seleccionado.
    fun deleteTransaction(transaction: GasolineTransaction) {
        viewModelScope.launch {
            gasolineDao.deleteTransaction(transaction)
        }
    }
}