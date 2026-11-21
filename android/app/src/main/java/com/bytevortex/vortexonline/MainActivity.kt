package com.bytevortex.vortexonline

import android.content.Intent
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var root: FrameLayout
    private var retroView: GLRetroView? = null
    private lateinit var status: TextView
    private var netplay: NetplayClient? = null
    private var roomStatus: TextView? = null
    private var romId: String = ""

    companion object { private const val PICK_ROM = 4012 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(3, 14, 9)
        window.navigationBarColor = Color.rgb(3, 14, 9)
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        showHome()
    }

    private fun showHome() {
        root.removeAllViews()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(26), dp(30), dp(26), dp(30))
            setBackgroundColor(Color.rgb(5, 17, 12))
        }
        val logo = TextView(this).apply {
            text = "VORTEX ONLINE"
            setTextColor(Color.rgb(112, 255, 155))
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val heading = TextView(this).apply {
            text = "Emulação nativa • Nintendo NES"
            setTextColor(Color.WHITE); textSize = 19f; gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(10))
        }
        status = TextView(this).apply {
            text = "Selecione uma ROM NES que você possui. O jogo roda no aparelho, sem WebView."
            setTextColor(Color.LTGRAY); textSize = 14f; gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(18))
        }
        val pick = button("ESCOLHER ROM NES") { pickRom() }
        val multiplayer = TextView(this).apply {
            text = "MULTIPLAYER À DISTÂNCIA: salas e relay de comandos em alpha"
            setTextColor(Color.rgb(255, 213, 120)); textSize = 13f; gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(5))
        }
        val disclosure = TextView(this).apply {
            text = "NES roda nativamente, sem WebView. Crie/entre numa sala para retransmitir comandos. Ainda não há sincronização por quadro nem rollback: pode dessincronizar e não foi validado entre celulares. Não inclui ROMs/BIOS."
            setTextColor(Color.rgb(177, 195, 184)); textSize = 12f; gravity = Gravity.CENTER
        }
        listOf(logo, heading, status, pick, multiplayer, disclosure).forEach { panel.addView(it, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT)) }
        root.addView(panel, FrameLayout.LayoutParams(-1, -1))
    }

    private fun pickRom() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, PICK_ROM)
    }

    @Deprecated("Legacy file picker callback supports Android 6")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_ROM || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        Thread {
            try {
                val target = File(cacheDir, "game.nes")
                contentResolver.openInputStream(uri).use { raw ->
                    requireNotNull(raw) { "Não consegui ler o arquivo escolhido." }
                    BufferedInputStream(raw).use { input ->
                        input.mark(8)
                        val signature = ByteArray(4)
                        val count = input.read(signature)
                        input.reset()
                        if (count >= 2 && signature[0] == 'P'.code.toByte() && signature[1] == 'K'.code.toByte()) {
                            extractNesFromZip(input, target)
                        } else {
                            target.outputStream().use { output -> copyBounded(input, output) }
                        }
                    }
                }
                validateNesRom(target)
                runOnUiThread { startNativeGame(target) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Não consegui abrir essa ROM NES: ${e.message ?: "arquivo inválido"}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun extractNesFromZip(input: BufferedInputStream, target: File) {
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.lowercase().endsWith(".nes")) {
                    target.outputStream().use { output -> copyBounded(zip, output) }
                    return
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        throw IllegalArgumentException("o ZIP não contém um arquivo .nes.")
    }

    private fun copyBounded(input: java.io.InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            total += size
            require(total <= 128L * 1024 * 1024) { "arquivo maior que 128 MB." }
            output.write(buffer, 0, size)
        }
    }

    private fun validateNesRom(rom: File) {
        require(rom.length() >= 16) { "arquivo vazio ou pequeno demais." }
        val header = ByteArray(4)
        rom.inputStream().use { input -> require(input.read(header) == 4) { "cabeçalho incompleto." } }
        require(header.contentEquals(byteArrayOf(0x4e, 0x45, 0x53, 0x1a))) {
            "esperava um .nes válido (ou ZIP contendo um .nes)."
        }
    }

    private fun startNativeGame(rom: File) {
        root.removeAllViews()
        val core = File(applicationInfo.nativeLibraryDir, "libquicknes_libretro_android.so")
        if (!core.exists()) {
            Toast.makeText(this, "Núcleo NES ARM64 não veio neste APK.", Toast.LENGTH_LONG).show()
            showHome(); return
        }
        val viewData = GLRetroViewData(this).apply {
            coreFilePath = core.absolutePath
            gameFilePath = rom.absolutePath
            systemDirectory = File(filesDir, "system").apply { mkdirs() }.absolutePath
            savesDirectory = File(filesDir, "saves").apply { mkdirs() }.absolutePath
            shader = ShaderConfig.Default
        }
        romId = try { MessageDigest.getInstance("SHA-256").digest(rom.readBytes()).take(8).joinToString("") { "%02x".format(it) } } catch (_: Exception) { "unknown" }
        val game = GLRetroView(this, viewData)
        retroView = game
        lifecycle.addObserver(game)
        root.addView(game, FrameLayout.LayoutParams(-1, -1))
        addOverlayControls()
    }

    private fun addOverlayControls() {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(6), dp(4), dp(6), dp(4)); setBackgroundColor(0x99040b07.toInt()) }
        roomStatus = TextView(this).apply { text = "NES local • ROM ${romId.take(8)}"; setTextColor(Color.WHITE); textSize = 10f; maxLines = 2 }
        val create = miniButton("CRIAR") { openRoomDialog(true) }
        val join = miniButton("ENTRAR") { openRoomDialog(false) }
        val close = miniButton("VOLTAR") { closeGame() }
        bar.addView(roomStatus, LinearLayout.LayoutParams(0, dp(42), 1f)); bar.addView(create); bar.addView(join); bar.addView(close)
        root.addView(bar, FrameLayout.LayoutParams(-1, dp(50), Gravity.TOP))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(16)) }
        val dpad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val middle = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        dpad.addView(controlButton("↑", KeyEvent.KEYCODE_DPAD_UP), LinearLayout.LayoutParams(dp(56), dp(54)))
        middle.addView(controlButton("←", KeyEvent.KEYCODE_DPAD_LEFT), LinearLayout.LayoutParams(dp(56), dp(54)))
        middle.addView(View(this), LinearLayout.LayoutParams(dp(48), dp(54)))
        middle.addView(controlButton("→", KeyEvent.KEYCODE_DPAD_RIGHT), LinearLayout.LayoutParams(dp(56), dp(54)))
        dpad.addView(middle)
        dpad.addView(controlButton("↓", KeyEvent.KEYCODE_DPAD_DOWN), LinearLayout.LayoutParams(dp(56), dp(54)))
        controls.addView(dpad, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val face = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        face.addView(controlButton("B", KeyEvent.KEYCODE_BUTTON_B), LinearLayout.LayoutParams(dp(60), dp(60)))
        face.addView(View(this), LinearLayout.LayoutParams(dp(12), dp(60)))
        face.addView(controlButton("A", KeyEvent.KEYCODE_BUTTON_A), LinearLayout.LayoutParams(dp(60), dp(60)))
        controls.addView(face)

        val lower = FrameLayout(this).apply { addView(controls, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)) }
        root.addView(lower, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
    }

    private fun controlButton(label: String, key: Int): TextView {
        return TextView(this).apply {
            text = label; gravity = Gravity.CENTER; textSize = 21f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(16).toFloat(); setColor(0x990d2618.toInt()); setStroke(dp(1), 0x9970ff9b.toInt()) }
            setOnTouchListener { _, event ->
                val action = when (event.actionMasked) { MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> KeyEvent.ACTION_DOWN; MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> KeyEvent.ACTION_UP; else -> return@setOnTouchListener true }
                dispatchControl(action, key)
                true
            }
        }
    }

    private fun miniButton(text: String, onClick: () -> Unit) = TextView(this).apply {
        this.text = text; gravity = Gravity.CENTER; textSize = 10f; setTextColor(Color.rgb(112, 255, 155))
        setPadding(dp(7), dp(8), dp(7), dp(8)); setOnClickListener { onClick() }
    }

    private fun dispatchControl(action: Int, keyCode: Int) {
        val client = netplay
        val localPort = if (client?.isConnected() == true) client.localPort() else 0
        retroView?.sendKeyEvent(action, keyCode, localPort)
        client?.sendInput(action, keyCode)
    }

    private fun openRoomDialog(create: Boolean) {
        if (romId.isBlank() || romId == "unknown") {
            Toast.makeText(this, "Não consegui identificar a ROM; escolha o arquivo novamente.", Toast.LENGTH_LONG).show(); return
        }
        if (create) {
            val code = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
            connectRoom(code, true)
            AlertDialog.Builder(this).setTitle("Código da sala")
                .setMessage("Compartilhe este código com a outra pessoa: $code\n\nOs dois precisam abrir a mesma ROM NES. A retransmissão de controles é experimental; ainda pode haver dessincronização.")
                .setPositiveButton("OK", null).show()
        } else {
            val field = EditText(this).apply { hint = "Código da sala"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS }
            AlertDialog.Builder(this).setTitle("Entrar na sala").setMessage("Digite o código que o outro jogador compartilhou.")
                .setView(field).setNegativeButton("Cancelar", null)
                .setPositiveButton("Entrar") { _, _ -> connectRoom(field.text.toString(), false) }.show()
        }
    }

    private fun connectRoom(code: String, create: Boolean) {
        netplay?.disconnect()
        val client = NetplayClient(object : NetplayClient.Listener {
            override fun onStatus(text: String) = runOnUiThread {
                roomStatus?.text = text
            }
            override fun onRoomPlayers(count: Int) = runOnUiThread {
                if (count >= 2) roomStatus?.text = "Sala conectada: 2 jogadores. Comandos retransmitidos; sincronização ainda experimental."
            }
            override fun onRemoteInput(action: Int, keyCode: Int, sequence: Long) = runOnUiThread {
                retroView?.sendKeyEvent(action, keyCode, netplay?.remotePort() ?: 0)
            }
            override fun onRoomError(text: String) = runOnUiThread {
                roomStatus?.text = "Falha na sala"
                Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
            }
        })
        netplay = client
        if (create) client.open(code, romId) else client.join(code, romId)
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.rgb(4, 20, 11))
        setBackgroundColor(Color.rgb(112, 255, 155))
        setOnClickListener { onClick() }
    }

    private fun closeGame() {
        netplay?.disconnect()
        netplay = null
        roomStatus = null
        retroView?.let { lifecycle.removeObserver(it) }
        retroView = null
        showHome()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        dispatchControl(KeyEvent.ACTION_DOWN, keyCode)
        return super.onKeyDown(keyCode, event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        dispatchControl(KeyEvent.ACTION_UP, keyCode)
        return super.onKeyUp(keyCode, event)
    }
    override fun onDestroy() { netplay?.disconnect(); retroView?.let { lifecycle.removeObserver(it) }; super.onDestroy() }
}
