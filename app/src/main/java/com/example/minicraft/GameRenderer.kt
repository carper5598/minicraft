package com.example.minicraft

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
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

class GameRenderer(
    private val context: Context,
    private val input: InputState
) : GLSurfaceView.Renderer {

    private val world = World()

    private var program = 0
    private var aPos = 0
    private var aColor = 0
    private var uMVP = 0

    private var vertexBuffer: FloatBuffer? = null
    private var vertexCount = 0
    private var meshDirty = true

    private var lastTime = 0L
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

    fun addLook(dx: Float, dy: Float) {
        yaw -= dx * 0.005f
        pitch -= dy * 0.005f
        pitch = pitch.coerceIn(-1.5f, 1.5f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.55f, 0.75f, 1.0f, 1.0f)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        program = createProgram()
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aColor = GLES20.glGetAttribLocation(program, "aColor")
        uMVP = GLES20.glGetUniformLocation(program, "uMVP")

        meshDirty = true
        lastTime = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        var dt = (now - lastTime) / 1_000_000_000.0
        lastTime = now
        if (dt > 0.05) dt = 0.05

        update(dt)

        if (meshDirty) {
            uploadMesh(world.buildMesh())
            meshDirty = false
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        Matrix.perspectiveM(proj, 0, 70f, aspect, 0.1f, 100f)

        val eyeX = px
        val eyeY = py + 1.62
        val eyeZ = pz
        val d = direction()

        Matrix.setLookAtM(
            view, 0,
            eyeX.toFloat(), eyeY.toFloat(), eyeZ.toFloat(),
            (eyeX + d[0]).toFloat(), (eyeY + d[1]).toFloat(), (eyeZ + d[2]).toFloat(),
            0f, 1f, 0f
        )

        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)

        val buf = vertexBuffer
        if (buf != null && vertexCount > 0) {
            buf.position(0)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, buf)

            buf.position(3)
            GLES20.glEnableVertexAttribArray(aColor)
            GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 24, buf)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            GLES20.glDisableVertexAttribArray(aPos)
            GLES20.glDisableVertexAttribArray(aColor)
        }
    }

    private fun update(dt: Double) {
        if (input.breakRequested) {
            input.breakRequested = false
            val hit = raycast()
            if (hit != null) {
                world.setBlock(hit.x, hit.y, hit.z, 0)
                meshDirty = true
            }
        }

        if (input.placeRequested) {
            input.placeRequested = false
            val hit = raycast()
            if (hit != null) {
                world.setBlock(hit.prevX, hit.prevY, hit.prevZ, 3)
                meshDirty = true
            }
        }

        var fx = 0.0
        var fz = 0.0

        if (input.moveForward) { fx += -sin(yaw.toDouble()); fz += -cos(yaw.toDouble()) }
        if (input.moveBack)    { fx +=  sin(yaw.toDouble()); fz +=  cos(yaw.toDouble()) }
        if (input.moveRight)   { fx +=  cos(yaw.toDouble()); fz += -sin(yaw.toDouble()) }
        if (input.moveLeft)    { fx += -cos(yaw.toDouble()); fz +=  sin(yaw.toDouble()) }

        val len = sqrt(fx * fx + fz * fz)
        if (len > 0.0) { fx /= len; fz /= len }

        val speed = 5.0
        val dx = fx * speed * dt
        val dz = fz * speed * dt

        px += dx
        if (collides()) px -= dx

        pz += dz
        if (collides()) pz -= dz

        if (input.jump) {
            if (onGround) {
                vy = 8.0
                onGround = false
            }
            input.jump = false
        }

        vy -= 20.0 * dt
        py += vy * dt

        if (collides()) {
            py -= vy * dt
            if (vy < 0.0) onGround = true
            vy = 0.0
        } else {
            onGround = false
        }

        if (py < -20.0) {
            px = 32.0; py = 20.0; pz = 32.0; vy = 0.0
        }
    }

    private fun collides(): Boolean {
        val minX = floor(px - 0.3).toInt()
        val maxX = floor(px + 0.3).toInt()
        val minY = floor(py).toInt()
        val maxY = floor(py + 1.8).toInt()
        val minZ = floor(pz - 0.3).toInt()
        val maxZ = floor(pz + 0.3).toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    if (world.isSolid(x, y, z)) return true
                }
            }
        }
        return false
    }

    private fun direction(): FloatArray {
        val cp = cos(pitch.toDouble()).toFloat()
        val sp = sin(pitch.toDouble()).toFloat()
        val cy = cos(yaw.toDouble()).toFloat()
        val sy = sin(yaw.toDouble()).toFloat()
        return floatArrayOf(-sy * cp, sp, -cy * cp)
    }

    private data class RayHit(
        val x: Int, val y: Int, val z: Int,
        val prevX: Int, val prevY: Int, val prevZ: Int
    )

    private fun raycast(): RayHit? {
        val eyeX = px
        val eyeY = py + 1.62
        val eyeZ = pz
        val d = direction()

        var lastX = floor(eyeX).toInt()
        var lastY = floor(eyeY).toInt()
        var lastZ = floor(eyeZ).toInt()

        var t = 0.0
        while (t < 6.0) {
            val bx = floor(eyeX + d[0] * t).toInt()
            val by = floor(eyeY + d[1] * t).toInt()
            val bz = floor(eyeZ + d[2] * t).toInt()

            if (bx != lastX || by != lastY || bz != lastZ) {
                if (world.isSolid(bx, by, bz)) {
                    return RayHit(bx, by, bz, lastX, lastY, lastZ)
                }
                lastX = bx; lastY = by; lastZ = bz
            }
            t += 0.05
        }
        return null
    }

    private fun uploadMesh(data: FloatArray) {
        vertexCount = data.size / 6
        val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(data)
        fb.position(0)
        vertexBuffer = fb
    }

    private fun createProgram(): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)

        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return p
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec3 aPos;
            attribute vec3 aColor;
            uniform mat4 uMVP;
            varying vec3 vColor;

            void main() {
                gl_Position = uMVP * vec4(aPos, 1.0);
                vColor = aColor;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec3 vColor;

            void main() {
                gl_FragColor = vec4(vColor, 1.0);
            }
        """
    }
}
