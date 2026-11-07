@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.tyfino.foundation.ui.screen

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import java.util.concurrent.atomic.AtomicReference

/** One mild monotone curve on linear SDR RGB; no HDR conversion, sharpening or invented detail. */
internal class VisionBalancedEffect(initialStrength: Float) : GlEffect {
    private val strength = AtomicReference(checkedStrength(initialStrength))

    // UI writes a scalar; only the GL thread touches program/uniform state.
    fun setStrength(value: Float) { strength.set(checkedStrength(value)) }

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        if (useHdr) throw VideoFrameProcessingException("Vision laboratory accepts SDR only")
        return CurveProgram(strength)
    }

    private class CurveProgram(private val strength: AtomicReference<Float>) : BaseGlShaderProgram(false, 1) {
        private val program: GlProgram
        init {
            try {
                program = GlProgram(VERTEX, FRAGMENT)
            } catch (error: GlUtil.GlException) {
                throw VideoFrameProcessingException(error)
            }
            try {
                program.setBufferAttribute("aPosition", GlUtil.getNormalizedCoordinateBounds(), 4)
                program.setFloatUniform("uStrength", strength.get())
            } catch (error: GlUtil.GlException) {
                try { program.delete() } catch (cleanup: GlUtil.GlException) { error.addSuppressed(cleanup) }
                throw VideoFrameProcessingException(error)
            }
        }
        override fun configure(inputWidth: Int, inputHeight: Int) = Size(inputWidth, inputHeight)
        override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
            try {
                program.use()
                program.setFloatUniform("uStrength", strength.get())
                program.setSamplerTexIdUniform("uTexture", inputTexId, 0)
                program.bindAttributesAndUniforms()
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                GlUtil.checkGlError()
            } catch (error: GlUtil.GlException) {
                throw VideoFrameProcessingException(error, presentationTimeUs)
            }
        }
        override fun release() {
            try { super.release() } finally {
                try { program.delete() } catch (error: GlUtil.GlException) {
                    throw VideoFrameProcessingException(error)
                }
            }
        }
    }

    private companion object {
        fun checkedStrength(value: Float): Float {
            require(value.isFinite() && value in 0f..1f)
            return value
        }
        const val VERTEX = """
            attribute vec4 aPosition;
            varying vec2 vCoordinate;
            void main() {
                gl_Position = aPosition;
                vCoordinate = aPosition.xy * 0.5 + 0.5;
            }
        """
        const val FRAGMENT = """
            precision highp float;
            uniform sampler2D uTexture;
            uniform float uStrength;
            varying vec2 vCoordinate;
            void main() {
                vec4 sampleColor = texture2D(uTexture, vCoordinate);
                vec3 x = clamp(sampleColor.rgb, 0.0, 1.0);
                vec3 y = x + (0.35 * uStrength) * x * (1.0 - x) * (x - 0.18);
                gl_FragColor = vec4(clamp(y, 0.0, 1.0), sampleColor.a);
            }
        """
    }
}
