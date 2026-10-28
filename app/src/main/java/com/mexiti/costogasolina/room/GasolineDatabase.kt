package com.mexiti.costogasolina.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mexiti.costogasolina.model.GasolineTransaction

@Database(
    entities = [GasolineTransaction::class],
    version = 3,
    exportSchema = false
)
abstract class GasolineDatabase : RoomDatabase() {

    abstract fun gasolineDao(): GasolineDao

    companion object {

        @Volatile
        private var INSTANCE: GasolineDatabase? = null

        // Conserva la migración anterior: agrega el indicador de propina.
        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE gasoline_transactions
                    ADD COLUMN includesTip INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE gasoline_transactions
                    SET includesTip = CASE
                        WHEN tip > 0 THEN 1
                        ELSE 0
                    END
                    """.trimIndent()
                )
            }
        }

        // Agrega el comprobante sin eliminar los registros existentes.
        private val MIGRATION_2_3 = object : Migration(2, 3) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE gasoline_transactions
                    ADD COLUMN receiptImage BLOB
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): GasolineDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GasolineDatabase::class.java,
                    "gasoline_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}