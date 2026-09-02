package com.example.util

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class SignatureStyleOption(
    val id: String,
    val number: Int,
    val name: String,
    val category: String,
    val description: String,
    val fontFileName: String,
    val assetPath: String,
    val flourishType: FlourishType = FlourishType.EXECUTIVE_SWOOSH,
    val slantAngle: Float = 0.0f,
    val letterSpacing: Float = 0.02f,
    val strokeScale: Float = 1.0f
)

enum class FlourishType {
    EXECUTIVE_SWOOSH,
    DOUBLE_CURVE,
    LINEAR_DOT,
    TAPERED_LOOP,
    WAVE_BASELINE,
    INFINITY_TAIL,
    ACCENT_DASH,
    LOOPING_UNDERSCORE,
    TRIPLE_DOT_ORBIT,
    FEATHER_SWIRL,
    PRESIDENTIAL_ELLIPSE,
    CROWN_CREST,
    PARABOLIC_SURGE,
    SHARP_STRIKE,
    STAR_CROSS,
    ROYAL_S_CURVE,
    MAJESTIC_LOOP,
    DOUBLE_PARALLEL,
    ASCENDER_KNOT,
    RIBBON_BOW,
    MINIMAL_DOT,
    GEOMETRIC_LINE,
    BRUSH_STRIKE,
    CURSIVE_WHIP
}

object SignatureHelper {

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    // Canonical catalog mapping for existing asset font files to rich metadata
    private val knownFontCatalog = listOf(
        FontMeta("Sederahana.ttf", "Sederahana", "Simple", "Pure minimalist cursive with clean flowing lines"),
        FontMeta("TheCheckmate-Regular.ttf", "The Checkmate", "Formal", "Decisive formal signature for official documents"),
        FontMeta("WhisperingSignature.ttf", "Whispering Signature", "Flowing", "Whisper-light continuous cursive with delicate glide"),
        FontMeta("AmericansClassy-Italic.ttf", "Americans Classy Italic", "Calligraphy", "Expressive italic cursive with elegant flowing loops"),
        FontMeta("AmericansClassy-Regular.ttf", "Americans Classy", "Classic", "Refined copperplate script with timeless proportions"),
        FontMeta("Amsterdam Handwriting.ttf", "Amsterdam Handwriting", "Handwritten", "Authentic fluid penmanship with natural rhythm"),
        FontMeta("AuthorThink-Regular.ttf", "Author Think", "Executive", "Distinguished authorial autograph with crisp baseline"),
        FontMeta("Bastliga One.ttf", "Bastliga One", "Luxury", "Prestigious high-contrast signature for executive signoff"),
        FontMeta("Bearetta-Regular.ttf", "Bearetta", "Elegant", "Sophisticated graceful cursive with delicate flourishes"),
        FontMeta("BrittanySignatureScript.ttf", "Brittany Signature Script", "Signature", "Flowing organic signature script with graceful ligatures"),
        FontMeta("Cattalague.ttf", "Cattalague", "Artistic", "Artisan brush cursive with expressive flourishes"),
        FontMeta("Centralwell.ttf", "Centralwell", "Modern", "Contemporary streamlined script with balanced ascenders"),
        FontMeta("Cimlajin.ttf", "Cimlajin", "Creative", "Unique artistic calligraphy with modern dynamic flair"),
        FontMeta("Cintarini.ttf", "Cintarini", "Stylish", "Fashion-forward signature with sweeping ascender loops"),
        FontMeta("EagleHorizonP.ttf", "Eagle Horizon", "Bold", "Commanding executive signature with strong horizontal motion"),
        FontMeta("Gateway Signature.ttf", "Gateway Signature", "Executive", "Boardroom authority signature with sweeping flourish"),
        FontMeta("Handgone.ttf", "Handgone", "Handwritten", "Effortless casual handwriting with authentic ink feel"),
        FontMeta("Himalayan.ttf", "Himalayan", "Natural", "Free-flowing organic script with bold character peaks"),
        FontMeta("Hugh is Life Personal Use .ttf", "Hugh is Life", "Casual", "Relaxed personal autograph with playful flourishes"),
        FontMeta("Humble Signation.ttf", "Humble Signation", "Minimal", "Understated clean signature with maximum legibility"),
        FontMeta("Mollani-Regular.ttf", "Mollani", "Vintage", "Old-world manuscript cursive with antique charm"),
        FontMeta("MonsieurLaDoulaise-Regular.ttf", "Monsieur La Doulaise", "Royal", "Lavish French calligraphic script with ornate flourishes"),
        FontMeta("Purgatory.ttf", "Purgatory", "Expressive", "Edgy high-energy signature with swift exit stroke"),
        FontMeta("ROOSTER PERSONAL USE.ttf", "Rooster", "Dynamic", "Punchy vibrant signature with sharp confident cuts"),
        FontMeta("Rallomy-Regular.ttf", "Rallomy", "Delicate", "Slender romantic script with delicate loops"),
        FontMeta("Rockybilly.ttf", "Rockybilly", "Freehand", "Bold retro signature with energetic bouncy cadence"),
        FontMeta("Wistania.ttf", "Wistania", "Prestige", "Imperial royal cursive with distinguished baseline")
    )

