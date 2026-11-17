package com.nabd.wallpaper

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var btnAudio: Button
    private lateinit var btnTouchMode: Button

    private val requestAudioPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateAudioButton()
        Toast.makeText(
            this,
            if (granted) "تم التفعيل! شغّل أي موسيقى في الموبايل والخلفيّة هتتفاعل معاها 🎵"
            else "تم رفض الإذن — يمكنك التفعيل لاحقاً",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("nabd_prefs", Context.MODE_PRIVATE)

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#06060D"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(38, 64, 38, 64)
        }
        rootScroll.addView(container)

        container.addView(TextView(this).apply {
            text = "نبض النقط — خلفية تفاعلية"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })
        container.addView(TextView(this).apply {
            text = "تعمل 100% بدون نت • تتفاعل مع موسيقى الهاتف"
            textSize = 13f
            setTextColor(Color.parseColor("#9CA3AF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 28)
        })

        container.addView(styledButton("🖼️ تعيين كخلفية متحركة للشاشة الآن", "#7C5CFF", "#38BDF8") {
            applyLiveWallpaper()
        }.apply {
            setPadding(24, 32, 24, 32)
        })

        btnAudio = styledButton("", "#1E2238", "#2B304D") {
            requestAudioPerm.launch(Manifest.permission.RECORD_AUDIO)
        }
        updateAudioButton()
        container.addView(btnAudio)

        val shapeCard = makeCard("النص والأشكال")
        val input = EditText(this).apply {
            hint = "اكتب اسمك أو كلمتك..."
            setText(prefs.getString("text", "أحمد"))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.parseColor("#0D0F1C"))
            setPadding(28, 24, 28, 24)
        }
        shapeCard.addView(input)
        shapeCard.addView(styledButton("تحويل الكلمة لنقط ✨", "#5B44D6", "#2E90FA") {
            val t = input.text.toString().ifBlank { "أحمد" }
            prefs.edit().putString("shape", "text").putString("text", t).apply()
            Toast.makeText(this, "تم تحويل: $t", Toast.LENGTH_SHORT).show()
        })

        val shapes = listOf("sphere" to "🌐 كرة", "heart" to "❤️ قلب", "star" to "⭐ نجمة", "wave" to "🌊 موجة", "butterfly" to "🦋 فراشة", "rose" to "🌹 وردة")
        shapeCard.addView(buildChipRow(shapes) { code ->
            prefs.edit().putString("shape", code).apply()
        })
        container.addView(shapeCard)

        val pixelCard = makeCard("البكسلات والحركة والألوان")
        pixelCard.addView(makeSlider("عدد البكسلات", 600, 4000, prefs.getInt("count", 2200)) { v ->
            prefs.edit().putInt("count", v).apply()
        })
        pixelCard.addView(makeSlider("سرعة الحركة (%)", 40, 220, (prefs.getFloat("speed", 1.0f) * 100).toInt()) { v ->
            prefs.edit().putFloat("speed", v / 100f).apply()
        })
        pixelCard.addView(makeSlider("اهتزاز النقط (%)", 0, 250, (prefs.getFloat("vib", 0.9f) * 100).toInt()) { v ->
            prefs.edit().putFloat("vib", v / 100f).apply()
        })
        pixelCard.addView(makeSlider("حجم النقطة (%)", 60, 220, (prefs.getFloat("size", 1.1f) * 100).toInt()) { v ->
            prefs.edit().putFloat("size", v / 100f).apply()
        })
        pixelCard.addView(makeSlider("حساسية الموسيقى (%)", 50, 300, (prefs.getFloat("music_sens", 1.6f) * 100).toInt()) { v ->
            prefs.edit().putFloat("music_sens", v / 100f).apply()
        })

        val isRepel = prefs.getBoolean("touch_repel", true)
        btnTouchMode = styledButton(if (isRepel) "وضع اللمس: هروب من الصباع 🖐️" else "وضع اللمس: جذب للصباع 🧲", "#1E2238", "#2B304D") {
            val next = !prefs.getBoolean("touch_repel", true)
            prefs.edit().putBoolean("touch_repel", next).apply()
            btnTouchMode.text = if (next) "وضع اللمس: هروب من الصباع 🖐️" else "وضع اللمس: جذب للصباع 🧲"
        }
        pixelCard.addView(btnTouchMode)

        val palettes = listOf("tcw" to "الريل", "gold" to "ذهبي", "love" to "رومانسي", "emerald" to "زمردي", "rainbow" to "قوس قزح")
        pixelCard.addView(buildChipRow(palettes) { pal ->
            prefs.edit().putString("palette", pal).apply()
        })
        pixelCard.addView(makeSlider("لون مخصص (0-360°)", 0, 360, prefs.getFloat("hue", 245f).toInt()) { v ->
            prefs.edit().putString("palette", "custom").putFloat("hue", v.toFloat()).apply()
        })
        container.addView(pixelCard)

        setContentView(rootScroll)
    }

    private fun updateAudioButton() {
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        btnAudio.text = if (ok) "✅ سماع موسيقى الهاتف مفعّل (بدون تسجيل)" else "🎵 تفعيل سماع موسيقى الخلفية (اضغط هنا)"
    }

    private fun applyLiveWallpaper() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, ParticleWallpaperService::class.java))
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (_: Exception) {
                Toast.makeText(this, "من إعدادات الموبايل افتح الخلفيات الحية واختر التطبيق", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun makeCard(title: String): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111322"))
                cornerRadius = 32f
                setStroke(2, Color.parseColor("#23263E"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 18, 0, 10)
            }
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(Color.parseColor("#C4B5FD"))
            setPadding(0, 0, 0, 14)
        })
        return card
    }

    private fun styledButton(label: String, c1: String, c2: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor(c1), Color.parseColor(c2))).apply {
                cornerRadius = 26f
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 10, 0, 10)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun buildChipRow(items: List<Pair<String, String>>, onSelect: (String) -> Unit): HorizontalScrollView {
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 6) }
        for ((code, title) in items) {
            row.addView(Button(this).apply {
                text = title
                textSize = 12f
                setTextColor(Color.WHITE)
                isAllCaps = false
                setPadding(24, 10, 24, 10)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1B1F36"))
                    cornerRadius = 50f
                    setStroke(2, Color.parseColor("#32385A"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 14, 0)
                }
                setOnClickListener {
                    onSelect(code)
                    Toast.makeText(this@MainActivity, "تم اختيار: $title", Toast.LENGTH_SHORT).show()
                }
            })
        }
        scroll.addView(row)
        return scroll
    }

    private fun makeSlider(label: String, min: Int, max: Int, initial: Int, onChange: (Int) -> Unit): View {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 8, 0, 8) }
        val tv = TextView(this).apply {
            text = "$label: $initial"
            textSize = 13f
            setTextColor(Color.parseColor("#D1D5DB"))
        }
        val sb = SeekBar(this).apply {
            this.max = max - min
            progress = (initial - min).coerceIn(0, this.max)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = p + min
                    tv.text = "$label: $v"
                    if (fromUser) onChange(v)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        wrap.addView(tv)
        wrap.addView(sb)
        return wrap
    }
}
