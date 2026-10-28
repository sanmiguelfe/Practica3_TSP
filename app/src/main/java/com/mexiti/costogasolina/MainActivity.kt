package com.mexiti.costogasolina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mexiti.costogasolina.navigation.NavManager
import com.mexiti.costogasolina.room.GasolineDatabase
import com.mexiti.costogasolina.ui.theme.CostoGasolinaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtiene la base de datos con la migración configurada.
        val db = GasolineDatabase.getDatabase(applicationContext)

        // Obtiene las operaciones para consultar y modificar registros.
        val dao = db.gasolineDao()

        setContent {
            CostoGasolinaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavManager(dao)
                }
            }
        }
    }
}