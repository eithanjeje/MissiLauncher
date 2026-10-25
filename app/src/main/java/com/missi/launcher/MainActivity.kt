package com.missi.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Tile(
    val label: String,
    val image: Drawable?,
    val full: Boolean,
    val color: Int,
    val onClick: () -> Unit
)

class TileAdapter(
    private val items: List<Tile>,
    private val dp: (Int) -> Int,
    private val onFocus: (Tile) -> Unit
) : RecyclerView.Adapter<TileAdapter.VH>() {

    class VH(val card: FrameLayout, val img: ImageView, val lbl: TextView) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val card = FrameLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(dp(240), dp(135)).also { it.marginEnd = dp(16) }
            isFocusable = true
            isClickable = true
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        val img = ImageView(ctx)
        val lbl = TextView(ctx).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(8))
            setBackgroundColor(0x66000000)
            maxLines = 1
        }
        card.addView(img)
        card.addView(lbl, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        return VH(card, img, lbl)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = items[pos]
        h.card.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(t.color, (t.color and 0x00FFFFFF) or 0xFF000000.toInt())
        ).apply { cornerRadius = dp(16).toFloat() }
        h.img.setImageDrawable(t.image)
        if (t.full) {
            h.img.layoutParams = FrameLayout.LayoutParams(-1, -1)
            h.img.scaleType = ImageView.ScaleType.CENTER_CROP
            h.lbl.visibility = View.GONE
        } else {
            h.img.layoutParams = FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER_HORIZONTAL).also { it.topMargin = dp(20) }
            h.img.scaleType = ImageView.ScaleType.FIT_CENTER
            h.lbl.visibility = View.VISIBLE
            h.lbl.text = t.label
        }
        h.card.setOnClickListener { t.onClick() }
        h.card.setOnFocusChangeListener { v, focused ->
            val s = if (focused) 1.1f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(150).start()
            v.translationZ = if (focused) dp(12).toFloat() else 0f
            v.foreground = if (focused) GradientDrawable().apply {
                setStroke(dp(4), Color.WHITE); cornerRadius = dp(16).toFloat()
            } else null
            if (focused) onFocus(t)
        }
    }
}

class MainActivity : Activity() {

    private lateinit var title: TextView
    private lateinit var sub: TextView
    private lateinit var appsRow: RecyclerView
    private lateinit var inputsRow: RecyclerView
    private val palette = intArrayOf(
        0xFF2B6CB0.toInt(), 0xFF6B46C1.toInt(), 0xFF2C7A7B.toInt(),
        0xFFC05621.toInt(), 0xFFB83280.toInt(), 0xFF2F855A.toInt()
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(0, dp(24), 0, dp(24))
        }
        val scroll = ScrollView(this).apply {
            clipChildren = false
            isVerticalScrollBarEnabled = false
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF22305A.toInt(), 0xFF0F1526.toInt())
            )
            addView(root)
        }

        val top = FrameLayout(this).apply { setPadding(dp(48), 0, dp(48), 0) }
        top.addView(TextView(this).apply {
            text = "Missi"; textSize = 30f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        })
        top.addView(TextClock(this).apply {
            format24Hour = "HH:mm"; format12Hour = "HH:mm"; textSize = 26f
            setTextColor(0xFFA8B2CC.toInt())
        }, FrameLayout.LayoutParams(-2, -2, Gravity.END))
        root.addView(top)

        title = TextView(this).apply {
            text = "Missi Launcher"; textSize = 52f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); setPadding(dp(48), dp(40), dp(48), 0)
        }
        sub = TextView(this).apply {
            text = "Tus apps y entradas, sin anuncios."; textSize = 20f
            setTextColor(0xFFA8B2CC.toInt()); setPadding(dp(48), dp(6), dp(48), dp(24))
        }
        root.addView(title); root.addView(sub)

        root.addView(heading("Apps"))
        appsRow = row(); root.addView(appsRow)
        root.addView(heading("Entradas"))
        inputsRow = row(); root.addView(inputsRow)

        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        appsRow.adapter = TileAdapter(loadApps(), ::dp, ::showFocus)
        inputsRow.adapter = TileAdapter(loadInputs(), ::dp, ::showFocus)
    }

    // Es el inicio de la tele: Atrás no hace nada.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}

    private fun showFocus(t: Tile) { title.text = t.label }

    private fun heading(s: String) = TextView(this).apply {
        text = s; textSize = 20f; setTextColor(0xFFA8B2CC.toInt()); setPadding(dp(48), dp(8), dp(48), 0)
    }

    private fun row() = RecyclerView(this).apply {
        layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        clipToPadding = false
        clipChildren = false
        setPadding(dp(48), dp(16), dp(48), dp(24))
    }

    private fun loadApps(): List<Tile> {
        val pm = packageManager
        val tv = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val phone = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val found = (pm.queryIntentActivities(tv, 0) + pm.queryIntentActivities(phone, 0))
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        return found.mapIndexed { n, ri ->
            val banner = ri.activityInfo.loadBanner(pm)
            val cn = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
            Tile(ri.loadLabel(pm).toString(), banner ?: ri.loadIcon(pm), banner != null, palette[n % palette.size]) {
                try {
                    startActivity(Intent(Intent.ACTION_MAIN).setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(this, "No se pudo abrir esta app", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadInputs(): List<Tile> {
        val tvm = getSystemService(TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return try {
            tvm.tvInputList.filter { it.isPassthroughInput }.mapIndexed { n, info ->
                val name = info.loadLabel(this)?.toString() ?: "Entrada ${n + 1}"
                Tile(name, info.loadIcon(this), false, palette[(n + 2) % palette.size]) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.id)))
                    } catch (e: Exception) {
                        Toast.makeText(this, "Usa el botón FUENTE del control para $name", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) { emptyList() }
    }
}
