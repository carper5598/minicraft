package com.example.minicraft

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class GameRenderer(private val input: InputState) : GLSurfaceView.Renderer {

    private val world = World()

    private var program = 0
    private var aPos = 0
    private var aUV = 0
    private var aShade = 0
    private var uMVP = 0
    private var uTex = 0
    private var texId = 0

    private var vbo: FloatBuffer? = null
    private var vCount = 0
    private var dirty = true

    private var lastT = 0L
    private var aspect = 1f

    private var px = 32.0
    private var py = 20.0
    private var pz = 32.0
    private var yaw = 0f
    private var pitch = 0f
    private var vy = 0.0
    private var onGround = false

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val mvp = FloatArray(16)

    private val halfW = 0.3
    private val height = 1.8
    private val eyeH = 1.62

    fun addLook(dx: Float, dy: Float) {
        yaw -= dx * 0.005f
        pitch -= dy * 0.005f
        pitch = pitch.coerceIn(-1.5f, 1.5f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.55f, 0.75f, 1.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        program = buildProgram()
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aUV = GLES20.glGetAttribLocation(program, "aUV")
        aShade = GLES20.glGetAttribLocation(program, "aShade")
        uMVP = GLES20.glGetUniformLocation(program, "uMVP")
        uTex = GLES20.glGetUniformLocation(program, "uTex")

        texId = loadTexture()
        dirty = true
        lastT = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        var dt = (now - lastT) / 1_000_000_000.0
        lastT = now
        if (dt > 0.05) dt = 0.05

        update(dt)

        if (dirty) {
            upload(world.buildMesh())
            dirty = false
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        Matrix.perspectiveM(proj, 0, 70f, aspect, 0.1f, 100f)
        val ex = px
        val ey = py + eyeH
        val ez = pz
        val d = dir()
        Matrix.setLookAtM(
            view, 0,
            ex.toFloat(), ey.toFloat(), ez.toFloat(),
            (ex + d[0]).toFloat(), (ey + d[1]).toFloat(), (ez + d[2]).toFloat(),
            0f, 1f, 0f
        )
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(uTex, 0)

        val b = vbo
        if (b != null && vCount > 0) {
            val stride = 6 * 4
            b.position(0)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, stride, b)
            b.position(3)
            GLES20.glEnableVertexAttribArray(aUV)
            GLES20.glVertexAttribPointer(aUV, 2, GLES20.GL_FLOAT, false, stride, b)
            b.position(5)
            GLES20.glEnableVertexAttribArray(aShade)
            GLES20.glVertexAttribPointer(aShade, 1, GLES20.GL_FLOAT, false, stride, b)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount)
            GLES20.glDisableVertexAttribArray(aPos)
            GLES20.glDisableVertexAttribArray(aUV)
            GLES20.glDisableVertexAttribArray(aShade)
        }
    }

    private fun update(dt: Double) {
        if (input.breakRequested) {
            input.breakRequested = false
            raycast()?.let {
                val type = world.getBlock(it.x, it.y, it.z).toInt()
                if (type in 1..3) {
                    world.setBlock(it.x, it.y, it.z, 0)
                    input.inventory.incrementAndGet(type)
                    dirty = true
                }
            }
        }

        if (input.placeRequested) {
            input.placeRequested = false
            raycast()?.let {
                val slot = input.selectedSlot
                if (slot in 1..3 &&
                    input.inventory.get(slot) > 0 &&
                    !overlapsPlayer(it.px, it.py, it.pz)
                ) {
                    world.setBlock(it.px, it.py, it.pz, slot.toByte())
                    input.inventory.decrementAndGet(slot)
                    dirty = true
                }
            }
        }

        var fx = 0.0; var fz = 0.0
        if (input.moveForward) { fx += -sin(yaw.toDouble()); fz += -cos(yaw.toDouble()) }
        if (input.moveBack)    { fx +=  sin(yaw.toDouble()); fz +=  cos(yaw.toDouble()) }
        if (input.moveRight)   { fx +=  cos(yaw.toDouble()); fz += -sin(yaw.toDouble()) }
        if (input.moveLeft)    { fx += -cos(yaw.toDouble()); fz +=  sin(yaw.toDouble()) }
        val l = sqrt(fx * fx + fz * fz)
        if (l > 0.0) { fx /= l; fz /= l }

        val sp = 5.0
        val dx = fx * sp * dt
        val dz = fz * sp * dt
        px += dx; if (collides()) px -= dx
        pz += dz; if (collides()) pz -= dz

        if (input.jump) {
            if (onGround) { vy = 8.0; onGround = false }
            input.jump = false
        }
        vy -= 20.0 * dt
        py += vy * dt
        if (collides()) {
            py -= vy * dt
            if (vy < 0.0) onGround = true
            vy = 0.0
        } else onGround = false

        if (py < -20.0) { px = 32.0; py = 20.0; pz = 32.0; vy = 0.0 }
    }

    private fun overlapsPlayer(bx: Int, by: Int, bz: Int): Boolean {
        val mnX = px - halfW; val mxX = px + halfW
        val mnY = py; val mxY = py + height
        val mnZ = pz - halfW; val mxZ = pz + halfW
        val bMnX = bx.toDouble(); val bMxX = bx + 1.0
        val bMnY = by.toDouble(); val bMxY = by + 1.0
        val bMnZ = bz.toDouble(); val bMxZ = bz + 1.0
        return mnX < bMxX && mxX > bMnX &&
               mnY < bMxY && mxY > bMnY &&
               mnZ < bMxZ && mxZ > bMnZ
    }

    private fun collides(): Boolean {
        val mnX = floor(px - halfW).toInt(); val mxX = floor(px + halfW).toInt()
        val mnY = floor(py).toInt(); val mxY = floor(py + height).toInt()
        val mnZ = floor(pz - halfW).toInt(); val mxZ = floor(pz + halfW).toInt()
        for (x in mnX..mxX) for (y in mnY..mxY) for (z in mnZ..mxZ)
            if (world.isSolid(x, y, z)) return true
        return false
    }

    private fun dir(): FloatArray {
        val cp = cos(pitch.toDouble()).toFloat()
        val sp = sin(pitch.toDouble()).toFloat()
        val cy = cos(yaw.toDouble()).toFloat()
        val sy = sin(yaw.toDouble()).toFloat()
        return floatArrayOf(-sy * cp, sp, -cy * cp)
    }

    private data class Hit(val x: Int, val y: Int, val z: Int,
                           val px: Int, val py: Int, val pz: Int)

    private fun raycast(): Hit? {
        val ex = px; val ey = py + eyeH; val ez = pz
        val d = dir()
        var lx = floor(ex).toInt(); var ly = floor(ey).toInt(); var lz = floor(ez).toInt()
        var t = 0.0
        while (t < 6.0) {
            val bx = floor(ex + d[0] * t).toInt()
            val by = floor(ey + d[1] * t).toInt()
            val bz = floor(ez + d[2] * t).toInt()
            if (bx != lx || by != ly || bz != lz) {
                if (world.isSolid(bx, by, bz)) return Hit(bx, by, bz, lx, ly, lz)
                lx = bx; ly = by; lz = bz
            }
            t += 0.05
        }
        return null
    }

    private fun upload(data: FloatArray) {
        vCount = data.size / 6
        val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(data)
        fb.position(0)
        vbo = fb
    }

    private fun loadTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val id = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, world.atlas, 0)
        return id
    }

    private fun buildProgram(): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, VS)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, FS)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return p
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        return s
    }

    companion object {
        private const val VS = """
            attribute vec3 aPos;
            attribute vec2 aUV;
            attribute float aShade;
            uniform mat4 uMVP;
            varying vec2 vUV;
            varying float vShade;
            void main() {
                gl_Position = uMVP * vec4(aPos, 1.0);
                vUV = aUV;
                vShade = aShade;
            }
        """
        private const val FS = """
            precision mediump float;
            varying vec2 vUV;
            varying float vShade;
            uniform sampler2D uTex;
            void main() {
                vec4 c = texture2D(uTex, vUV);
                gl_FragColor = vec4(c.rgb * vShade, 1.0);
            }
        """
    }
}
