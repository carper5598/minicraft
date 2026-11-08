package com.example.minicraft

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

class World(val sx: Int = 64, val sy: Int = 32, val sz: Int = 64) {

    private val blocks = ByteArray(sx * sy * sz)

    // ВАЖНО: tiles и tilePx объявлены ДО atlas,
    // иначе buildAtlas() видит нули и падает с "width and height must be > 0"
    private val tiles = 2
    private val tilePx = 64

    val atlas: Bitmap = buildAtlas()

    init {
        // generate() только для нового мира; если blocks уже заполнены
        // (загрузка из файла через load()), не трогаем.
        if (blocks.all { it == 0.toByte() }) generate()
    }

    constructor(file: File) : this() {
        if (file.exists() && file.length() > 0) {
            load(file)
        }
    }

    private fun idx(x: Int, y: Int, z: Int): Int = (y * sz + z) * sx + x

    fun getBlock(x: Int, y: Int, z: Int): Byte {
        if (x < 0 || x >= sx || y < 0 || y >= sy || z < 0 || z >= sz) return 0
        return blocks[idx(x, y, z)]
    }

    fun isSolid(x: Int, y: Int, z: Int): Boolean {
        if (y < 0) return true
        if (y >= sy) return false
        return getBlock(x, y, z) != 0.toByte()
    }

    fun setBlock(x: Int, y: Int, z: Int, type: Byte) {
        if (x in 0 until sx && y in 0 until sy && z in 0 until sz) {
            blocks[idx(x, y, z)] = type
        }
    }

    private fun generate() {
        for (x in 0 until sx) {
            for (z in 0 until sz) {
                val h = 8 + (sin(x * 0.25) * 2.0 + cos(z * 0.3) * 2.0).toInt()
                for (y in 0..h) {
                    val t: Byte = when {
                        y == h -> 1
                        y > h - 3 -> 2
                        else -> 3
                    }
                    setBlock(x, y, z, t)
                }
            }
        }
    }

    fun save(file: File) {
        file.parentFile?.mkdirs()
        DataOutputStream(file.outputStream().buffered()).use { out ->
            out.writeInt(sx)
            out.writeInt(sy)
            out.writeInt(sz)
            out.write(blocks)
        }
    }

    private fun load(file: File) {
        try {
            DataInputStream(file.inputStream().buffered()).use { inp ->
                val fx = inp.readInt()
                val fy = inp.readInt()
                val fz = inp.readInt()
                if (fx != sx || fy != sy || fz != sz) {
                    // Несовпадение размеров — генерируем новый мир.
                    generate()
                    return
                }
                inp.readFully(blocks)
            }
        } catch (e: Exception) {
            // Повреждённый файл — генерируем новый.
            generate()
        }
    }

