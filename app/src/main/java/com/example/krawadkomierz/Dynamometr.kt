package com.example.krawadkomierz

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

const val MANUFACTURER_ID = 0x0100   // nr producenta
class Dynamometer(private val context: Context) {
    var latestKg = 0f
        private set

    private var lastPacketTime = 0L    //ostatnia paczka ms

    private var scanner: BluetoothLeScanner? = null


    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val data = result.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID) ?: return
            if (data.size < 12) return
            // bajty 10 i 11, w setnych kg
            val raw = ((data[10].toInt() and 0xFF) shl 8) or (data[11].toInt() and 0xFF)
            latestKg = raw / 100f
            lastPacketTime = System.currentTimeMillis()
        }
    }
    fun isActive(): Boolean = System.currentTimeMillis() - lastPacketTime < 1000

    fun start() {
        val bluetoothScanner = context.getSystemService(BluetoothManager::class.java)
            ?.adapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setManufacturerData(MANUFACTURER_ID, ByteArray(0))   //tylko z 0x0100
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)      // najczesciej
            .build()
        try {
            bluetoothScanner.startScan(listOf(filter), settings, callback)
            scanner = bluetoothScanner
        } catch (e: SecurityException) {
            // brak zgody
        }
    }
    fun stop() {
        try {
            scanner?.stopScan(callback)
        } catch (e: SecurityException) {
        } catch (e: IllegalStateException) {
            // wylaczony
        }
        scanner = null
    }
}

fun bluetoothPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_SCAN
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }

@Composable
fun rememberDynamometer(): Dynamometer {
    val context = LocalContext.current
    val dynamometer = remember { Dynamometer(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) dynamometer.start() }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(bluetoothPermission())
    }

    DisposableEffect(Unit) {
        onDispose { dynamometer.stop() }
    }

    return dynamometer
}

@Composable
fun DynamometerStatus(dynamometer: Dynamometer) {
    val connected = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            connected.value = dynamometer.isActive()
            delay(500)
        }
    }
    Text(if (connected.value) "Siłomierz: WH-C06" else "Brak siłomierza")
}