    private data class FontMeta(
        val fileName: String,
        val displayName: String,
        val category: String,
        val description: String
    )

    @Volatile
    private var registeredStyles: List<SignatureStyleOption> = buildDefaultStyles()

    val styles: List<SignatureStyleOption>
        get() = registeredStyles

    val categories: List<String>
        get() {
            val distinctCats = registeredStyles.map { it.category }.distinct().sorted()
            return listOf("All") + distinctCats
        }

    /**
     * Build the initial styles list based on known local asset font files.
     */
    private fun buildDefaultStyles(): List<SignatureStyleOption> {
        return knownFontCatalog.mapIndexed { index, meta ->
            val num = index + 1
            val flourishType = FlourishType.entries[index % FlourishType.entries.size]
            SignatureStyleOption(
                id = "signature_$num",
                number = num,
                name = meta.displayName,
                category = meta.category,
                description = meta.description,
                fontFileName = meta.fileName,
                assetPath = "Fonts/${meta.fileName}",
                flourishType = flourishType,
                slantAngle = 0.0f,
                letterSpacing = 0.02f,
                strokeScale = 1.0f
            )
        }
    }

    /**
     * Initializes the signature font system by scanning assets and pre-caching typefaces.
     */
    fun initialize(context: Context) {
        scanAndRegisterFonts(context)
        // Pre-load typefaces in the background/eagerly
        registeredStyles.forEach { style ->
            getTypeface(context, style.assetPath)
        }
    }

