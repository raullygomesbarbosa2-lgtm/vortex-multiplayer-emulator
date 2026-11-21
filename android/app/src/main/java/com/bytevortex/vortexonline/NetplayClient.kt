package com.bytevortex.vortexonline

import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/** Room and controller-event relay. This relays inputs; it does not yet implement frame lockstep/rollback. */
class NetplayClient(
    private val listener: Listener,
    private val endpoint: String = "https://vortex-online-netplay.onrender.com"
) {
    interface Listener {
        fun onStatus(text: String)
        fun onRoomPlayers(count: Int)
        fun onRemoteInput(action: Int, keyCode: Int, sequence: Long)
        fun onRoomError(text: String)
    }

    private var socket: Socket? = null
    private var roomCode: String = ""
    private var romId: String = ""
    private var playerId: String = UUID.randomUUID().toString()
    private var host = false
    private val sequence = AtomicLong(0)

    @Synchronized fun open(room: String, gameId: String) = connect(room, gameId, true)
    @Synchronized fun join(room: String, gameId: String) = connect(room, gameId, false)

    @Synchronized private fun connect(room: String, gameId: String, create: Boolean) {
        disconnect()
        roomCode = room.trim().uppercase()
        romId = gameId
        host = create
        if (roomCode.length !in 4..12) { listener.onRoomError("O código da sala deve ter de 4 a 12 caracteres."); return }
        listener.onStatus("Conectando ao relay…")
        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                timeout = 12000
            }
            val s = IO.socket(endpoint, options)
            socket = s
            s.on(Socket.EVENT_CONNECT) {
                listener.onStatus("Conectado; ${if (create) "criando" else "entrando na"} sala $roomCode…")
                val extra = JSONObject()
                    .put("sessionid", roomCode)
                    .put("userid", playerId)
                    .put("playerId", playerId)
                    .put("player_name", if (create) "Host" else "Convidado")
                    .put("room_name", "Vortex NES $roomCode")
                    .put("game_id", romId)
                    .put("domain", "vortex-native")
                val data = JSONObject().put("extra", extra)
                val event = if (create) "open-room" else "join-room"
                if (create) data.put("maxPlayers", 2)
                s.emit(event, data, Ack { args ->
                    val first = args.firstOrNull()
                    if (first != null && first != JSONObject.NULL) {
                        listener.onRoomError(first.toString())
                    } else {
                        listener.onStatus("Sala $roomCode aberta. Aguardando o outro jogador…")
                    }
                })
            }
            s.on(Socket.EVENT_CONNECT_ERROR) { args ->
                listener.onRoomError("Falha de conexão com o relay: ${args.firstOrNull() ?: "sem detalhe"}")
            }
            s.on(Socket.EVENT_DISCONNECT) { listener.onStatus("Conexão com a sala interrompida.") }
            s.on("users-updated") { args ->
                val players = args.firstOrNull() as? JSONObject
                val count = players?.length() ?: 0
                listener.onRoomPlayers(count)
                listener.onStatus(if (count >= 2) "Dois jogadores na sala; controles podem ser retransmitidos." else "Sala $roomCode aberta. Aguardando o outro jogador…")
            }
            s.on("input") { args ->
                try {
                    val packet = args.firstOrNull() as? JSONObject ?: return@on
                    if (packet.optString("romId") != romId || packet.optString("playerId") == playerId) return@on
                    listener.onRemoteInput(packet.optInt("action"), packet.optInt("keyCode"), packet.optLong("sequence"))
                } catch (e: Exception) { Log.w("VortexNetplay", "Invalid input event", e) }
            }
            s.connect()
        } catch (e: Exception) {
            listener.onRoomError("Não consegui iniciar o multiplayer: ${e.message}")
        }
    }

    fun sendInput(action: Int, keyCode: Int) {
        val s = socket ?: return
        if (!s.connected()) return
        val packet = JSONObject()
            .put("playerId", playerId)
            .put("romId", romId)
            .put("action", action)
            .put("keyCode", keyCode)
            .put("sequence", sequence.incrementAndGet())
            .put("sentAt", System.currentTimeMillis())
        s.emit("input", packet)
    }

    fun localPort(): Int = if (host) 0 else 1
    fun remotePort(): Int = if (host) 1 else 0
    fun isConnected(): Boolean = socket?.connected() == true

    @Synchronized fun disconnect() {
        socket?.let { it.disconnect(); it.close() }
        socket = null
    }
}
