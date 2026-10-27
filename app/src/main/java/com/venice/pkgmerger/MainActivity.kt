package com.venice.pkgmerger

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.documentfile.provider.DocumentFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.Random

class AspectRatioVideoView(context: Context) : VideoView(context) {
    private var videoWidth = 0
    private var videoHeight = 0

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(videoWidth, widthMeasureSpec)
        val height = getDefaultSize(videoHeight, heightMeasureSpec)
        
        if (videoWidth > 0 && videoHeight > 0) {
            if (videoWidth * height < width * videoHeight) {
                setMeasuredDimension(width, width * videoHeight / videoWidth)
            } else {
                setMeasuredDimension(height * videoWidth / videoHeight, height)
            }
        } else {
            setMeasuredDimension(width, height)
        }
    }
}

class MainActivity : Activity() {

    private val uris = ArrayList<Uri>()
    private val names = ArrayList<String>()
    private val sizes = ArrayList<Long>()

    private var output: Uri? = null
    private var reversed = false
    private var lastHash: String? = null
    private var appMode = "MERGE"
    private var currentTheme = "CLASSIC"
    private var currentLang = "fr"

    private var bgVideoView: AspectRatioVideoView? = null
    private var musicPlayer: MediaPlayer? = null

    @Volatile private var processing = false
    @Volatile private var cancelled = false
    private val ui = Handler(Looper.getMainLooper())

    private lateinit var prefs: SharedPreferences
    private lateinit var rootFrameLayout: FrameLayout
    private lateinit var rootLayout: ScrollView
    private lateinit var mainCol: LinearLayout
    private lateinit var listContainer: LinearLayout
    private lateinit var checkView: TextView
    private lateinit var outView: TextView
    private lateinit var statusView: TextView
    private lateinit var hashView: TextView
    private lateinit var hashField: EditText
    private lateinit var verifyBox: CheckBox
    private lateinit var deleteBox: CheckBox
    private lateinit var bar: ProgressBar

    private lateinit var pickBtnLayout: View
    private lateinit var dirBtnLayout: View
    private lateinit var outBtnLayout: View
    private lateinit var goBtnLayout: View
    private lateinit var cancelBtnLayout: View
    private lateinit var revBtnLayout: View
    private lateinit var copyBtnLayout: View

    private lateinit var menuBtn: Button
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var section1Title: TextView
    private lateinit var section2Title: TextView
    private lateinit var section3Title: TextView
    private lateinit var pkgIconView: ImageView
    private lateinit var infoLayout: LinearLayout

    private val cardViews = ArrayList<LinearLayout>()

    private data class ThemeConfig(
        val name: String,
        val bg: Int,
        val card: Int,
        val border: String,
        val primaryText: Int,
        val secondaryText: Int,
        val dim: Int,
        val pressColor: String,
        val btnPick: Pair<String, String>,
        val btnDir: Pair<String, String>,
        val btnRev: Pair<String, String>,
        val btnOut: Pair<String, String>,
        val btnGo: Pair<String, String>,
        val btnCancel: Pair<String, String>
    )