    /**
     * Scans the assets/fonts directory for all available font files (.ttf, .otf, etc.)
     * and dynamically registers them as signature styles.
     */
    fun scanAndRegisterFonts(context: Context? = null): List<SignatureStyleOption> {
        val detectedFiles = mutableSetOf<String>()

        // 1. Scan via Android AssetManager if context is available
        if (context != null) {
            val assetManager = context.assets
            val candidateDirs = listOf("Fonts", "fonts", "assets/Fonts", "assets/fonts", "")
            for (dir in candidateDirs) {
                try {
                    val files = assetManager.list(dir) ?: emptyArray()
                    for (file in files) {
                        if (isFontFile(file)) {
                            val relativePath = if (dir.isEmpty()) file else "$dir/$file"
                            detectedFiles.add(relativePath)
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        // 2. Scan via Local File System (useful for Robolectric tests / JVM runtime)
        val fileSystemDirs = listOf(
            File("assets/Fonts"),
            File("assets/fonts"),
            File("../assets/Fonts"),
            File("../assets/fonts"),
            File("app/src/main/assets/Fonts"),
            File("app/src/main/assets/fonts")
        )
        for (dir in fileSystemDirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && isFontFile(file.name)) {
                        detectedFiles.add("Fonts/${file.name}")
                    }
                }
            }
        }

        // If no files detected dynamically (e.g. static preview without context), retain default styles
        if (detectedFiles.isEmpty()) {
            return registeredStyles
        }

        // Build dynamically registered styles preserving known metadata or deriving clean names
        val knownMap = knownFontCatalog.associateBy { it.fileName.lowercase() }
        val newStyles = detectedFiles.toList().sorted().mapIndexed { index, path ->
            val fileName = path.substringAfterLast('/')
            val num = index + 1
            val meta = knownMap[fileName.lowercase()]

            val name = meta?.displayName ?: formatDisplayNameFromFileName(fileName)
            val category = meta?.category ?: deriveCategoryFromFileName(fileName)
            val description = meta?.description ?: "Signature style crafted with local font $name"
            val flourishType = FlourishType.entries[index % FlourishType.entries.size]

            SignatureStyleOption(
                id = "signature_$num",
                number = num,
                name = name,
                category = category,
                description = description,
                fontFileName = fileName,
                assetPath = path,
                flourishType = flourishType,
                slantAngle = 0.0f,
                letterSpacing = 0.02f,
                strokeScale = 1.0f
            )
        }

        registeredStyles = newStyles
        return newStyles
    }

    private fun isFontFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc") || lower.endsWith(".woff")
    }

    /**
     * Converts raw font filename (e.g. "TheCheckmate-Regular.ttf", "AmericansClassy-Italic.ttf")
     * into a clean, human-friendly display name.
     */
    fun formatDisplayNameFromFileName(fileName: String): String {
        val base = fileName.substringAfterLast('/').substringBeforeLast('.')
        var cleaned = base
            .replace(Regex("(?i)\\b(personal\\s*use|commercial\\s*use|demo|free\\s*version|regular|otf|ttf|font)\\b"), " ")
            .replace('-', ' ')
            .replace('_', ' ')

        // Insert spaces before capital letters: "TheCheckmate" -> "The Checkmate"
        cleaned = cleaned.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        cleaned = cleaned.replace(Regex("(?<=[A-Z])(?=[A-Z][a-z])"), " ")

        val words = cleaned.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return base

        return words.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun deriveCategoryFromFileName(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.contains("italic") || lower.contains("calligraphy") -> "Calligraphy"
            lower.contains("script") -> "Script"
            lower.contains("hand") -> "Handwritten"
            lower.contains("classy") || lower.contains("classic") -> "Classic"
            lower.contains("modern") -> "Modern"
            lower.contains("luxury") || lower.contains("one") -> "Luxury"
            lower.contains("bold") || lower.contains("horizon") -> "Bold"
            lower.contains("flow") || lower.contains("whisper") -> "Flowing"
            lower.contains("minimal") || lower.contains("humble") -> "Minimal"
            lower.contains("executive") || lower.contains("gateway") || lower.contains("author") -> "Executive"
            lower.contains("royal") || lower.contains("wistania") -> "Royal"
            lower.contains("vintage") || lower.contains("mollani") -> "Vintage"
            lower.contains("art") || lower.contains("brush") -> "Artistic"
            else -> "Signature"
        }
    }

    /**
     * Resolves a signature style by ID, file name, or style name.
     */
    fun getStyleById(id: String): SignatureStyleOption {
        // Direct ID match
        styles.find { it.id.equals(id, ignoreCase = true) }?.let { return it }

        // Numbered signature index match (e.g. "signature_15")
        if (id.startsWith("signature_", ignoreCase = true)) {
            val num = id.substringAfter('_').toIntOrNull()
            if (num != null && styles.isNotEmpty()) {
                val index = (num - 1).coerceIn(0, styles.size - 1)
                return styles[index]
            }
        }

        // File name or asset path match
        styles.find {
            it.fontFileName.equals(id, ignoreCase = true) ||
            it.assetPath.equals(id, ignoreCase = true) ||
            it.assetPath.endsWith(id, ignoreCase = true)
        }?.let { return it }

        // Display name match
        styles.find { it.name.equals(id, ignoreCase = true) }?.let { return it }

        // Fallback to first available style from local assets
        return styles.firstOrNull() ?: buildDefaultStyles().first()
    }

    /**
     * Loads the Typeface for a signature font exclusively from local assets/fonts.
     * Includes graceful fallback to other local asset fonts on error.
     */
    fun getTypeface(context: Context?, assetPathOrFileName: String): Typeface? {
        val cacheKey = assetPathOrFileName.substringAfterLast('/')
        typefaceCache[cacheKey]?.let { return it }

        var typeface: Typeface? = null
        val fileName = assetPathOrFileName.substringAfterLast('/')

        // 1. Load via AssetManager
        if (context != null) {
            val assetManager: AssetManager = context.assets
            val candidatePaths = listOf(
                assetPathOrFileName,
                "Fonts/$fileName",
                "fonts/$fileName",
                "assets/Fonts/$fileName",
                "assets/fonts/$fileName",
                fileName
            ).distinct()

            for (path in candidatePaths) {
                try {
                    val tf = Typeface.createFromAsset(assetManager, path)
                    if (tf != null) {
                        typeface = tf
                        break
                    }
                } catch (_: Throwable) {}
            }
        }

        // 2. Load via File System (Robolectric / JVM Unit Tests)
        if (typeface == null) {
            val candidateFilePaths = listOf(
                "assets/Fonts/$fileName",
                "assets/fonts/$fileName",
                "../assets/Fonts/$fileName",
                "../assets/fonts/$fileName",
                "app/src/main/assets/Fonts/$fileName",
                "app/src/main/assets/fonts/$fileName",
                assetPathOrFileName
            )
            for (filePath in candidateFilePaths) {
                val file = File(filePath)
                if (file.exists() && file.isFile) {
                    try {
                        val tf = Typeface.createFromFile(file)
                        if (tf != null) {
                            typeface = tf
                            break
                        }
                    } catch (_: Throwable) {}
                }
            }
        }

        if (typeface != null) {
            typefaceCache[cacheKey] = typeface
            return typeface
        }

        // 3. Fallback Gracefully: Return the first successfully loaded local asset font
        val cachedFallback = typefaceCache.values.firstOrNull()
        if (cachedFallback != null) {
            return cachedFallback
        }

        // 4. Try loading the primary default font from local assets (Sederahana.ttf)
        if (fileName != "Sederahana.ttf") {
            val defaultTf = getTypeface(context, "Fonts/Sederahana.ttf")
            if (defaultTf != null) {
                return defaultTf
            }
        }

        return null
    }

    /**
     * Draw signature onto an Android Canvas using the selected local font asset.
     */
    fun drawSignatureOnCanvas(
        canvas: Canvas,
        name: String,
        styleId: String,
        centerX: Float,
        centerY: Float,
        color: Int = android.graphics.Color.BLACK,
        scale: Float = 1.0f,
        context: Context? = null
    ) {
        val displayName = if (name.isNotBlank()) name else "Authorized Signatory"
        val style = getStyleById(styleId)
        val typeface = getTypeface(context, style.assetPath)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = 32f * scale
            this.textAlign = Paint.Align.CENTER
            this.textSkewX = style.slantAngle
            this.letterSpacing = style.letterSpacing
            if (typeface != null) {
                this.typeface = typeface
            }
        }

        // Draw the name using the local signature font
        canvas.drawText(displayName, centerX, centerY, textPaint)

        // Draw matching subtle accent flourish / underline beneath signature
        val textWidth = textPaint.measureText(displayName)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.STROKE
            this.strokeWidth = (2.0f * style.strokeScale) * scale
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.FILL
        }

