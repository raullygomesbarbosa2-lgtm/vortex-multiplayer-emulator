package com.bytevortex.vortexonline

import android.content.Intent
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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ShaderConfig
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var root: FrameLayout
    private var retroView: GLRetroView? = null
    private lateinit var status: TextView

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
            text = "MULTIPLAYER À DISTÂNCIA: EM DESENVOLVIMENTO"
            setTextColor(Color.rgb(255, 213, 120)); textSize = 13f; gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, dp(5))
        }
        val disclosure = TextView(this).apply {
            text = "Esta primeira etapa é o motor nativo local. A sala online e a sincronização entre dois celulares ainda precisam ser implementadas e testadas. Não inclui ROMs nem BIOS."
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
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) { }
        Thread {
            try {
                val target = File(cacheDir, "game.nes")
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Não consegui ler o arquivo." }
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                runOnUiThread { startNativeGame(target) }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Falha ao abrir a ROM: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
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
        val game = GLRetroView(this, viewData)
        retroView = game
        lifecycle.addObserver(game)
        root.addView(game, FrameLayout.LayoutParams(-1, -1))
        addOverlayControls()
    }

    private fun addOverlayControls() {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(6), dp(10), dp(6)); setBackgroundColor(0x88040b07.toInt()) }
        val roomLabel = TextView(this).apply { text = "NES nativo • 2 jogadores online ainda não ativo"; setTextColor(Color.WHITE); textSize = 12f }
        val close = TextView(this).apply { text = "ROMs"; setTextColor(Color.rgb(112,255,155)); gravity = Gravity.CENTER; setPadding(dp(14),dp(8),dp(8),dp(8)); setOnClickListener { closeGame() } }
        bar.addView(roomLabel, LinearLayout.LayoutParams(0, dp(42), 1f)); bar.addView(close)
        root.addView(bar, FrameLayout.LayoutParams(-1, dp(48), Gravity.TOP))

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
                retroView?.sendKeyEvent(action, key, 0)
                true
            }
        }
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.rgb(4, 20, 11))
        setBackgroundColor(Color.rgb(112, 255, 155))
        setOnClickListener { onClick() }
    }

    private fun closeGame() {
        retroView?.let { lifecycle.removeObserver(it) }
        retroView = null
        showHome()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        retroView?.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0)
        return super.onKeyDown(keyCode, event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        retroView?.sendKeyEvent(KeyEvent.ACTION_UP, keyCode, 0)
        return super.onKeyUp(keyCode, event)
    }
    override fun onDestroy() { retroView?.let { lifecycle.removeObserver(it) }; super.onDestroy() }
}