    private fun getThemeConfig(key: String): ThemeConfig {
        return when (key) {
            "FORTNITE" -> ThemeConfig(
                name = "Fortnite Battle Royale",
                bg = Color.parseColor("#0C0826"),
                card = Color.parseColor("#85162447"),
                border = "#00D9FF",
                primaryText = Color.parseColor("#FFE600"),
                secondaryText = Color.parseColor("#00D9FF"),
                dim = Color.parseColor("#9DB2F5"),
                pressColor = "#FFE600",
                btnPick = Pair("#1E2D6E", "#00D9FF"),
                btnDir = Pair("#1E2D6E", "#00D9FF"),
                btnRev = Pair("#4B1E6E", "#B800FF"),
                btnOut = Pair("#0F5E6B", "#00FFF0"),
                btnGo = Pair("#198038", "#00FF66"),
                btnCancel = Pair("#801935", "#FF0055")
            )
            "GOW" -> ThemeConfig(
                name = "God of War Ragnarök",
                bg = Color.parseColor("#120A0A"),
                card = Color.parseColor("#80241212"),
                border = "#FF4500",
                primaryText = Color.parseColor("#FFD700"),
                secondaryText = Color.parseColor("#FF4500"),
                dim = Color.parseColor("#B8860B"),
                pressColor = "#FF0000",
                btnPick = Pair("#3D1414", "#FF4500"),
                btnDir = Pair("#3D1414", "#FF4500"),
                btnRev = Pair("#4A3B12", "#FFD700"),
                btnOut = Pair("#291321", "#FF1493"),
                btnGo = Pair("#1C3D14", "#32CD32"),
                btnCancel = Pair("#5E0F0F", "#FF0000")
            )
            else -> ThemeConfig(
                name = "Sombre Classique",
                bg = Color.parseColor("#0F172A"),
                card = Color.parseColor("#1E293B"),
                border = "#334155",
                primaryText = Color.WHITE,
                secondaryText = Color.parseColor("#38BDF8"),
                dim = Color.parseColor("#94A3B8"),
                pressColor = "#38BDF8",
                btnPick = Pair("#0284C7", "#38BDF8"),
                btnDir = Pair("#0369A1", "#38BDF8"),
                btnRev = Pair("#334155", "#64748B"),
                btnOut = Pair("#0D9488", "#14B8A6"),
                btnGo = Pair("#16A34A", "#22C55E"),
                btnCancel = Pair("#DC2626", "#EF4444")
            )
        }
    }

    private fun tr(key: String): String {
        val map = mapOf(
            "app_title" to mapOf("fr" to "⚡ PS PKG FREEDOM HUB"),
            "universe" to mapOf("fr" to "UNIVERS"),
            "menu_btn" to mapOf("fr" to "⚙️ MENU"),
            "sec1" to mapOf("fr" to "1. FICHIERS SOURCE (PKG / PARTIES)"),
            "sec1_split" to mapOf("fr" to "1. FICHIER SOURCE UNIQUE (.PKG)"),
            "btn_pick" to mapOf("fr" to "CHOISIR LES FICHIERS"),
            "btn_dir" to mapOf("fr" to "CHOISIR UN DOSSIER COMPLET"),
            "btn_rev" to mapOf("fr" to "INVERSER L'ORDRE DES PARTIES"),
            "sec2" to mapOf("fr" to "2. SORTIE & TRAITEMENT"),
            "btn_out" to mapOf("fr" to "DÉFINIR EMPLACEMENT DE SORTIE"),
            "out_lbl" to mapOf("fr" to "Sortie : —"),
            "btn_go_merge" to mapOf("fr" to "▶️ LANCER LA FUSION"),
            "btn_go_split" to mapOf("fr" to "▶️ LANCER LE DÉCOUPAGE"),
            "btn_cancel" to mapOf("fr" to "❌ ANNULER"),
            "status_ready" to mapOf("fr" to "Prêt."),
            "status_split_ready" to mapOf("fr" to "Prêt à découper."),
            "sec3" to mapOf("fr" to "3. SÉCURITÉ & VÉRIFICATION"),
            "chk_verify" to mapOf("fr" to "Contrôle d'intégrité après écriture"),
            "chk_delete" to mapOf("fr" to "Supprimer les sources après succès"),
            "hash_hint" to mapOf("fr" to "Coller un hash SHA-256 de contrôle"),
            "btn_copy" to mapOf("fr" to "📋 Copier le SHA-256"),
            "menu_options" to mapOf("fr" to "⚙️ OPTIONS & MENU"),
            "mode_split" to mapOf("fr" to "📂 Basculer en Mode Splitter"),
            "mode_merge" to mapOf("fr" to "🔗 Basculer en Mode Fusion")
        )
        return map[key]?.get(currentLang) ?: map[key]?.get("fr") ?: key
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        prefs = getSharedPreferences("pkg_prefs", Context.MODE_PRIVATE)
        currentTheme = if (BuildConfig.FLAVOR == "lite") "CLASSIC" else (prefs.getString("theme", "CLASSIC") ?: "CLASSIC")
        currentLang = prefs.getString("lang", "fr") ?: "fr"
        
        createNotificationChannel()
        checkPermissions()
        showMergerScreen()
        applyTheme()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @SuppressLint("ClickableViewAccessibility")
    private fun createCustomButton(labelText: String, bgHex: String, borderHexStr: String, allowDuringProcessing: Boolean = false, onClickAction: () -> Unit): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }

