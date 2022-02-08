package dev.thunderramgee.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import dev.thunderramgee.bluetooth.databinding.ActivityMainBinding

class MainActivity : BluetoothLeUart.Callback, AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Bluetooth LE UART instance.  This is defined in BluetoothLeUart.java.
    private lateinit var uart: BluetoothLeUart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        init()
    }

    private fun init() {
        // Initialize UART.
        uart = BluetoothLeUart(applicationContext)

        binding.run {
            send.setOnClickListener { sendClick() }
            messages.movementMethod = ScrollingMovementMethod()
        }
        enableSendBtn(false)
    }

    // Write some text to the messages text view.
    // Care is taken to do this on the main UI thread so writeLine can be called from any thread
    // (like the BTLE callback).
    private fun writeLine(text: CharSequence) {
        runOnUiThread {
            binding.messages.append("${text}\n")
        }
    }

    // Send Button enable / disable by "enabled" value
    private fun enableSendBtn(enabled: Boolean) {
        binding.send.run {
            isClickable = enabled
            isEnabled = enabled
        }
    }

    // Handler for mouse click on the send button.
    private fun sendClick() {
        val stringBuilder = StringBuilder()
        val message: String = binding.input.text.toString()

        // We can only send 20 bytes per packet, so break longer messages
        // up into 20 byte payloads
        var len = message.length
        var pos = 0
        while (len != 0) {
            stringBuilder.setLength(0)
            if (len >= 20) {
                stringBuilder.append(message.toCharArray(), pos, 20)
                len -= 20
                pos += 20
            } else {
                stringBuilder.append(message.toCharArray(), pos, len)
                len = 0
            }
            uart.send(stringBuilder.toString())
        }
        // Terminate with a newline character if requests
        // Terminate with a newline character if requests
        if (binding.newline.isChecked) {
            stringBuilder.setLength(0)
            stringBuilder.append("\n")
            uart.send(stringBuilder.toString())
        }
    }

    // region lifecycle
    override fun onResume() {
        super.onResume()
        writeLine("Scanning for devices ...")
        uart.run {
            registerCallback(this@MainActivity)
            connectFirstAvailable()
        }
    }

    override fun onStop() {
        super.onStop()
        uart.run {
            unregisterCallback(this@MainActivity)
            disconnect()
        }
    }

    //endregion

    //region UART Callback event handlers.
    override fun onConnected(uart: BluetoothLeUart?) {
        // Called when UART device is connected and ready to send/receive data.
        writeLine("Connected!")
        runOnUiThread {
            enableSendBtn(true)
        }
    }

    override fun onConnectFailed(uart: BluetoothLeUart?) {
        // Called when some error occured which prevented UART connection from completing.
        writeLine("Error connecting to device!")
        runOnUiThread {
            enableSendBtn(false)
        }
    }

    override fun onDisconnected(uart: BluetoothLeUart?) {
        // Called when the UART device disconnected.
        writeLine("Disconnected!")
        // Disable the send button.
        runOnUiThread {
            enableSendBtn(false)
        }
    }

    override fun onReceive(uart: BluetoothLeUart?, rx: BluetoothGattCharacteristic?) {
        // Called when data is received by the UART.
        writeLine("Received: " + rx?.getStringValue(0))
    }

    override fun onDeviceFound(device: BluetoothDevice) {
        // Called when a UART device is discovered (after calling startScan).
        writeLine("Found device : " + device.address)
        writeLine("Waiting for a connection ...")
    }

    override fun onDeviceInfoAvailable() {
        writeLine(uart.deviceInfo)
    }
    //endregion
}