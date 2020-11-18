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
import java.nio.IntBuffer;

/**
 * GPU 측에 저장된 정점 인덱스 목록입니다.
 *
 * <p>When constructing a {@link Mesh}, an {@link IndexBuffer} may be passed to describe the
 * ordering of vertices when drawing each primitive.
 * <p>
 * 메시를 구성 할 때 각 프리미티브를 그릴 때 정점 순서를 설명하기 위해 IndexBuffer가 전달 될 수 있습니다.
 *
 * @see <a href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glDrawElements.xhtml">glDrawElements</a>
 */
public class IndexBuffer implements Closeable {
    private final GpuBuffer buffer;

    /**
     * Construct an {@link IndexBuffer} populated with initial data.
     *
     * <p>The GPU buffer will be filled with the data in the <i>direct</i> buffer {@code entries},
     * starting from the beginning of the buffer (not the current cursor position). The cursor will be
     * left in an undefined position after this function returns.
     *
     * <p>The {@code entries} buffer may be null, in which case an empty buffer is constructed
     * instead.
     * <p>
     * <p>
     * 초기 데이터로 채워진 IndexBuffer를 생성합니다.
     * <p>
     * GPU 버퍼는 버퍼의 시작 (현재 커서 위치 아님)부터 시작하여 직접 버퍼 항목의 데이터로 채워집니다.
     * 이 함수가 반환 된 후 커서는 정의되지 않은 위치에 남아 있습니다.
     * <p>
     * 엔트리 버퍼는 null 일 수 있으며,이 경우 빈 버퍼가 대신 생성됩니다.
     */
    public IndexBuffer(SampleRender render, IntBuffer entries) {
        buffer = new GpuBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GpuBuffer.INT_SIZE, entries);
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
     * <p>
     * <p>
     * 새 데이터로 채 웁니다.
     * <p>
     * 전체 버퍼는 현재 커서 위치가 아니라 버퍼의 시작 부분에서 시작하는 직접 버퍼 항목의 내용으로 대체됩니다.
     * 이 함수가 반환 된 후 커서는 정의되지 않은 위치에 남아 있습니다.
     * <p>
     * 필요한 경우 GPU 버퍼가 자동으로 재 할당됩니다.
     * <p>
     * 엔트리 버퍼는 널일 수 있으며,이 경우 버퍼가 비어있게됩니다.
     */
    public void set(IntBuffer entries) {
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
    int getSize() {
        return buffer.getSize();
    }
}
