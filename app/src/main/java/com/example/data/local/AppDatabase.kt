package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.GymSettingDao
import com.example.data.local.dao.InvoiceDao
import com.example.data.local.dao.MembershipDao
import com.example.data.local.dao.MembershipPlanDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.PaymentDao
import com.example.data.local.entity.AttendanceRecord
import com.example.data.local.entity.Customer
import com.example.data.local.entity.Expense
import com.example.data.local.entity.GymSetting
import com.example.data.local.entity.Invoice
import com.example.data.local.entity.Membership
import com.example.data.local.entity.MembershipPlan
import com.example.data.local.entity.NotificationItem
import com.example.data.local.entity.Payment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Customer::class,
        MembershipPlan::class,
        Membership::class,
        Payment::class,
        Invoice::class,
        Expense::class,
        GymSetting::class,
        NotificationItem::class,
        AttendanceRecord::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun membershipPlanDao(): MembershipPlanDao
    abstract fun membershipDao(): MembershipDao
    abstract fun paymentDao(): PaymentDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun gymSettingDao(): GymSettingDao
    abstract fun notificationDao(): NotificationDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gym_settings ADD COLUMN requireOtpForMemberCreation INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_fitness_gym_erp.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                InitialData.populateInitialData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
