package com.evallab.api.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 雪花 ID 序列化为字符串。
 * <p>19 位雪花 ID 超出 JS 的 Number.MAX_SAFE_INTEGER（2^53-1），前端解析会丢精度，
 * 导致按 ID 编辑/删除打到错误的行。所有对外暴露的 Long 主键都用它。
 * 反序列化无需处理：Jackson 默认接受字符串形式的 Long。
 */
public final class IdJson extends JsonSerializer<Long> {

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(String.valueOf(value));
    }
}
