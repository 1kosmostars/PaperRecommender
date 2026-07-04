package com.recommender.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * JPA converter: float[] (Java) ↔ bytea (PostgreSQL).
 * Stores 768-dim SPECTER2 vectors as raw little-endian binary (4 bytes per float).
 */
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        ByteBuffer buf = ByteBuffer.allocate(attribute.length * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float f : attribute) {
            buf.putFloat(f);
        }
        return buf.array();
    }

    @Override
    public float[] convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) return null;
        FloatBuffer fb = ByteBuffer.wrap(dbData)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer();
        float[] result = new float[fb.remaining()];
        fb.get(result);
        return result;
    }
}