        val startX = centerX - (textWidth / 2f) - (6f * scale)
        val endX = centerX + (textWidth / 2f) + (10f * scale)
        val flourishY = centerY + (6f * scale)

        val path = Path()

        when (style.flourishType) {
            FlourishType.EXECUTIVE_SWOOSH -> {
                path.moveTo(startX, flourishY)
                path.cubicTo(
                    centerX - 20f * scale, flourishY + 10f * scale,
                    centerX + 20f * scale, flourishY - 6f * scale,
                    endX + 12f * scale, flourishY + 5f * scale
                )
                canvas.drawPath(path, strokePaint)
                canvas.drawCircle(endX + 18f * scale, flourishY + 4f * scale, 2.2f * scale, fillPaint)
            }
            FlourishType.DOUBLE_CURVE -> {
                path.moveTo(startX + 8f * scale, flourishY)
                path.quadTo(centerX, flourishY + 7f * scale, endX, flourishY - 2f * scale)
                canvas.drawPath(path, strokePaint)

                val path2 = Path()
                path2.moveTo(startX + 16f * scale, flourishY + 4f * scale)
                path2.quadTo(centerX + 8f * scale, flourishY + 9f * scale, endX - 8f * scale, flourishY + 2f * scale)
                canvas.drawPath(path2, strokePaint)
            }
            FlourishType.LINEAR_DOT -> {
                path.moveTo(startX, flourishY)
                path.lineTo(endX, flourishY)
                canvas.drawPath(path, strokePaint)
                canvas.drawCircle(endX + 6f * scale, flourishY, 2.5f * scale, fillPaint)
            }
            FlourishType.TAPERED_LOOP -> {
                path.moveTo(endX, flourishY - 3f * scale)
                path.cubicTo(
                    endX + 14f * scale, flourishY + 12f * scale,
                    centerX, flourishY + 14f * scale,
                    startX - 6f * scale, flourishY + 3f * scale
                )
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.WAVE_BASELINE -> {
                path.moveTo(startX, flourishY)
                path.cubicTo(
                    startX + (textWidth * 0.25f), flourishY - 5f * scale,
                    startX + (textWidth * 0.75f), flourishY + 7f * scale,
                    endX, flourishY
                )
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.INFINITY_TAIL -> {
                path.moveTo(startX, flourishY)
                path.lineTo(endX - 18f * scale, flourishY)
                path.cubicTo(
                    endX - 6f * scale, flourishY - 8f * scale,
                    endX + 14f * scale, flourishY + 8f * scale,
                    endX + 4f * scale, flourishY
                )
                path.cubicTo(
                    endX - 4f * scale, flourishY - 8f * scale,
                    endX + 14f * scale, flourishY - 6f * scale,
                    endX + 22f * scale, flourishY + 4f * scale
                )
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.ACCENT_DASH -> {
                canvas.drawLine(startX + 15f * scale, flourishY, endX - 15f * scale, flourishY, strokePaint)
                canvas.drawCircle(startX + 6f * scale, flourishY, 2.2f * scale, fillPaint)
                canvas.drawCircle(endX - 6f * scale, flourishY, 2.2f * scale, fillPaint)
            }
            FlourishType.LOOPING_UNDERSCORE -> {
                path.moveTo(startX, flourishY)
                path.lineTo(endX, flourishY)
                path.cubicTo(
                    endX + 10f * scale, flourishY,
                    endX + 14f * scale, flourishY + 8f * scale,
                    endX, flourishY + 8f * scale
                )
                path.lineTo(startX + 20f * scale, flourishY + 8f * scale)
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.TRIPLE_DOT_ORBIT -> {
                canvas.drawLine(startX, flourishY, endX - 18f * scale, flourishY, strokePaint)
                canvas.drawCircle(endX - 10f * scale, flourishY, 2.2f * scale, fillPaint)
                canvas.drawCircle(endX, flourishY, 2.8f * scale, fillPaint)
                canvas.drawCircle(endX + 10f * scale, flourishY, 2.2f * scale, fillPaint)
            }
            FlourishType.FEATHER_SWIRL -> {
                path.moveTo(startX + 10f * scale, flourishY)
                path.cubicTo(
                    centerX, flourishY - 6f * scale,
                    endX - 10f * scale, flourishY + 8f * scale,
                    endX + 18f * scale, flourishY - 4f * scale
                )
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.PRESIDENTIAL_ELLIPSE -> {
                path.moveTo(startX, flourishY)
                path.quadTo(centerX, flourishY + 12f * scale, endX + 16f * scale, flourishY)
                path.quadTo(centerX, flourishY - 10f * scale, startX + 10f * scale, flourishY + 4f * scale)
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.CROWN_CREST -> {
                canvas.drawLine(startX, flourishY, endX, flourishY, strokePaint)
                val peakY = flourishY - 6f * scale
                path.moveTo(centerX - 10f * scale, flourishY)
                path.lineTo(centerX - 5f * scale, peakY)
                path.lineTo(centerX, flourishY - 2f * scale)
                path.lineTo(centerX + 5f * scale, peakY)
                path.lineTo(centerX + 10f * scale, flourishY)
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.PARABOLIC_SURGE -> {
                path.moveTo(startX, flourishY + 4f * scale)
                path.quadTo(centerX, flourishY - 8f * scale, endX + 16f * scale, flourishY + 10f * scale)
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.SHARP_STRIKE -> {
                path.moveTo(startX, flourishY)
                path.lineTo(endX, flourishY)
                path.lineTo(endX + 10f * scale, flourishY - 6f * scale)
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.STAR_CROSS -> {
                canvas.drawLine(startX, flourishY, endX, flourishY, strokePaint)
                val starX = endX + 12f * scale
                canvas.drawLine(starX - 4f * scale, flourishY - 4f * scale, starX + 4f * scale, flourishY + 4f * scale, strokePaint)
                canvas.drawLine(starX - 4f * scale, flourishY + 4f * scale, starX + 4f * scale, flourishY - 4f * scale, strokePaint)
            }
            FlourishType.ROYAL_S_CURVE -> {
                path.moveTo(startX, flourishY)
                path.cubicTo(
                    centerX - 15f * scale, flourishY - 7f * scale,
                    centerX + 15f * scale, flourishY + 9f * scale,
                    endX + 10f * scale, flourishY
                )
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.MAJESTIC_LOOP -> {
                path.moveTo(startX, flourishY)
                path.lineTo(endX + 5f * scale, flourishY)
                path.cubicTo(
                    endX + 16f * scale, flourishY - 8f * scale,
                    endX + 16f * scale, flourishY + 10f * scale,
                    endX - 10f * scale, flourishY + 5f * scale
                )
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.DOUBLE_PARALLEL -> {
                canvas.drawLine(startX, flourishY, endX, flourishY, strokePaint)
                canvas.drawLine(startX + 12f * scale, flourishY + 4f * scale, endX - 12f * scale, flourishY + 4f * scale, strokePaint)
            }
            FlourishType.ASCENDER_KNOT -> {
                path.moveTo(startX, flourishY)
                path.lineTo(centerX, flourishY)
                path.cubicTo(
                    centerX + 10f * scale, flourishY - 10f * scale,
                    centerX + 15f * scale, flourishY + 8f * scale,
                    centerX + 25f * scale, flourishY
                )
                path.lineTo(endX, flourishY)
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.RIBBON_BOW -> {
                path.moveTo(startX, flourishY)
                path.quadTo(centerX, flourishY + 8f * scale, endX, flourishY)
                path.moveTo(centerX - 8f * scale, flourishY + 4f * scale)
                path.lineTo(centerX + 8f * scale, flourishY + 4f * scale)
                canvas.drawPath(path, strokePaint)
            }
            FlourishType.MINIMAL_DOT -> {
                canvas.drawCircle(centerX, flourishY + 2f * scale, 2.5f * scale, fillPaint)
            }
            FlourishType.GEOMETRIC_LINE -> {
                canvas.drawLine(startX + 5f * scale, flourishY, endX - 5f * scale, flourishY, strokePaint)
            }
            FlourishType.BRUSH_STRIKE -> {
                canvas.drawLine(startX, flourishY, endX, flourishY, strokePaint)
                canvas.drawLine(startX + 10f * scale, flourishY + 5f * scale, endX - 10f * scale, flourishY + 3f * scale, strokePaint)
            }
            FlourishType.CURSIVE_WHIP -> {
                path.moveTo(startX, flourishY)
                path.cubicTo(
                    centerX, flourishY + 10f * scale,
                    endX + 5f * scale, flourishY - 10f * scale,
                    endX + 20f * scale, flourishY + 6f * scale
                )
                canvas.drawPath(path, strokePaint)
            }
        }
    }
}

@Composable
fun OwnerSignatureDisplay(
    name: String,
    styleId: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF1E293B)
) {
    val displayName = if (name.isNotBlank()) name else "Gym Owner"
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        SignatureHelper.initialize(context)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            val scale = size.height / 48f
            val canvas = drawContext.canvas.nativeCanvas
            SignatureHelper.drawSignatureOnCanvas(
                canvas = canvas,
                name = displayName,
                styleId = styleId,
                centerX = size.width / 2f,
                centerY = size.height * 0.50f,
                color = android.graphics.Color.argb(
                    (color.alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                ),
                scale = scale.coerceIn(0.7f, 1.4f),
                context = context
            )
        }
    }
}
