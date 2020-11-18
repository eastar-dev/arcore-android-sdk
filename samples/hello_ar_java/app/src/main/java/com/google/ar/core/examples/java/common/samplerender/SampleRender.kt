/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.common.samplerender

import android.opengl.GLSurfaceView
import android.content.res.AssetManager
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES30
import javax.microedition.khronos.egl.EGLConfig

class SampleRender(glSurfaceView: GLSurfaceView, renderer: Renderer, val assets: AssetManager) {
    /** 콜백 렌더링을 위해 구현할 인터페이스입니다. */
    interface Renderer {
        fun onSurfaceCreated(render: SampleRender)
        fun onSurfaceChanged(render: SampleRender, width: Int, height: Int)
        fun onDrawFrame(render: SampleRender)
    }

    init {
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
                GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                GLES30.glEnable(GLES30.GL_BLEND)
                renderer.onSurfaceCreated(this@SampleRender)
            }

            override fun onSurfaceChanged(gl: GL10, w: Int, h: Int) {
                GLES30.glViewport(0, 0, w, h)
                renderer.onSurfaceChanged(this@SampleRender, w, h)
            }

            override fun onDrawFrame(gl: GL10) {
                GLES30.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
                renderer.onDrawFrame(this@SampleRender)
            }
        })
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.setWillNotDraw(false)
    }

    fun draw(mesh: Mesh, shader: Shader) {
        shader.use()
        mesh.draw()
    }
}