        val buttonContent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        buttonContent.background = GradientDrawable().apply {
            setColor(Color.parseColor(bgHex))
            cornerRadius = dp(6).toFloat()
            setStroke(dp(2), Color.parseColor(borderHexStr))
        }

        val tv = TextView(this).apply {
            text = labelText
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        buttonContent.addView(tv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        container.addView(buttonContent, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        buttonContent.setOnClickListener {
            if (processing && !allowDuringProcessing) {
                toast("⚠️ Traitement en cours...")
                return@setOnClickListener
            }
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            onClickAction()
        }

        return container
    }

    private fun applyTheme() {
        val cfg = getThemeConfig(currentTheme)
        rootFrameLayout.setBackgroundColor(cfg.bg)
        rootLayout.setBackgroundColor(Color.TRANSPARENT)
        mainCol.setBackgroundColor(Color.TRANSPARENT)

        titleView.setTextColor(cfg.primaryText)
        subtitleView.setTextColor(cfg.secondaryText)
        subtitleView.text = "${tr("universe")} : ${cfg.name.uppercase()}"

        section1Title.setTextColor(cfg.secondaryText)
        section2Title.setTextColor(cfg.secondaryText)
        section3Title.setTextColor(cfg.secondaryText)

        checkView.setTextColor(cfg.secondaryText)
        statusView.setTextColor(cfg.primaryText)
        hashView.setTextColor(cfg.secondaryText)

        verifyBox.setTextColor(cfg.dim)
        deleteBox.setTextColor(cfg.dim)
        outView.setTextColor(cfg.dim)

        cardViews.forEach { card ->
            card.background = GradientDrawable().apply {
                setColor(cfg.card)
                cornerRadius = dp(8).toFloat()
                setStroke(dp(2), Color.parseColor(cfg.border))
            }
        }

        rebuildButtons()
        refresh()
    }

    private fun rebuildButtons() {
        val cfg = getThemeConfig(currentTheme)
        val parentCard1 = pickBtnLayout.parent as? LinearLayout
        val parentCard2 = outBtnLayout.parent as? LinearLayout
        val parentCard3 = copyBtnLayout.parent as? LinearLayout

        val idxPick = parentCard1?.indexOfChild(pickBtnLayout) ?: -1
        val idxDir = parentCard1?.indexOfChild(dirBtnLayout) ?: -1
        val idxRev = parentCard1?.indexOfChild(revBtnLayout) ?: -1
        val idxOut = parentCard2?.indexOfChild(outBtnLayout) ?: -1
        val idxGo = (goBtnLayout.parent as? LinearLayout)?.indexOfChild(goBtnLayout) ?: -1
        val idxCancel = (cancelBtnLayout.parent as? LinearLayout)?.indexOfChild(cancelBtnLayout) ?: -1
        val idxCopy = parentCard3?.indexOfChild(copyBtnLayout) ?: -1

        val actionContainer = goBtnLayout.parent as? LinearLayout

        pickBtnLayout = createCustomButton(tr("btn_pick"), cfg.btnPick.first, cfg.btnPick.second, false) { pick() }
        dirBtnLayout = createCustomButton(tr("btn_dir"), cfg.btnDir.first, cfg.btnDir.second, false) { pickFolder() }
        revBtnLayout = createCustomButton(tr("btn_rev"), cfg.btnRev.first, cfg.btnRev.second, false) { reversed = !reversed; refresh() }
        outBtnLayout = createCustomButton(tr("btn_out"), cfg.btnOut.first, cfg.btnOut.second, false) { pickOutput() }
        
        val goTextKey = if (appMode == "SPLIT") "btn_go_split" else "btn_go_merge"
        goBtnLayout = createCustomButton(tr(goTextKey), cfg.btnGo.first, cfg.btnGo.second, false) { executeAction() }
        cancelBtnLayout = createCustomButton(tr("btn_cancel"), cfg.btnCancel.first, cfg.btnCancel.second, true) {
            if (processing) { cancelled = true; statusView.text = "Annulation..." }
        }
        copyBtnLayout = createCustomButton(tr("btn_copy"), cfg.btnRev.first, cfg.btnRev.second, false) { copyHash() }

        if (parentCard1 != null && idxPick != -1) {
            parentCard1.removeViewAt(idxPick); parentCard1.addView(pickBtnLayout, idxPick, lp())
            parentCard1.removeViewAt(idxDir); parentCard1.addView(dirBtnLayout, idxDir, lp())
            parentCard1.removeViewAt(idxRev); parentCard1.addView(revBtnLayout, idxRev, lp())
        }
        if (parentCard2 != null && idxOut != -1) {
            parentCard2.removeViewAt(idxOut); parentCard2.addView(outBtnLayout, idxOut, lp())
        }
        if (actionContainer != null && idxGo != -1) {
            actionContainer.removeViewAt(idxGo)
            actionContainer.addView(goBtnLayout, idxGo, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            actionContainer.removeViewAt(idxCancel)
            actionContainer.addView(cancelBtnLayout, idxCancel, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dp(8) })
        }
        if (parentCard3 != null && idxCopy != -1) {
            parentCard3.removeViewAt(idxCopy); parentCard3.addView(copyBtnLayout, idxCopy, lp())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("pkg_channel", "PS PKG Notifications", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    private fun showMergerScreen() {
        rootFrameLayout = FrameLayout(this)
        bgVideoView = AspectRatioVideoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER)
        }
        rootFrameLayout.addView(bgVideoView)

        rootLayout = ScrollView(this).apply { setBackgroundColor(Color.TRANSPARENT) }
        mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.TRANSPARENT)
        }

        val headerLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(8)) }
        val titlesLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleView = TextView(this).apply { text = tr("app_title"); textSize = 20f; setTypeface(null, Typeface.BOLD) }
        titlesLayout.addView(titleView)
        subtitleView = TextView(this).apply { text = tr("universe"); textSize = 10f; typeface = Typeface.MONOSPACE }
        titlesLayout.addView(subtitleView)
        headerLayout.addView(titlesLayout, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        menuBtn = Button(this).apply { text = tr("menu_btn"); setOnClickListener { showCustomMenu() } }
        headerLayout.addView(menuBtn, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER_VERTICAL })
        mainCol.addView(headerLayout)

