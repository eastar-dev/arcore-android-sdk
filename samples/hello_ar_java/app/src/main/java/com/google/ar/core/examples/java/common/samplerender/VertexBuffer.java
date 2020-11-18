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

import android.opengl.GLES30;

import java.io.Closeable;
import java.nio.FloatBuffer;

/**
 * GPU 측에 저장된 정점 속성 데이터 목록입니다.
 * 하나 이상의 VertexBuffer는 정점 속성 데이터를 설명하기 위해 메시를 구성 할 때 사용됩니다.
 * 예를 들어 로컬 좌표, 텍스처 좌표, 정점 법선 등이 있습니다.
 *
 * @see <a href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glVertexAttribPointer.xhtml">glVertexAttribPointer</a>
 */
public class VertexBuffer implements Closeable {
    private final GpuBuffer buffer;
    private final int numberOfEntriesPerVertex;

    /**
     * Construct a {@link VertexBuffer} populated with initial data.
     *
     * <p>The GPU buffer will be filled with the data in the <i>direct</i> buffer {@code entries},
     * starting from the beginning of the buffer (not the current cursor position). The cursor will be
     * left in an undefined position after this function returns.
     *
     * <p>The number of vertices in the buffer can be expressed as {@code entries.limit() /
     * numberOfEntriesPerVertex}. Thus, The size of the buffer must be divisible by {@code
     * numberOfEntriesPerVertex}.
     *
     * <p>The {@code entries} buffer may be null, in which case an empty buffer is constructed
     * instead.
     *
     *
     * 초기 데이터로 채워진 VertexBuffer를 생성합니다.
     * GPU 버퍼는 버퍼의 시작 (현재 커서 위치 아님)부터 시작하여 직접 버퍼 항목의 데이터로 채워집니다.
     * 이 함수가 반환 된 후 커서는 정의되지 않은 위치에 남아 있습니다.
     * <p>
     * 버퍼의 정점 수는 entry.limit() / numberOfEntriesPerVertex로 표현할 수 있습니다.
     * 따라서 버퍼의 크기는 numberOfEntriesPerVertex로 나눌 수 있어야합니다.
     *
     * 엔트리 버퍼는 null 일 수 있으며, 이 경우 빈 버퍼가 대신 생성됩니다.
     */
    public VertexBuffer(SampleRender render, int numberOfEntriesPerVertex, FloatBuffer entries) {
        if (entries != null && entries.limit() % numberOfEntriesPerVertex != 0) {
            throw new IllegalArgumentException("If non-null, vertex buffer data must be divisible by the number of data points per vertex");
        }

        this.numberOfEntriesPerVertex = numberOfEntriesPerVertex;
        buffer = new GpuBuffer(GLES30.GL_ARRAY_BUFFER, GpuBuffer.FLOAT_SIZE, entries);
    }

    /**
     * Populate with new data.
     *
     * <p>The entire buffer is replaced by the contents of the <i>direct</i> buffer {@code entries}
     * starting from the beginning of the buffer, not the current cursor position. The cursor will be
     * left in an undefined position after this function returns.
     *
     * <p>The GPU buffer is reallocated automatically if necessary.
     *
     * <p>The {@code entries} buffer may be null, in which case the buffer will become empty.
     * Otherwise, the size of {@code entries} must be divisible by the number of entries per vertex
     * specified during construction.
     *
     * 새 데이터로 채 웁니다.
     *
     * 전체 버퍼는 현재 커서 위치가 아니라 버퍼의 시작 부분에서 시작하는 직접 버퍼 항목의 내용으로 대체됩니다.
     * 이 함수가 반환 된 후 커서는 정의되지 않은 위치에 남아 있습니다.
     *
     * 필요한 경우 GPU 버퍼가 자동으로 재 할당됩니다.
     *
     * 엔트리 버퍼는 널일 수 있으며,이 경우 버퍼가 비어있게됩니다.
     * 그렇지 않으면 항목 크기를 구성 중에 지정된 정점 당 항목 수로 나눌 수 있어야합니다.
     *
     */
    public void set(FloatBuffer entries) {
        if (entries != null && entries.limit() % numberOfEntriesPerVertex != 0) {
            throw new IllegalArgumentException("If non-null, vertex buffer data must be divisible by the number of data points per vertex");
        }
        buffer.set(entries);
    }

    @Override
    public void close() {
        buffer.free();
    }

    /* package-private */
    int getBufferId() {
        return buffer.getBufferId();
    }

    /* package-private */
    int getNumberOfEntriesPerVertex() {
        return numberOfEntriesPerVertex;
    }

    /* package-private */
    int getNumberOfVertices() {
        return buffer.getSize() / numberOfEntriesPerVertex;
    }
}