    fun buildMesh(): FloatArray {
        // 6 float на вершину: x, y, z, u, v, shade
        val out = FloatArray(2_000_000)
        var n = 0

        fun put(x: Float, y: Float, z: Float, u: Float, v: Float, s: Float) {
            if (n + 6 > out.size) return
            out[n++] = x
            out[n++] = y
            out[n++] = z
            out[n++] = u
            out[n++] = v
            out[n++] = s
        }

        fun u(tile: Int, i: Int): Float {
            val tx = tile % tiles
            val du = 1f / tiles
            val uu = floatArrayOf(0f, 1f, 1f, 0f)
            return tx * du + uu[i] * du
        }

        fun v(tile: Int, i: Int): Float {
            val ty = tile / tiles
            val dv = 1f / tiles
            val vv = floatArrayOf(0f, 0f, 1f, 1f)
            return ty * dv + vv[i] * dv
        }

        val order = intArrayOf(0, 1, 2, 0, 2, 3)

        for (y in 0 until sy) {
            for (z in 0 until sz) {
                for (x in 0 until sx) {
                    val b = getBlock(x, y, z)
                    if (b == 0.toByte()) continue

                    val topTile: Int
                    val sideTile: Int
                    val botTile: Int
                    when (b.toInt()) {
                        1 -> { topTile = 0; sideTile = 3; botTile = 1 }
                        2 -> { topTile = 1; sideTile = 1; botTile = 1 }
                        else -> { topTile = 2; sideTile = 2; botTile = 2 }
                    }

                    // верх (+Y)
                    if (!isSolid(x, y + 1, z)) {
                        val verts = arrayOf(
                            intArrayOf(0, 1, 0), intArrayOf(1, 1, 0),
                            intArrayOf(1, 1, 1), intArrayOf(0, 1, 1)
                        )
                        for (i in order) {
                            val w = verts[i]
                            put(
                                (x + w[0]).toFloat(), (y + w[1]).toFloat(), (z + w[2]).toFloat(),
                                u(topTile, i), v(topTile, i), 1.0f
                            )
                        }
                    }
                    // низ (-Y)
                    if (!isSolid(x, y - 1, z)) {
                        val verts = arrayOf(
                            intArrayOf(0, 0, 0), intArrayOf(0, 0, 1),
                            intArrayOf(1, 0, 1), intArrayOf(1, 0, 0)
                        )
                        for (i in order) {
                            val w = verts[i]
                            put(
                                (x + w[0]).toFloat(), (y + w[1]).toFloat(), (z + w[2]).toFloat(),
                                u(botTile, i), v(botTile, i), 0.5f
                            )
                        }
                    }
                    // +X
                    if (!isSolid(x + 1, y, z)) {
                        val verts = arrayOf(
                            intArrayOf(1, 0, 0), intArrayOf(1, 1, 0),
                            intArrayOf(1, 1, 1), intArrayOf(1, 0, 1)
                        )
                        for (i in order) {
                            val w = verts[i]
                            put(
                                (x + w[0]).toFloat(), (y + w[1]).toFloat(), (z + w[2]).toFloat(),
                                u(sideTile, i), v(sideTile, i), 0.8f
                            )
                        }
                    }
                    // -X
                    if (!isSolid(x - 1, y, z)) {
                        val verts = arrayOf(
                            intArrayOf(0, 0, 1), intArrayOf(0, 1, 1),
                            intArrayOf(0, 1, 0), intArrayOf(0, 0, 0)
                        )
                        for (i in order) {
                            val w = verts[i]
                            put(
                                (x + w[0]).toFloat(), (y + w[1]).toFloat(), (z + w[2]).toFloat(),
                                u(sideTile, i), v(sideTile, i), 0.8f
                            )
                        }
                    }
                    // +Z
                    if (!isSolid(x, y, z + 1)) {
                        val verts = arrayOf(
                            intArrayOf(0, 0, 1), intArrayOf(0, 1, 1),
                            intArrayOf(1, 1, 1), intArrayOf(1, 0, 1)
                        )
                        for (i in order) {
                            val w = verts[i]
                            put(
                                (x + w[0]).toFloat(), (y + w[1]).toFloat(), (z + w[2]).toFloat(),
                                u(sideTile, i), v(sideTile, i), 0.7f
                            )
                        }
                    }
                    // -Z
                    if (!isSolid(x, y, z - 1)) {
                        val verts = arrayOf(
                            intArrayOf(1, 0, 0), intArrayOf(1, 1, 0),
                            intArrayOf(0, 1, 0), intArrayOf(0, 0, 0)
                        )
                        for (i in order) {
                            val w = verts[i]
                            put(
                                (x + w[0]).toFloat(), (y + w[1]).toFloat(), (z + w[2]).toFloat(),
                                u(sideTile, i), v(sideTile, i), 0.7f
                            )
                        }
                    }
                }
            }
        }

        val result = FloatArray(n)
        System.arraycopy(out, 0, result, 0, n)
        return result
    }