        val cfg = getThemeConfig(currentTheme)

        // Card 1
        val card1 = createCard()
        section1Title = text(tr("sec1"), 13f, Color.CYAN, true)
        card1.addView(section1Title)
        pickBtnLayout = createCustomButton(tr("btn_pick"), cfg.btnPick.first, cfg.btnPick.second, false) { pick() }
        card1.addView(pickBtnLayout, lp())
        dirBtnLayout = createCustomButton(tr("btn_dir"), cfg.btnDir.first, cfg.btnDir.second, false) { pickFolder() }
        card1.addView(dirBtnLayout, lp())
        revBtnLayout = createCustomButton(tr("btn_rev"), cfg.btnRev.first, cfg.btnRev.second, false) { reversed = !reversed; refresh() }
        card1.addView(revBtnLayout, lp())

        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(4)) }
        card1.addView(listContainer, lp())

        infoLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        pkgIconView = ImageView(this).apply { visibility = View.GONE; scaleType = ImageView.ScaleType.FIT_CENTER; adjustViewBounds = true }
        infoLayout.addView(pkgIconView, LinearLayout.LayoutParams(dp(80), WRAP_CONTENT).apply { rightMargin = dp(12) })
        checkView = text("", 11f, Color.CYAN).apply { typeface = Typeface.MONOSPACE }
        infoLayout.addView(checkView, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        card1.addView(infoLayout, lp())
        mainCol.addView(card1, lpCard())

        // Card 2
        val card2 = createCard()
        section2Title = text(tr("sec2"), 13f, Color.CYAN, true)
        card2.addView(section2Title)
        outBtnLayout = createCustomButton(tr("btn_out"), cfg.btnOut.first, cfg.btnOut.second, false) { pickOutput() }
        card2.addView(outBtnLayout, lp())
        outView = text(tr("out_lbl"), 11f, Color.GRAY).apply { typeface = Typeface.MONOSPACE }
        card2.addView(outView, lp())

        val actionLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        goBtnLayout = createCustomButton(tr("btn_go_merge"), cfg.btnGo.first, cfg.btnGo.second, false) { executeAction() }
        actionLayout.addView(goBtnLayout, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        cancelBtnLayout = createCustomButton(tr("btn_cancel"), cfg.btnCancel.first, cfg.btnCancel.second, true) {
            if (processing) { cancelled = true; statusView.text = "Annulation..." }
        }
        actionLayout.addView(cancelBtnLayout, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dp(8) })
        card2.addView(actionLayout, lp())

        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 1000; progress = 0 }
        card2.addView(bar, lp())
        statusView = text(tr("status_ready"), 13f, Color.MAGENTA).apply { typeface = Typeface.MONOSPACE }
        card2.addView(statusView, lp())
        mainCol.addView(card2, lpCard())

        // Card 3
        val card3 = createCard()
        section3Title = text(tr("sec3"), 13f, Color.CYAN, true)
        card3.addView(section3Title)
        verifyBox = CheckBox(this).apply { text = tr("chk_verify"); isChecked = true; textSize = 12f }
        card3.addView(verifyBox, lp())
        deleteBox = CheckBox(this).apply { text = tr("chk_delete"); isChecked = false; textSize = 12f }
        card3.addView(deleteBox, lp())
        hashField = EditText(this).apply { hint = tr("hash_hint"); setTextColor(Color.WHITE); textSize = 11f; typeface = Typeface.MONOSPACE }
        card3.addView(hashField, lp())
        hashView = text("", 11f, Color.CYAN).apply { typeface = Typeface.MONOSPACE }
        card3.addView(hashView, lp())
        copyBtnLayout = createCustomButton(tr("btn_copy"), cfg.btnRev.first, cfg.btnRev.second, false) { copyHash() }
        card3.addView(copyBtnLayout, lp())
        mainCol.addView(card3, lpCard())

        rootLayout.addView(mainCol)
        rootFrameLayout.addView(rootLayout, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(rootFrameLayout)
    }

    private fun showCustomMenu() {
        if (processing) { toast("⚠️ Traitement en cours !"); return }
        val cfg = getThemeConfig(currentTheme)
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply { setColor(cfg.card); cornerRadius = dp(10).toFloat(); setStroke(dp(2), Color.parseColor(cfg.border)) }
        }
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        fun addMenu(txt: String, act: () -> Unit) {
            val btn = Button(this).apply { text = txt; setTextColor(Color.WHITE); textSize = 12f; gravity = Gravity.START or Gravity.CENTER_VERTICAL; setBackgroundColor(Color.TRANSPARENT); setOnClickListener { act() } }
            dialogView.addView(btn, LinearLayout.LayoutParams(MATCH_PARENT, dp(40)))
        }

        addMenu(if (appMode == "MERGE") tr("mode_split") else tr("mode_merge")) {
            dialog.dismiss()
            appMode = if (appMode == "MERGE") "SPLIT" else "MERGE"
            section1Title.text = if (appMode == "SPLIT") tr("sec1_split") else tr("sec1")
            statusView.text = if (appMode == "SPLIT") tr("status_split_ready") else tr("status_ready")
            output = null; outView.text = tr("out_lbl")
            rebuildButtons()
        }
        dialog.show()
    }

    private fun lp() = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(6) }
    private fun lpCard() = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12); bottomMargin = dp(4) }
    private fun createCard() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)).also { cardViews.add(this) } }
    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply { text = t; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD) }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun pick() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) }, 1) }
    private fun pickFolder() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 3) }
    
    private fun pickOutput() {
        if (uris.isEmpty()) { toast("Choisis d'abord les sources !"); return }
        if (appMode == "SPLIT") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 5)
        } else {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { type = "application/octet-stream"; putExtra(Intent.EXTRA_TITLE, "${suggestName()}.pkg") }, 4)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK) return
        if (req == 1) {
            uris.clear(); names.clear(); sizes.clear()
            data?.clipData?.let { clip -> for (i in 0 until clip.itemCount) add(clip.getItemAt(i).uri) } ?: data?.data?.let { add(it) }
            reversed = false; refresh()
        } else if (req == 2 || req == 4) {
            data?.data?.let { output = it; outView.text = "Sortie : ${it.lastPathSegment}" }
        } else if (req == 3 || req == 5) {
            data?.data?.let { if (req == 5) { output = it; outView.text = "Dossier de sortie défini" } else loadTree(it) }
        }
    }

    private fun isPkgFile(n: String) = n.lowercase().run { endsWith(".pkg") || contains(".pkg.") || contains("_") }
    private fun getFileName(u: Uri): String {
        var n = "game.pkg"
        contentResolver.query(u, null, null, null, null)?.use { c ->
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && ni >= 0 && !c.isNull(ni)) c.getString(ni)?.let { n = it }
        }
        return n
    }

    private fun add(u: Uri) { uris.add(u); names.add(getFileName(u)); sizes.add(size(u)) }
    private fun size(u: Uri): Long {
        return contentResolver.query(u, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst() && i >= 0 && !c.isNull(i)) c.getLong(i) else 0L
        } ?: 0L
    }

    private fun loadTree(tree: Uri) {
        uris.clear(); names.clear(); sizes.clear()
        try {
            val docId = DocumentsContract.getTreeDocumentId(tree)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
            contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0); val name = c.getString(1); val mime = c.getString(3)
                    if (id != null && name != null && mime != DocumentsContract.Document.MIME_TYPE_DIR && isPkgFile(name)) {
                        uris.add(DocumentsContract.buildDocumentUriUsingTree(tree, id))
                        names.add(name)
                        sizes.add(if (c.isNull(2)) 0L else c.getLong(2))
                    }
                }
            }
        } catch (_: Exception) {}
        refresh()
    }

    private fun refresh() {
        if (!::listContainer.isInitialized) return
        listContainer.removeAllViews()
        if (uris.isEmpty()) { listContainer.addView(text("AUCUN FICHIER SÉLECTIONNÉ", 11f, Color.GRAY)); checkView.text = ""; return }
        val total = uris.indices.sumOf { sizes[it] }
        listContainer.addView(text("${uris.size} partie(s) • ${fmt(total)}", 12f, Color.WHITE, true))
    }

    private fun executeAction() {
        if (uris.isEmpty() || output == null) { toast("Sélectionne des fichiers et une sortie !"); return }
        if (appMode == "SPLIT") splitPkg() else mergePkg()
    }

    // Fonction de calcul SHA-256 universelle
    private fun computeSha256(u: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openInputStream(u)?.use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) { null }
    }

    // MODE SPLITTER AVEC CONTRÔLE SHA-256 INTÉGRÉ
    private fun splitPkg() {
        val treeUri = output ?: return
        val sourceUri = uris[0]
        processing = true; cancelled = false
        goBtnLayout.isEnabled = false; cancelBtnLayout.isEnabled = true
        bar.progress = 0; statusView.text = "Découpage en cours…"

        Thread {
            var success = false
            try {
                val total = size(sourceUri)
                val targetDir = DocumentFile.fromTreeUri(this, treeUri) ?: throw Exception("Dossier invalide")
                val cleanBase = getFileName(sourceUri).substringBeforeLast(".")

                contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfdIn ->
                    FileInputStream(pfdIn.fileDescriptor).use { fis ->
                        fis.channel.use { inChannel ->
                            var partIndex = 0; var transferred = 0L
                            while (transferred < total && !cancelled) {
                                val partName = "${cleanBase}_${String.format(Locale.US, "%02d", partIndex)}.pkg"
                                val newFile = targetDir.createFile("application/octet-stream", partName) ?: throw Exception("Création fichier échouée")

                                contentResolver.openFileDescriptor(newFile.uri, "w")?.use { pfdOut ->
                                    FileOutputStream(pfdOut.fileDescriptor).use { fos ->
                                        fos.channel.use { outChannel ->
                                            val partSize = minOf(4L * 1024 * 1024 * 1024, total - transferred)
                                            val n = inChannel.transferTo(transferred, partSize, outChannel)
                                            transferred += n
                                            outChannel.force(true)
                                        }
                                    }
                                }
                                partIndex++
                            }
                        }
                    }
                }
                success = !cancelled
            } catch (_: Exception) {}

            // VÉRIFICATION D'INTÉGRITÉ SHA-256 DU FICHIER SOURCE AU SPLITTER
            var computedHash: String? = null
            if (success && verifyBox.isChecked) {
                ui.post { statusView.text = "Calcul du SHA-256 de la source..." }
                computedHash = computeSha256(sourceUri)
            }

            ui.post {
                processing = false; doneUi()
                if (success) {
                    bar.progress = 1000
                    lastHash = computedHash
                    if (computedHash != null) {
                        hashView.text = "SHA-256 (Source) : $computedHash"
                    }
                    statusView.text = "Découpage réussi !"
                } else {
                    statusView.text = "Découpage échoué ou annulé."
                }
            }
        }.start()
    }

    // MODE FUSION AVEC CONTRÔLE SHA-256
    private fun mergePkg() {
        val out = output ?: return
        processing = true; cancelled = false
        goBtnLayout.isEnabled = false; cancelBtnLayout.isEnabled = true
        bar.progress = 0; statusView.text = "Fusion en cours…"

        Thread {
            val total = uris.sumOf { size(it) }
            var done = 0L; var success = false

            try {
                contentResolver.openFileDescriptor(out, "w")?.use { pfdOut ->
                    FileOutputStream(pfdOut.fileDescriptor).use { fos ->
                        fos.channel.use { outChannel ->
                            var offset = 0L
                            for (u in uris) {
                                if (cancelled) break
                                contentResolver.openFileDescriptor(u, "r")?.use { pfdIn ->
                                    FileInputStream(pfdIn.fileDescriptor).use { fis ->
                                        fis.channel.use { inChannel ->
                                            val n = outChannel.transferFrom(inChannel, offset, inChannel.size())
                                            offset += n; done += n
                                        }
                                    }
                                }
                            }
                            outChannel.force(true)
                        }
                    }
                }
                success = !cancelled
            } catch (_: Exception) {}

            // VÉRIFICATION D'INTÉGRITÉ SHA-256 APRÈS FUSION
            var computedHash: String? = null
            if (success && verifyBox.isChecked) {
                ui.post { statusView.text = "Calcul du SHA-256..." }
                computedHash = computeSha256(out)
            }

            ui.post {
                processing = false; doneUi()
                if (success) {
                    bar.progress = 1000
                    lastHash = computedHash
                    if (computedHash != null) {
                        hashView.text = "SHA-256 : $computedHash"
                    }
                    statusView.text = "Fusion réussie !"
                } else {
                    statusView.text = "Fusion échouée/annulée."
                }
            }
        }.start()
    }

    private fun doneUi() { goBtnLayout.isEnabled = true; cancelBtnLayout.isEnabled = false }
    private fun copyHash() {
        val h = lastHash ?: return
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("sha256", h))
        toast("SHA-256 copié")
    }

    private fun fmt(b: Long): String {
        if (b <= 0L) return "0 o"
        val u = arrayOf("o", "Ko", "Mo", "Go", "To")
        var v = b.toDouble(); var i = 0
        while (v >= 1024 && i < u.size - 1) { v /= 1024; i++ }
        return String.format(Locale.US, "%.2f %s", v, u[i])
    }

    private fun suggestName(): String {
        if (names.isEmpty()) return "merged"
        var base = names[0].substringBeforeLast(".")
        return base.replace(Regex("[._\\- ]?(?:part)?[._\\- ]?\\d+$", RegexOption.IGNORE_CASE), "")
    }
}
