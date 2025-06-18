package com.AuroraByteSoftware.AuroraDMX

import android.app.Activity
import android.util.Log
import com.AuroraByteSoftware.AuroraDMX.network.SendArtnetUpdate
import com.AuroraByteSoftware.AuroraDMX.network.SendSacnUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramSocket
import java.net.SocketException

/**
 * Network management
 * Created by furtchet on 5/24/17.
 * Kotlinized by damianogiusti on 6/18/25.
 */
object AuroraNetwork : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    //Network timers
    private var ArtNet: Job? = null
    private var SACN: Job? = null
    private var SACNUnicast: Job? = null

    private var clientSocket: DatagramSocket? = null
    private var _artnetSocket: DatagramSocket? = null

    @Throws(SocketException::class)
    @JvmStatic
    fun getArtnetSocket(): DatagramSocket {
        return _artnetSocket?.takeUnless { it.isClosed } ?: run {
            DatagramSocket(ART_NET_PORT)
                .apply { setReuseAddress(true) }
                .also { _artnetSocket = it }
        }
    }

    const val ART_NET_PORT: Int = 6454

    @JvmStatic
    fun setUpNetwork(activity: Activity?) {
        val sharedPref = MainActivity.getSharedPref() ?: return

        val updatePacketIntervalMs = sharedPref.getString("packet_send_interval", null)?.toLongOrNull() ?: 25
        val protocol = sharedPref.getString("select_protocol", null)
        Log.i("AuroraNetwork", "Starting Network $protocol with interval ${updatePacketIntervalMs}ms")
        stopNetwork()

        when (protocol) {
            "SACNUNI" -> {
                SACNUnicast = launch {
                    delay(200)
                    while (isActive) {
                        SendSacnUpdate(activity, clientSocket).run()
                        delay(updatePacketIntervalMs)
                    }
                }
            }
            "SACN" -> {
                SACN = launch {
                    delay(200)
                    while (isActive) {
                        SendSacnUpdate(activity, clientSocket).run()
                        delay(updatePacketIntervalMs)
                    }
                }
            }
            else -> {
                ArtNet = launch {
                    delay(200)
                    while (isActive) {
                        SendArtnetUpdate(activity, clientSocket).run()
                        delay(updatePacketIntervalMs)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun stopNetwork() {
        ArtNet?.cancel()
        SACN?.cancel()
        SACNUnicast?.cancel()
        clientSocket?.close()
    }
}
