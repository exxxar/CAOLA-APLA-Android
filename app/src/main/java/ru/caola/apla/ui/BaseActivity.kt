package ru.caola.apla.ui

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import ru.caola.apla.data.DebugLog
import ru.caola.apla.data.SessionStore

abstract class BaseActivity : AppCompatActivity() {
    protected val navy = Color.rgb(10, 18, 32)
    protected val panel = Color.rgb(22, 34, 54)
    protected val panel2 = Color.rgb(29, 44, 67)
    protected val mint = Color.rgb(49, 211, 158)
    protected val blue = Color.rgb(91, 156, 255)
    protected val muted = Color.rgb(151, 168, 190)
    protected val white = Color.WHITE
    protected lateinit var root: LinearLayout
    protected val session by lazy { SessionStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DebugLog.add("INFO", "Открыт экран ${this::class.java.simpleName}")
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(navy) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(14), dp(20), dp(10)) }
        header.addView(TextView(this).apply { text = "CAOLA  /  APLA"; textSize = 15f; letterSpacing = .12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(mint) }, LinearLayout.LayoutParams(0, dp(34), 1f))
        header.addView(TextView(this).apply { text = if (session.mode.name == "DEMO") "DEMO  •  RU" else "SERVER  •  RU"; textSize = 10f; letterSpacing = .08f; setTextColor(muted); setPadding(dp(10), dp(7), dp(10), dp(7)); background = rounded(panel2, 30) })
        shell.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; setPadding(0, 0, 0, dp(10)) }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(6), dp(20), dp(24)); setBackgroundColor(navy) }
        scroll.addView(root)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        if (hasBottomNav()) shell.addView(bottomNav())
        setContentView(shell)
    }
    private fun hasBottomNav() = this is DashboardActivity || this is LessonCatalogActivity || this is ProgressActivity || this is CaolaActivity || this is AplaActivity
    private fun bottomNav(): View {
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(5), dp(8), dp(5), dp(8)); setBackgroundColor(Color.rgb(16, 27, 44)) }
        val items = listOf(Triple("⌂", "Главная", DashboardActivity::class.java), Triple("▤", "Уроки", LessonCatalogActivity::class.java), Triple("◷", "Прогресс", ProgressActivity::class.java), Triple("◈", "CAOLA", CaolaActivity::class.java), Triple("✦", "APLA", AplaActivity::class.java))
        items.forEach { (glyph, label, target) ->
            val active = this::class.java == target
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, dp(3), 0, dp(2)) }
            item.addView(TextView(this).apply { text = glyph; textSize = 20f; gravity = Gravity.CENTER; setTextColor(if (active) mint else muted) })
            item.addView(TextView(this).apply { text = label; textSize = 9f; gravity = Gravity.CENTER; setTextColor(if (active) mint else muted); typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT })
            item.setOnClickListener { if (!active) startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
            bar.addView(item, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        return bar
    }
    protected fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    protected fun rounded(color: Int, radius: Int = 18): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    protected fun title(value: String, subtitle: String? = null) {
        root.addView(TextView(this).apply { text = value; textSize = 27f; typeface = Typeface.create("sans-serif", Typeface.BOLD); setTextColor(white); setPadding(0, dp(10), 0, dp(4)) })
        if (subtitle != null) root.addView(TextView(this).apply { text = subtitle; textSize = 13f; setTextColor(muted); setPadding(0, 0, 0, dp(16)) })
    }
    protected fun section(value: String) { root.addView(TextView(this).apply { text = value; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(white); setPadding(0, dp(18), 0, dp(10)) }) }
    protected fun text(value: String, size: Float = 15f) { root.addView(TextView(this).apply { text = value; textSize = size; setTextColor(if (size <= 13f) muted else Color.rgb(218, 228, 240)); setPadding(0, dp(4), 0, dp(4)); setLineSpacing(dp(2).toFloat(), 1f) }) }
    protected fun card(block: LinearLayout.() -> Unit): LinearLayout {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); background = rounded(panel); block() }
        root.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }); return box
    }
    protected fun cardText(parent: LinearLayout, value: String, size: Float = 14f, color: Int = Color.rgb(218,228,240), bold: Boolean = false) {
        parent.addView(TextView(this).apply { text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(3), 0, dp(3)); setLineSpacing(dp(2).toFloat(), 1f) })
    }
    protected fun metric(parent: LinearLayout, value: String, label: String) { cardText(parent, value, 24f, mint, true); cardText(parent, label, 11f, muted) }
    protected fun progress(parent: LinearLayout, label: String, value: Int, color: Int = mint) {
        cardText(parent, "$label  ·  $value%", 12f, Color.rgb(220,230,242), true)
        val track = FrameLayout(this).apply { background = rounded(Color.rgb(42,57,78), 20); clipToOutline = true }
        val fill = View(this).apply { background = rounded(color, 20) }
        track.addView(fill, FrameLayout.LayoutParams((resources.displayMetrics.widthPixels - dp(72)) * value.coerceIn(0,100) / 100, dp(7)))
        parent.addView(track, LinearLayout.LayoutParams(-1, dp(7)).apply { topMargin = dp(5); bottomMargin = dp(9) })
    }
    protected fun chart(values: List<Int>, labels: List<String>): View = object : View(this) {
        private val line = Paint(3).apply { color = mint; strokeWidth = dp(3).toFloat(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val dot = Paint(3).apply { color = mint; style = Paint.Style.FILL }
        private val grid = Paint(1).apply { color = Color.rgb(49,65,86); strokeWidth = 1f }
        private val labelPaint = Paint(1).apply { color = muted; textSize = dp(10).toFloat(); textAlign = Paint.Align.CENTER }
        override fun onDraw(c: Canvas) { super.onDraw(c); val left=dp(12).toFloat(); val right=width-dp(12).toFloat(); val top=dp(14).toFloat(); val bottom=height-dp(26).toFloat(); repeat(4) { i -> val y=top+(bottom-top)*i/3f; c.drawLine(left,y,right,y,grid) }; if(values.isEmpty()) return; val maxV=(values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat(); val step=(right-left)/(values.size-1).coerceAtLeast(1); val pts=values.mapIndexed { i,v -> (left+i*step) to (bottom-(v/maxV)*(bottom-top)*.82f) }; for(i in 0 until pts.size-1) c.drawLine(pts[i].first,pts[i].second,pts[i+1].first,pts[i+1].second,line); pts.forEachIndexed { i,p -> c.drawCircle(p.first,p.second,dp(4).toFloat(),dot); if(i<labels.size) c.drawText(labels[i],p.first,height-dp(7).toFloat(),labelPaint) } }
    }.apply { layoutParams = LinearLayout.LayoutParams(-1, dp(160)); setPadding(0, dp(6), 0, 0) }
    protected fun action(label: String, target: Class<out BaseActivity>) {
        root.addView(Button(this).apply { text=label; isAllCaps=false; textSize=14f; setTextColor(white); background=rounded(panel2,14); setOnClickListener { startActivity(Intent(this@BaseActivity,target)) } }, LinearLayout.LayoutParams(-1,dp(50)).apply { bottomMargin=dp(8) })
    }
    protected fun primary(label: String, onClick: () -> Unit) {
        root.addView(Button(this).apply { text=label; isAllCaps=false; textSize=14f; typeface=Typeface.DEFAULT_BOLD; setTextColor(navy); background=rounded(mint,14); setOnClickListener { try { onClick() } catch(e:Exception) { DebugLog.add("ERROR",e.stackTraceToString()); Toast.makeText(this@BaseActivity,"Ошибка. Подробности в отладке",Toast.LENGTH_LONG).show() } } }, LinearLayout.LayoutParams(-1,dp(50)).apply { bottomMargin=dp(8) })
    }
    protected fun secondary(label: String, onClick: () -> Unit) {
        root.addView(Button(this).apply { text=label; isAllCaps=false; textSize=14f; setTextColor(white); background=rounded(panel2,14); setOnClickListener { try { onClick() } catch(e:Exception) { DebugLog.add("ERROR",e.stackTraceToString()); Toast.makeText(this@BaseActivity,"Ошибка. Подробности в отладке",Toast.LENGTH_LONG).show() } } }, LinearLayout.LayoutParams(-1,dp(50)).apply { bottomMargin=dp(8) })
    }
    protected fun field(hint: String, value: String = ""): EditText { val edit=EditText(this).apply { this.hint=hint; setText(value); setTextColor(white); setHintTextColor(Color.LTGRAY); setPadding(dp(12),dp(8),dp(12),dp(8)); background=rounded(panel2) }; root.addView(edit); return edit }
}
