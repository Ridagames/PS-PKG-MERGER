package com.venice.pkgmerger

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.Html
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

class MainActivity : Activity() {

    private val uris = ArrayList<Uri>()
    private val names = ArrayList<String>()
    private val sizes = ArrayList<Long>()

    private var output: Uri? = null
    private var reversed = false
    private var lastHash: String? = null
    private val ui = Handler(Looper.getMainLooper())

    private lateinit var listView: TextView
    private lateinit var checkView: TextView
    private lateinit var outView: TextView
    private lateinit var statusView: TextView
    private lateinit var hashView: TextView
    private lateinit var hashField: EditText
    private lateinit var verifyBox: CheckBox
    private lateinit var bar: ProgressBar
    private lateinit var pickBtn: Button
    private lateinit var dirBtn: Button
    private lateinit var outBtn: Button
    private lateinit var goBtn: Button
    private lateinit var revBtn: Button
    private lateinit var copyBtn: Button

    private val accent = Color.parseColor("#8AB4FF")
    private val dim = Color.parseColor("#9AA6B4")

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val pad = dp(16)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.parseColor("#0C1016"))
        }

        col.addView(text("PS5 PKG Merger", 22f, Color.WHITE, true))
        col.addView(text("Réassemble les parties d'un PKG et vérifie l'intégrité.", 13f, dim))

        pickBtn = Button(this).apply {
            text = "1. Choisir les parties (.pkg / .pkg.0 …)"
            setOnClickListener { pick() }
        }
        col.addView(pickBtn, lp())

        dirBtn = Button(this).apply {
            text = "…ou choisir un dossier entier"
            setOnClickListener { pickFolder() }
        }
        col.addView(dirBtn, lp())

        revBtn = Button(this).apply {
            text = "Inverser l'ordre"
            setOnClickListener { reversed = !reversed; refresh() }
        }
        col.addView(revBtn, lp())

        listView = text("Aucune partie sélectionnée.", 12f, dim).apply {
            typeface = Typeface.MONOSPACE
        }
        col.addView(listView, lp())

        checkView = text("", 12f, accent).apply { typeface = Typeface.MONOSPACE }
        col.addView(checkView, lp())

        outBtn = Button(this).apply {
            text = "2. Choisir le fichier de sortie"
            setOnClickListener { pickOutput() }
        }
        col.addView(outBtn, lp())

        outView = text("Sortie : —", 12f, dim).apply { typeface = Typeface.MONOSPACE }
        col.addView(outView, lp())

        goBtn = Button(this).apply {
            text = "3. Fusionner"
            setOnClickListener { merge() }
        }
        col.addView(goBtn, lp())

        bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = 0
        }
        col.addView(bar, lp())

        statusView = text("Prêt.", 13f, accent)
        col.addView(statusView, lp())

        col.addView(text("Vérifications", 13f, Color.WHITE, true))

        verifyBox = CheckBox(this).apply {
            text = "Relire le fichier après écriture (recalcule le hash et compare)"
            isChecked = true
            setTextColor(dim)
            textSize = 12f
        }
        col.addView(verifyBox, lp())

        col.addView(text("Hash attendu — optionnel (MD5 32 / SHA-1 40 / SHA-256 64)", 12f, dim))
        hashField = EditText(this).apply {
            hint = "colle un hash pour comparer"
            setTextColor(Color.WHITE)
            setHintTextColor(dim)
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        col.addView(hashField, lp())

        hashView = text("", 11f, accent).apply { typeface = Typeface.MONOSPACE }
        col.addView(hashView, lp())

        copyBtn = Button(this).apply {
            text = "Copier le SHA-256 du fichier produit"
            isEnabled = false
            setOnClickListener { copyHash() }
        }
        col.addView(copyBtn, lp())

        col.addView(
            text(
                "Destination conseillée : stockage interne ou exFAT. Évite le FAT32 (limite 4 Go).",
                11f, dim
            )
        )

        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp() = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
        topMargin = dp(6)
    }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
        }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    // ---------- Sélection ----------

    private fun pick() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }, 1
        )
    }

    private fun pickFolder() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, 3
        )
    }

    private fun pickOutput() {
        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, suggestName())
            }, 2
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK) return

        if (req == 1) {
            uris.clear(); names.clear(); sizes.clear()
            val clip = data?.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) add(clip.getItemAt(i).uri)
            } else {
                data?.data?.let { add(it) }
            }
            reversed = false
            refresh()
        } else if (req == 2) {
            val u = data?.data ?: return
            persist(u, true)
            output = u
            outView.text = "Sortie : ${u.lastPathSegment}"
        } else if (req == 3) {
            val tree = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(
                    tree, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            loadTree(tree)
        }
    }

    private fun loadTree(tree: Uri) {
        uris.clear(); names.clear(); sizes.clear()
        try {
            val docId = DocumentsContract.getTreeDocumentId(tree)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
            val cols = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            val all = ArrayList<Triple<Uri, String, Long>>()
            contentResolver.query(children, cols, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val name = c.getString(1)
                    val mime = c.getString(3)
                    if (id != null && name != null &&
                        mime != DocumentsContract.Document.MIME_TYPE_DIR
                    ) {
                        val size = if (c.isNull(2)) 0L else c.getLong(2)
                        all.add(
                            Triple(
                                DocumentsContract.buildDocumentUriUsingTree(tree, id),
                                name, size
                            )
                        )
                    }
                }
            }
            val picked = all.filter { isPkgPart(it.second) }.ifEmpty { all }
            picked.forEach { p -> uris.add(p.first); names.add(p.second); sizes.add(p.third) }
        } catch (e: Exception) {
            toast("Lecture du dossier impossible : ${e.message}")
        }
        reversed = false
        refresh()
        if (uris.size < 2) toast("Aucune partie .pkg trouvée dans ce dossier.")
    }

    private fun persist(u: Uri, write: Boolean = false) {
        try {
            var f = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (write) f = f or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(u, f)
        } catch (_: Exception) {
        }
    }

    private fun add(u: Uri) {
        persist(u)
        var n = u.lastPathSegment ?: "part"
        var s = 0L
        try {
            contentResolver.query(u, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0 && !c.isNull(ni)) c.getString(ni)?.let { n = it }
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) s = c.getLong(si)
                }
            }
        } catch (_: Exception) {
        }
        uris.add(u); names.add(n); sizes.add(s)
    }

    private fun refresh() {
        if (uris.isEmpty()) {
            listView.text = "Aucune partie sélectionnée."
            checkView.text = ""
            return
        }
        val order = order()
        val total = order.sumOf { sizes[it] }
        listView.text = buildString {
            append(order.size).append(" partie(s)")
            if (total > 0) append(" • ").append(fmt(total))
            append(" • ordre ").append(if (reversed) "inversé" else "croissant").append("\n")
            order.forEachIndexed { i, idx ->
                append(i + 1).append(". ").append(names[idx])
                if (sizes[idx] > 0) append("  (").append(fmt(sizes[idx])).append(")")
                append("\n")
            }
        }
        checkView.text = Html.fromHtml(checksHtml(), Html.FROM_HTML_MODE_LEGACY)
    }

    private fun checksHtml(): String {
        val sb = StringBuilder()
        val order = order()
        if (order.isEmpty()) return ""

        if (hasPkgMagic(uris[order[0]])) {
            sb.append("<font color=\"#9CC7A6\">✓ En-tête PKG valide (7F 43 4E 54)</font><br>")
        } else {
            sb.append("<font color=\"#FFB74D\">⚠ En-tête PKG absent sur la 1re partie</font><br>")
        }

        val s = order.map { sizes[it] }
        val body = s.dropLast(1)
        when {
            body.isEmpty() ->
                sb.append("<font color=\"#9AA6B4\">• Une seule partie</font><br>")
            body.distinct().size > 1 ->
                sb.append("<font color=\"#FFB74D\">⚠ Parties de tailles différentes</font><br>")
            s.last() > body[0] ->
                sb.append("<font color=\"#FFB74D\">⚠ Dernière partie plus grande que les autres</font><br>")
            else ->
                sb.append("<font color=\"#9CC7A6\">✓ Tailles cohérentes (")
                    .append(fmt(body[0])).append(" par partie)</font><br>")
        }

        val nums = order.mapNotNull { trailingNumber(names[it]) }
        if (nums.size == order.size) {
            val sorted = nums.sorted()
            var gap = false
            for (i in 1 until sorted.size) {
                if (sorted[i] != sorted[i - 1] + 1) gap = true
            }
            if (gap) {
                sb.append("<font color=\"#FFB74D\">⚠ Numérotation discontinue — partie manquante ?</font><br>")
            } else {
                sb.append("<font color=\"#9CC7A6\">✓ Numérotation continue</font><br>")
            }
        } else {
            sb.append("<font color=\"#9AA6B4\">• Numérotation non détectée</font><br>")
        }
        return sb.toString()
    }

    // ---------- Fusion ----------

    private fun order(): List<Int> {
        val o = uris.indices.sortedWith { a, b -> naturalCompare(names[a], names[b]) }
        return if (reversed) o.reversed() else o
    }

    private fun merge() {
        if (uris.size < 2) { toast("Sélectionne au moins 2 parties."); return }
        val out = output
        if (out == null) { toast("Choisis d'abord le fichier de sortie."); return }

        val list = order().map { uris[it] }
        val wantReread = verifyBox.isChecked
        val expected = hashField.text.toString().trim().lowercase().replace(" ", "")

        pickBtn.isEnabled = false
        dirBtn.isEnabled = false
        revBtn.isEnabled = false
        outBtn.isEnabled = false
        goBtn.isEnabled = false
        copyBtn.isEnabled = false
        hashView.text = ""
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        bar.progress = 0
        statusView.text = "Fusion en cours…"

        Thread {
            val total = list.sumOf { size(it) }
            val md5 = MessageDigest.getInstance("MD5")
            val sha1 = MessageDigest.getInstance("SHA-1")
            val sha256 = MessageDigest.getInstance("SHA-256")
            val problems = ArrayList<String>()
            var done = 0L
            var lastPush = 0L
            val t0 = System.currentTimeMillis()

            try {
                val pfd = contentResolver.openFileDescriptor(out, "wt")
                    ?: throw IllegalStateException("Ouverture de la sortie impossible")
                try {
                    val os = BufferedOutputStream(FileOutputStream(pfd.fileDescriptor), 1 shl 20)
                    val buf = ByteArray(1 shl 20)
                    for (u in list) {
                        val ins = contentResolver.openInputStream(u)
                            ?: throw IllegalStateException("Lecture impossible : ${u.lastPathSegment}")
                        ins.use { input ->
                            while (true) {
                                val r = input.read(buf)
                                if (r <= 0) break
                                os.write(buf, 0, r)
                                md5.update(buf, 0, r)
                                sha1.update(buf, 0, r)
                                sha256.update(buf, 0, r)
                                done += r
                                val now = System.currentTimeMillis()
                                if (now - lastPush > 250) {
                                    lastPush = now
                                    push(done, total, t0)
                                }
                            }
                        }
                    }
                    os.flush()
                    try { pfd.fileDescriptor.sync() } catch (_: Exception) {}
                } finally {
                    try { pfd.close() } catch (_: Exception) {}
                }
            } catch (e: Throwable) {
                ui.post {
                    statusView.text = "Erreur : ${e.message ?: e.javaClass.simpleName}"
                    doneUi()
                }
                return@Thread
            }

            val h5 = hex(md5.digest())
            val h1 = hex(sha1.digest())
            val h256 = hex(sha256.digest())

            // --- Vérifications ---
            val disk = size(out)
            if (total > 0 && disk != done) {
                problems.add("Taille sur disque (${fmt(disk)}) différente de l'écrit (${fmt(done)})")
            }
            if (!hasPkgMagic(out)) {
                problems.add("En-tête PKG absent sur le fichier produit")
            }

            if (wantReread) {
                ui.post { statusView.text = "Vérification : relecture du fichier…" }
                try {
                    val re = MessageDigest.getInstance("SHA-256")
                    var reread = 0L
                    contentResolver.openInputStream(out)?.use { ins ->
                        val b = ByteArray(1 shl 20)
                        while (true) {
                            val r = ins.read(b)
                            if (r <= 0) break
                            re.update(b, 0, r)
                            reread += r
                        }
                    }
                    if (reread != done) {
                        problems.add("Relecture : ${fmt(reread)} lus au lieu de ${fmt(done)}")
                    } else if (hex(re.digest()) != h256) {
                        problems.add("Hash différent après relecture — écriture instable")
                    }
                } catch (e: Throwable) {
                    problems.add("Relecture impossible : ${e.message}")
                }
            }

            if (expected.isNotEmpty()) {
                val actual = when (expected.length) {
                    32 -> h5
                    40 -> h1
                    64 -> h256
                    else -> null
                }
                if (actual == null) {
                    problems.add("Hash attendu : longueur invalide (${expected.length})")
                } else if (actual != expected) {
                    problems.add("Le hash attendu ne correspond pas")
                }
            }

            val secs = (System.currentTimeMillis() - t0) / 1000
            val report = StringBuilder()
            report.append("<font color=\"#9AA6B4\">SHA-256</font> ").append(h256).append("<br>")
            report.append("<font color=\"#9AA6B4\">SHA-1&nbsp;&nbsp;</font> ").append(h1).append("<br>")
            report.append("<font color=\"#9AA6B4\">MD5&nbsp;&nbsp;&nbsp;&nbsp;</font> ").append(h5)
            report.append("<br><br>")
            if (problems.isEmpty()) {
                report.append("<font color=\"#9CC7A6\">✓ Intégrité vérifiée — ")
                    .append(fmt(done)).append("</font>")
            } else {
                problems.forEach {
                    report.append("<font color=\"#FFB74D\">⚠ ").append(it).append("</font><br>")
                }
            }

            ui.post {
                bar.progress = 1000
                lastHash = h256
                copyBtn.isEnabled = true
                hashView.text = Html.fromHtml(report.toString(), Html.FROM_HTML_MODE_LEGACY)
                statusView.text = if (problems.isEmpty())
                    "✓ Fusion terminée et vérifiée en ${secs} s"
                else
                    "⚠ Fusion terminée AVEC ANOMALIES — voir ci-dessus"
                doneUi()
            }
        }.start()
    }

    private fun push(done: Long, total: Long, t0: Long) {
        val elapsed = (System.currentTimeMillis() - t0) / 1000.0
        val speed = if (elapsed > 0.5) done / elapsed else 0.0
        val pct = if (total > 0) (done * 1000 / total).toInt() else 0
        val eta = if (speed > 0 && total > done) ((total - done) / speed).toLong() else -1L
        ui.post {
            bar.progress = pct
            statusView.text = buildString {
                append(fmt(done))
                if (total > 0) { append(" / "); append(fmt(total)) }
                append(" • ").append(fmt(speed.toLong())).append("/s")
                if (eta > 0) append(" • reste ").append(eta).append("s")
            }
        }
    }

    private fun doneUi() {
        pickBtn.isEnabled = true
        dirBtn.isEnabled = true
        revBtn.isEnabled = true
        outBtn.isEnabled = true
        goBtn.isEnabled = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun copyHash() {
        val h = lastHash ?: return
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("sha256", h))
        toast("SHA-256 copié")
    }

    // ---------- Utilitaires ----------

    private fun size(u: Uri): Long {
        return try {
            contentResolver.query(u, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst() && i >= 0 && !c.isNull(i)) c.getLong(i) else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun hasPkgMagic(u: Uri): Boolean {
        return try {
            contentResolver.openInputStream(u)?.use { i ->
                val b = ByteArray(4)
                i.read(b) == 4 &&
                    b[0] == 0x7F.toByte() && b[1] == 0x43.toByte() &&
                    b[2] == 0x4E.toByte() && b[3] == 0x54.toByte()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun hex(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (x in b) sb.append(String.format("%02x", x))
        return sb.toString()
    }

    private fun fmt(b: Long): String {
        if (b <= 0) return "0 o"
        val u = arrayOf("o", "Ko", "Mo", "Go", "To")
        var v = b.toDouble()
        var i = 0
        while (v >= 1024 && i < u.size - 1) { v /= 1024; i++ }
        return if (i == 0) "$b o" else String.format(Locale.US, "%.2f %s", v, u[i])
    }

    private val chunk = Regex("(\\d+|\\D+)")
    private val trailing = Regex("(\\d+)$")
    private val pkgPart = Regex("(?i)^.+\\.pkg(\\.(\\d+|part\\d+))?$")

    private fun isPkgPart(n: String) = pkgPart.matches(n)

    private fun trailingNumber(n: String) =
        trailing.find(n)?.groupValues?.get(1)?.toLongOrNull()

    private fun naturalCompare(a: String, b: String): Int {
        val ca = chunk.findAll(a).map { it.value }.toList()
        val cb = chunk.findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(ca.size, cb.size)) {
            val x = ca[i]
            val y = cb[i]
            val dx = x[0].isDigit()
            val dy = y[0].isDigit()
            val c = when {
                dx && dy -> {
                    val xs = x.trimStart('0').ifEmpty { "0" }
                    val ys = y.trimStart('0').ifEmpty { "0" }
                    if (xs.length != ys.length) xs.length - ys.length else xs.compareTo(ys)
                }
                dx != dy -> if (dx) 1 else -1
                else -> x.compareTo(y, ignoreCase = true)
            }
            if (c != 0) return c
        }
        return ca.size - cb.size
    }

    private fun suggestName(): String {
        if (names.isEmpty()) return "merged.pkg"
        val first = names.minWithOrNull { a, b -> naturalCompare(a, b) } ?: return "merged.pkg"
        val m = Regex("^(.*?)[._\\- ]?(?:part)?[._\\- ]?(\\d+)$", RegexOption.IGNORE_CASE)
            .find(first)
        var base = m?.groupValues?.get(1)?.trimEnd('.', '_', '-', ' ') ?: first
        if (base.isBlank()) base = "merged"
        return if (base.lowercase().endsWith(".pkg")) base else "$base.pkg"
    }
}
