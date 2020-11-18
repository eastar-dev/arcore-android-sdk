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
package com.google.ar.core.examples.java.common.samplerender;

import android.log.Log;
import android.opengl.GLES30;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

/**
 * 3D 객체를 렌더링하는 방법을 정의하는 정점,면 및 기타 속성 모음입니다.
 * mesh를 렌더링하려면 {@link SampleRender#draw()}를 사용하십시오.
 */
public class Mesh implements Closeable {
    private static final String TAG = Mesh.class.getSimpleName();

    /**
     * 렌더링 할 기본 유형입니다.
     * <p>
     * {@link VertexBuffer}의 데이터가 해석되는 방식을 결정합니다.
     * 프리미티브의 작동 방식에 대한 자세한 내용은 아래 참조하세요.
     *
     * @see <a href="https://www.khronos.org/opengl/wiki/Primitive">here</a>
     */
    public enum PrimitiveMode {
        POINTS(GLES30.GL_POINTS),
        LINE_STRIP(GLES30.GL_LINE_STRIP),
        LINE_LOOP(GLES30.GL_LINE_LOOP),
        LINES(GLES30.GL_LINES),
        TRIANGLE_STRIP(GLES30.GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GLES30.GL_TRIANGLE_FAN),
        TRIANGLES(GLES30.GL_TRIANGLES);

        /* package-private */
        final int glesEnum;

        private PrimitiveMode(int glesEnum) {
            this.glesEnum = glesEnum;
        }
    }

    private final int[] vertexArrayId = {0};
    private final PrimitiveMode primitiveMode;
    private final IndexBuffer indexBuffer;
    private final VertexBuffer[] vertexBuffers;

    /**
     * Construct a {@link Mesh}.
     *
     * <p>The data in the given {@link IndexBuffer} and {@link VertexBuffer}s does not need to be
     * finalized; they may be freely changed throughout the lifetime of a {@link Mesh} using their
     * respective {@code set()} methods.
     *
     * <p>The ordering of the {@code vertexBuffers} is significant. Their array indices will
     * correspond to their attribute locations, which must be taken into account in shader code. The
     * <a href="https://www.khronos.org/opengl/wiki/Layout_Qualifier_(GLSL)">layout qualifier</a> must
     * be used in the vertex shader code to explicitly associate attributes with these indices.
     * <p>
     * 메시를 생성합니다.
     * <p>
     * 지정된 IndexBuffer 및 VertexBuffers의 데이터는 마무리 할 필요가 없습니다.
     * 각각의 set () 메서드를 사용하여 Mesh의 수명 동안 자유롭게 변경할 수 있습니다.
     * <p>
     * vertexBuffer의 순서는 중요합니다.
     * 배열 인덱스는 셰이더 코드에서 고려해야하는 속성 위치에 해당합니다.
     * 레이아웃 한정자는 정점 셰이더 코드에서 특성을 이러한 인덱스와 명시 적으로 연결하는 데 사용되어야합니다.
     */
    public Mesh(
        SampleRender render,
        PrimitiveMode primitiveMode,
        IndexBuffer indexBuffer,
        VertexBuffer[] vertexBuffers) {
        if (vertexBuffers == null || vertexBuffers.length == 0) {
            throw new IllegalArgumentException("Must pass at least one vertex buffer");
        }

        this.primitiveMode = primitiveMode;
        this.indexBuffer = indexBuffer;
        this.vertexBuffers = vertexBuffers;

        try {
            // Create vertex array
            GLES30.glGenVertexArrays(1, vertexArrayId, 0);
            GLError.maybeThrowGLException("Failed to generate a vertex array", "glGenVertexArrays");

            // Bind vertex array
            GLES30.glBindVertexArray(vertexArrayId[0]);
            GLError.maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray");

            if (indexBuffer != null) {
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getBufferId());
            }

            for (int i = 0; i < vertexBuffers.length; ++i) {
                // Bind each vertex buffer to vertex array
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffers[i].getBufferId());
                GLError.maybeThrowGLException("Failed to bind vertex buffer", "glBindBuffer");
                GLES30.glVertexAttribPointer(i, vertexBuffers[i].getNumberOfEntriesPerVertex(), GLES30.GL_FLOAT, false, 0, 0);
                GLError.maybeThrowGLException("Failed to associate vertex buffer with vertex array", "glVertexAttribPointer");
                GLES30.glEnableVertexAttribArray(i);
                GLError.maybeThrowGLException("Failed to enable vertex buffer", "glEnableVertexAttribArray");
            }
        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    /**
     * 주어진 Wavefront OBJ 파일에서 메시를 생성합니다.
     * <p>
     * {@link Mesh}는
     * local coordinates (location 0, vec3), (로컬 좌표)
     * texture coordinates (location 1, vec2), (텍스처 좌표)
     * and vertex normals (location 2, vec3) (정점 법선)
     * 순서로 색인화 된 세 가지 속성으로 구성됩니다.
     */
    public static Mesh createFromAsset(SampleRender render, String assetFileName) throws IOException {
        try (InputStream inputStream = render.getAssets().open(assetFileName)) {
            Obj obj = ObjUtils.convertToRenderable(ObjReader.read(inputStream));

            // Obtain the data from the OBJ, as direct buffers:
            IntBuffer vertexIndices = ObjData.getFaceVertexIndices(obj, /*numVerticesPerFace=*/ 3);
            FloatBuffer localCoordinates = ObjData.getVertices(obj);
            FloatBuffer textureCoordinates = ObjData.getTexCoords(obj, /*dimensions=*/ 2);
            FloatBuffer normals = ObjData.getNormals(obj);

            VertexBuffer[] vertexBuffers = {
                new VertexBuffer(render, 3, localCoordinates),
                new VertexBuffer(render, 2, textureCoordinates),
                new VertexBuffer(render, 3, normals),
            };

            IndexBuffer indexBuffer = new IndexBuffer(render, vertexIndices);

            return new Mesh(render, Mesh.PrimitiveMode.TRIANGLES, indexBuffer, vertexBuffers);
        }
    }

    @Override
    public void close() {
        if (vertexArrayId[0] != 0) {
            GLES30.glDeleteVertexArrays(1, vertexArrayId, 0);
            GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free vertex array object", "glDeleteVertexArrays");
        }
    }

    /* package-private */
    void draw() {
        if (vertexArrayId[0] == 0) {
            throw new IllegalStateException("Tried to draw a freed Mesh");
        }

        GLES30.glBindVertexArray(vertexArrayId[0]);
        GLError.maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray");
        if (indexBuffer == null) {
            // Sanity check for debugging
            int numberOfVertices = vertexBuffers[0].getNumberOfVertices();
            for (int i = 1; i < vertexBuffers.length; ++i) {
                if (vertexBuffers[i].getNumberOfVertices() != numberOfVertices) {
                    throw new IllegalStateException("Vertex buffers have mismatching numbers of vertices");
                }
            }
            GLES30.glDrawArrays(primitiveMode.glesEnum, 0, numberOfVertices);
            GLError.maybeThrowGLException("Failed to draw vertex array object", "glDrawArrays");
        } else {
            GLES30.glDrawElements(primitiveMode.glesEnum, indexBuffer.getSize(), GLES30.GL_UNSIGNED_INT, 0);
            GLError.maybeThrowGLException("Failed to draw vertex array object with indices", "glDrawElements");
        }
    }
}