    private fun buildAtlas(): Bitmap {
        val size = tilePx * tiles
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint()

        // tile 0 — трава сверху
        drawGrassTop(c, p, 0, 0, tilePx)
        // tile 1 — земля
        drawDirt(c, p, tilePx, 0, tilePx)
        // tile 2 — камень
        drawStone(c, p, 0, tilePx, tilePx)
        // tile 3 — трава сбоку
        drawGrassSide(c, p, tilePx, tilePx, tilePx)

        return bmp
    }

    private fun drawDirt(c: Canvas, p: Paint, ox: Int, oy: Int, size: Int) {
        p.color = Color.rgb(120, 85, 50)
        p.style = Paint.Style.FILL
        c.drawRect(ox.toFloat(), oy.toFloat(), (ox + size).toFloat(), (oy + size).toFloat(), p)

        val r = Random(42L)
        repeat(80) {
            val x = ox + r.nextInt(size)
            val y = oy + r.nextInt(size)
            val sh = r.nextInt(50) - 25
            p.color = Color.rgb(
                (120 + sh).coerceIn(0, 255),
                (85 + sh).coerceIn(0, 255),
                (50 + sh).coerceIn(0, 255)
            )
            val s = 2 + r.nextInt(4)
            c.drawRect(x.toFloat(), y.toFloat(), (x + s).toFloat(), (y + s).toFloat(), p)
        }
    }

    private fun drawGrassTop(c: Canvas, p: Paint, ox: Int, oy: Int, size: Int) {
        p.color = Color.rgb(60, 140, 60)
        p.style = Paint.Style.FILL
        c.drawRect(ox.toFloat(), oy.toFloat(), (ox + size).toFloat(), (oy + size).toFloat(), p)

        val r = Random(7L)
        repeat(120) {
            val x = ox + r.nextInt(size)
            val y = oy + r.nextInt(size)
            val sh = r.nextInt(40) - 15
            p.color = Color.rgb(
                (60 + sh).coerceIn(0, 255),
                (140 + sh).coerceIn(0, 255),
                (60 + sh).coerceIn(0, 255)
            )
            val s = 2 + r.nextInt(3)
            c.drawRect(x.toFloat(), y.toFloat(), (x + s).toFloat(), (y + s).toFloat(), p)
        }
    }

    private fun drawStone(c: Canvas, p: Paint, ox: Int, oy: Int, size: Int) {
        p.color = Color.rgb(128, 128, 128)
        p.style = Paint.Style.FILL
        c.drawRect(ox.toFloat(), oy.toFloat(), (ox + size).toFloat(), (oy + size).toFloat(), p)

        val r = Random(1234L)
        repeat(100) {
            val x = ox + r.nextInt(size)
            val y = oy + r.nextInt(size)
            val sh = r.nextInt(80) - 40
            p.color = Color.rgb(
                (128 + sh).coerceIn(0, 255),
                (128 + sh).coerceIn(0, 255),
                (128 + sh).coerceIn(0, 255)
            )
            val s = 2 + r.nextInt(5)
            c.drawRect(x.toFloat(), y.toFloat(), (x + s).toFloat(), (y + s).toFloat(), p)
        }
    }

    private fun drawGrassSide(c: Canvas, p: Paint, ox: Int, oy: Int, size: Int) {
        drawDirt(c, p, ox, oy, size)

        val bandH = size / 4
        val r = Random(9L)
        p.color = Color.rgb(60, 140, 60)
        p.style = Paint.Style.FILL
        c.drawRect(ox.toFloat(), oy.toFloat(), (ox + size).toFloat(), (oy + bandH).toFloat(), p)

        repeat(60) {
            val x = ox + r.nextInt(size)
            val y = oy + r.nextInt(bandH)
            val sh = r.nextInt(40) - 15
            p.color = Color.rgb(
                (60 + sh).coerceIn(0, 255),
                (140 + sh).coerceIn(0, 255),
                (60 + sh).coerceIn(0, 255)
            )
            val s = 2 + r.nextInt(3)
            c.drawRect(x.toFloat(), y.toFloat(), (x + s).toFloat(), (y + s).toFloat(), p)
        }
    }
}
