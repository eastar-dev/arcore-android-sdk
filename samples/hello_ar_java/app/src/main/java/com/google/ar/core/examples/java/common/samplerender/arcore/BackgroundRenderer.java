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
package com.google.ar.core.examples.java.common.samplerender.arcore;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 이 클래스는 카메라 피드에서 AR 배경을 렌더링합니다.
 * 카메라 이미지로 채우기 위해 ARCore에 제공된 텍스처를 생성하고 호스팅합니다.
 */
public class BackgroundRenderer {
    private static final String TAG = BackgroundRenderer.class.getSimpleName();

    // Shader names.
    private static final String CAMERA_VERTEX_SHADER_NAME = "shaders/background_show_camera.vert";
    private static final String CAMERA_FRAGMENT_SHADER_NAME = "shaders/background_show_camera.frag";
    private static final String DEPTH_VISUALIZER_VERTEX_SHADER_NAME = "shaders/background_show_depth_color_visualization.vert";
    private static final String DEPTH_VISUALIZER_FRAGMENT_SHADER_NAME = "shaders/background_show_depth_color_visualization.frag";
    private static final int FLOAT_SIZE = 4;

    private static final float[] QUAD_COORDS_ARRAY = {        /*0:*/ -1f, -1f, /*1:*/ +1f, -1f, /*2:*/ -1f, +1f, /*3:*/ +1f, +1f,};

    private static final FloatBuffer QUAD_COORDS_BUFFER =
        ByteBuffer.allocateDirect(QUAD_COORDS_ARRAY.length * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();

    static {
        QUAD_COORDS_BUFFER.put(QUAD_COORDS_ARRAY);
    }

    private final FloatBuffer texCoords =
        ByteBuffer.allocateDirect(QUAD_COORDS_ARRAY.length * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();

    private final Mesh mesh;
    private final VertexBuffer texCoordsVertexBuffer;
    private final Shader cameraShader;
    private final Shader depthShader;
    private final Texture cameraTexture;

    /**
     * 백그라운드 렌더러에 필요한 OpenGL 리소스를 할당하고 초기화합니다.
     * 반드시 SampleRender.Renderer .callback 도중에 호출되어야합니다
     */
    public BackgroundRenderer(SampleRender render, Texture depthTexture) throws IOException {
        cameraTexture = new Texture(render, Texture.Target.TEXTURE_EXTERNAL_OES, Texture.WrapMode.CLAMP_TO_EDGE);

        cameraShader = Shader.createFromAssets(
            render, CAMERA_VERTEX_SHADER_NAME, CAMERA_FRAGMENT_SHADER_NAME, /*defines=*/ null)
            .setTexture("u_Texture", cameraTexture)
            .setDepthTest(false)
            .setDepthWrite(false);

        if (depthTexture == null) {
            depthShader = null;
        } else {
            depthShader = Shader.createFromAssets(
                render,
                DEPTH_VISUALIZER_VERTEX_SHADER_NAME,
                DEPTH_VISUALIZER_FRAGMENT_SHADER_NAME,
                /*defines=*/ null)
                .setTexture("u_DepthTexture", depthTexture)
                .setDepthTest(false)
                .setDepthWrite(false);
        }

        VertexBuffer localCoordsVertexBuffer = new VertexBuffer(render, 2, QUAD_COORDS_BUFFER);
        texCoordsVertexBuffer = new VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 2, /*entries=*/ null);
        VertexBuffer[] vertexBuffers = {localCoordsVertexBuffer, texCoordsVertexBuffer,};
        mesh = new Mesh(render, Mesh.PrimitiveMode.TRIANGLE_STRIP, /*indexBuffer=*/ null, vertexBuffers);
    }

    /**
     * AR 배경 이미지를 그립니다.
     * com.google.ar.core.Camera.getViewMatrix (float [], int) 및 com.google.ar.core.Camera.getProjectionMatrix (float [], int, float, float)는
     * 정적 물리적 객체를 정확하게 따릅니다. 가상 콘텐츠를 그리기 전에 호출해야합니다.
     * 가상 콘텐츠는 com.google.ar.core.Camera.getViewMatrix (float [], int) 및 com.google.ar.core.Camera.getProjectionMatrix (float [], int, float, float에서
     * 제공하는 매트릭스를 사용하여 렌더링되어야합니다. ).
     *
     * @param frame             The current {@code Frame} as returned by {@link Session#update()}.
     * @param showDebugDepthMap Toggles whether to show the live camera feed or latest depth image.
     */
    public void draw(SampleRender render, Frame frame, boolean showDebugDepthMap) {
        // If display rotation changed (also includes view size change), we need to re-query the uv
        // coordinates for the screen rect, as they may have changed as well.
        if (frame.hasDisplayGeometryChanged()) {
            QUAD_COORDS_BUFFER.rewind();
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                QUAD_COORDS_BUFFER,
                Coordinates2d.TEXTURE_NORMALIZED,
                texCoords);
            texCoordsVertexBuffer.set(texCoords);
        }

        if (frame.getTimestamp() == 0) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            return;
        }

        if (showDebugDepthMap) {
            render.draw(mesh, depthShader);
        } else {
            render.draw(mesh, cameraShader);
        }
    }

    /**
     * 이 객체가 생성 한 텍스처 ID를 반환합니다.
     */
    public int getTextureId() {
        return cameraTexture.getTextureId();
    }
}
