package com.jannikklein47.batterysync

import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*

class BLEBatteryReader(private val context: Context) {

    companion object {
        private val UUID_BATTERY_SERVICE =
            UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
        private val UUID_BATTERY_LEVEL =
            UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    /**
     * Reads battery levels from all currently connected BLE devices.
     * @return List of Pairs: (device name, battery level [0-100 or -1 if unreadable])
     */
    suspend fun readAllConnectedBatteryLevels(): List<Pair<String, Double>> =
        coroutineScope {

            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    context,
                    "Bluetooth permission not granted. Please enable it in settings.",
                    Toast.LENGTH_LONG
                ).show()
                return@coroutineScope emptyList()
            }

            Log.d("BLE SERVICE", "Get devices:")
            val devices = getConnectedBleDevices()
            for (dev in devices) {
                Log.d("BLE SERVICE", "${dev.name}")
            }
            if (devices.isEmpty()) return@coroutineScope emptyList()

            devices.map { device ->
                async(Dispatchers.IO) { device.name.orUnknown() to readBatteryLevel(device) }
            }.awaitAll()
        }

    // Helper — get all connected GATT (BLE) devices
    private fun getConnectedBleDevices(): List<BluetoothDevice> {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                "Bluetooth permission not granted. Please enable it in settings.",
                Toast.LENGTH_LONG
            ).show()
            Log.d("BLE SERVICE", "NO PERMISSION")
            return emptyList()
        }
        Log.d("BLE SERVICE", "${bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)}")
        return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            .filter { it.type == BluetoothDevice.DEVICE_TYPE_LE }
    }

    // Reads one device’s battery level or returns -1.0 if unavailable
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun readBatteryLevel(device: BluetoothDevice): Double = suspendCancellableCoroutine { cont ->
        try {
            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(
                            context,
                            "Bluetooth permission not granted. Please enable it in settings.",
                            Toast.LENGTH_LONG
                        ).show()
                        cont.resume(-1.0) {}
                    }
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        g.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (!cont.isCompleted) cont.resume(-1.0) {}
                        g.close()
                    }
                }

                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    val service = g.getService(UUID_BATTERY_SERVICE)
                    val characteristic = service?.getCharacteristic(UUID_BATTERY_LEVEL)

                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(
                            context,
                            "Bluetooth permission not granted. Please enable it in settings.",
                            Toast.LENGTH_LONG
                        ).show()
                        cont.resume(-1.0) {}
                        return
                    }

                    if (characteristic == null) {
                        g.disconnect()
                        if (!cont.isCompleted) cont.resume(-1.0) {}
                        return
                    }
                    g.readCharacteristic(characteristic)
                }

                override fun onCharacteristicRead(
                    g: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(
                            context,
                            "Bluetooth permission not granted. Please enable it in settings.",
                            Toast.LENGTH_LONG
                        ).show()
                        cont.resume(-1.0) {}
                    }
                    if (characteristic.uuid == UUID_BATTERY_LEVEL && status == BluetoothGatt.GATT_SUCCESS) {
                        val level = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                        Log.d("BleBatteryReader", "${device.name} battery = $level%")
                        if (!cont.isCompleted) cont.resume(level.toDouble()) {}
                    } else {
                        if (!cont.isCompleted) cont.resume(-1.0) {}
                    }
                    g.disconnect()
                }
            }

            val gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

            // Auto-timeout after 8 seconds to prevent hanging connections
            CoroutineScope(Dispatchers.IO).launch {
                delay(8000)
                if (!cont.isCompleted) {
                    gatt.disconnect()
                    cont.resume(-1.0) {}
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            if (!cont.isCompleted) cont.resume(-1.0) {}
        }
    }

    private fun String?.orUnknown(): String = this ?: "Unknown Device"
}
