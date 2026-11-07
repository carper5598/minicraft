package com.example.minicraft

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

class World(
    val sx: Int = 64,
    val sy: Int = 32,
    val sz: Int = 64
) {
    private val blocks = ByteArray(sx * sy * sz)

    // Атлас: 2x2 тайла по 16x16, увеличенные до 64x64 каждый.
    // 0 — трава сверху, 1 — земля, 2 — камень, 3 — трава сбоку.
    val atlas: Bitmap = buildAtlas()
    private val tiles = 2
    private val tileSizePx = 64

    init {
        generate()
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
                    val type: Byte = when {
                        y == h -> 1
                        y > h - 3 -> 2
                        else -> 3
                    }
                    setBlock(x, y, z, type)
                }
            }
        }
    }

    // uv тайла в атласе по индексу
    private fun tileUV(tile: Int): FloatArray {
        val tx = tile % tiles
        val ty = tile / tiles
        val u0 = tx.toFloat() / tiles
        val v0 = ty.toFloat() / tiles
        val u1 = (tx + 1).toFloat() / tiles
        val v1 = (ty + 1).toFloat() / tiles
        return floatArrayOf(u0, v0, u1, v1)
    }

    fun buildMesh(): FloatArray {
        val data = ArrayList<Float>(600_000)
        val order = intArrayOf(0, 1, 2, 0, 2, 3)

        // каждая грань: 4 вершины xyz
        val top = floatArrayOf(0f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f)
        val bottom = floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 0f)
        val posX = floatArrayOf(1f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f)
        val negX = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 0f, 0f)
        val posZ = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 0f, 1f)
        val negZ = floatArrayOf(1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f)

        // uv для каждой грани (0..1)
        val faceUV = floatArrayOf(
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f
        )

        fun addFace(
            x: Int, y: Int, z: Int,
            offsets: FloatArray,
            tile: Int,
            shade: Float
        ) {
            val uv = tileUV(tile)
            val u0 = uv[0]
            val v0 = uv[1]
            val u1 = uv[2]
            val v1 = uv[3]

            for (i in order) {
                val o = i * 3
                data.add(x + offsets[o])
                data.add(y + offsets[o + 1])
                data.add(z + offsets[o + 2])

                val uu = faceUV[i * 2]
                val vv = faceUV[i * 2 + 1]
                val u = u0 + (u1 - u0) * uu
                val v = v0 + (v1 - v0) * vv
                data.add(u)
                data.add(v)

                data.add(shade)
            }
        }

        for (y in 0 until sy) {
            for (z in 0 until sz) {
                for (x in 0 until sx) {
                    val block = getBlock(x, y, z)
                    if (block == 0.toByte()) continue

                    val topTile: Int
                    val sideTile: Int
                    val bottomTile: Int
                    when (block.toInt()) {
                        1 -> { topTile = 0; sideTile = 3; bottomTile = 1 } // трава
                        2 -> { topTile = 1; sideTile = 1; bottomTile = 1 } // земля
                        else -> { topTile = 2; sideTile = 2; bottomTile = 2 } // камень
                    }

                    if (getBlock(x, y + 1, z) == 0.toByte()) addFace(x, y, z, top, topTile, 1.0f)
                    if (getBlock(x, y - 1, z) == 0.toByte()) addFace(x, y, z, bottom, bottomTile, 0.5f)
                    if (getBlock(x + 1, y, z) == 0.toByte()) addFace(x, y, z, posX, sideTile, 0.8f)
                    if (getBlock(x - 1, y, z) == 0.toByte()) addFace(x, y, z, negX, sideTile, 0.8f)
                    if (getBlock(x, y, z + 1) == 0.toByte()) addFace(x, y, z, posZ, sideTile, 0.7f)
                    if (getBlock(x, y, z - 1) == 0.toByte()) addFace(x, y, z, negZ, sideTile, 0.7f)
                }
            }
        }

        val result = FloatArray(data.size)
        for (i in data.indices) result[i] = data[i]
        return result
    }

    private fun buildAtlas(): Bitmap {
        val tileSize = tileSizePx
        val size = tileSize * tiles
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint()

        // tile 0 — трава сверху
        drawGrass(c, p, 0, 0, tileSize, top = true)
        // tile 1 — земля
        drawDirt(c, p, tileSize, 0, tileSize)
        // tile 2 — камень
        drawStone(c, p, 0, tileSize, tileSize)
        // tile 3 — трава сбоку (земля + зелёная полоса сверху)
        drawGrass(c, p, tileSize, tileSize, tileSize, top = false)

        return bmp
    }

    private fun drawDirt(c: Canvas, p: Paint, ox: Int, oy: Int, size: Int) {
        val base = Color.rgb(120, 85, 50)
        c.drawRect(ox.toFloat(), oy.toFloat(), (ox + size).toFloat(), (oy + size).toFloat(), p.apply {
            color = base
            style = Paint.Style.FILL
        })

        val rnd = java.util.Random(42L)
        repeat(120) {
            val x = ox + rnd.nextInt(size)
            val y = oy + rnd.nextInt(size)
            val shade = rnd.nextInt(60) - 30
            val col = Color.rgb(
                (120 + shade).coerceIn(0, 255),
                (85 + shade).coerceIn(0, 255),
                (50 + shade).coerceIn(0, 255)
            )
            p.color = col
            val s = 2 + rnd.nextInt(4)
            c.drawRect(x.toFloat(), y.toFloat(), (x + s).toFloat(), (y + s).toFloat(), p)
        }
    }

    private fun drawGrass(c: Canvas, p: Paint, ox: Int, oy: Int, size: Int, top: Boolean) {
        // сначала земля
        drawDirt(c, p, ox, oy, size)

        if (top) {
            val rnd = java.util.Random(7L)
            repeat(160) {
                val x = ox + rnd.nextInt(size)
                val y = oy + rnd.nextInt(size)
                val shade = rnd.nextInt(60) - 20
                val col = Color.rgb(
                    (60 + shade).coerceIn(0, 255),
                    (140 + shade).coerceIn(0, 255),
                    (60 + shade).coerceIn(0, 255)
                )
                p.color = col
                val s = 2 + rnd.nextInt(4)
                c.drawRect(x.toFloat(), y.toFloat(), (x + s).toFloat(), (y + s).toFloat(), p)
            }
        } else {
            // верхняя зелёная полоса
            val bandH = size / 4
            val rnd = java.util.Random(9L)
            repeat(100) {
                val x = ox + rnd.nextInt(size)
                val y = oy + rnd.nextInt(bandH)
                val shade = rnd.nextInt(50) - 15
                val col = Color.rgb(
                    (60 + shade).coerceIn(0, 255),
                    (140 + shade).coerceIn(0, 255),
                    (60 + shade).coerceIn(0, 255)
                )
                p.color = col
                val s = 2 + rnd.nextInt(4)
                c.drawRect(x.toFloat(), y.toFloat(), (x + s).toFloat(), (y + s).toFloat(), p)
            }
            // рваная граница
            p.color = Color.rgb(70, 150, 70)
            val step = size / 8
            for (i in 0 until 8) {
                val x = ox + i * step
                val y = oy + bandH - rnd.nextInt(step)
                c.drawRect(x.toFloat(), y.toFloat(), (x + step).toFloat(), (y + step / 2).toFloat(), p)
            }
        }
    }

    private fun drawStone(c: Canvas, p: Paint, ox: Int, oy: Int, size: Int) {
        p.color = Color.rgb(128, 128, 128)
        p.style = Paint.Style.FILL
        c.drawRect(ox.toFloat(), oy.toFloat(), (ox + size).toFloat(), (oy + size).toFloat(), p)

        val rnd = java.util.Random(1234L)
        repeat(150) {
            val x = ox + rnd.nextInt(size)
            val y = oy + rnd.nextInt(size)
            val shade = rnd.nextInt(80) - 40
            val col = Color.rgb(
                (128 + shade).coerceIn(0, 255),
                (128 + shade).coerceIn(0, 255),
                (128 + shade).coerceIn(0, 255)
            )
            p.color = col
            val s = 2 + rnd.nextInt(5)
            c.drawRect(x.toFloat(), y.toFloat(), (x + s).toFloat(), (y + s).toFloat(), p)
        }
    }
}
