/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.jeo.representation.bytecode;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Plain codec.
 * Converts objects to bytes and vice versa using Java type sizes.
 *
 * @since 0.8
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 */
public final class JavaCodec implements Codec {

    /**
     * Empty bytes.
     */
    private static final byte[] EMPTY = new byte[0];

    /**
     * Constructor.
     */
    public JavaCodec() {
        // Nothing to initialize.
    }

    @Override
    public byte[] encode(final Object value, final DataType type) {
        final byte[] result;
        switch (type) {
            case BOOL:
                result = JavaCodec.booleanBytes(value);
                break;
            case CHAR:
                result = JavaCodec.charBytes(value);
                break;
            case BYTE:
                result = ByteBuffer.allocate(Byte.BYTES).put((byte) value).array();
                break;
            case SHORT:
                result = ByteBuffer.allocate(Short.BYTES).putShort((short) value).array();
                break;
            case INT:
                result = ByteBuffer.allocate(Long.BYTES).putLong((int) value).array();
                break;
            case LONG:
                result = ByteBuffer.allocate(Long.BYTES).putLong((long) value).array();
                break;
            case FLOAT:
                result = ByteBuffer.allocate(Float.BYTES).putFloat((float) value).array();
                break;
            case DOUBLE:
                result = ByteBuffer.allocate(Double.BYTES).putDouble((double) value).array();
                break;
            case STRING:
                result = Optional.ofNullable(value).map(String::valueOf)
                    .map(JavaCodec::encoded)
                    .orElse(null);
                break;
            case BYTES:
                result = byte[].class.cast(value);
                break;
            case NULL:
                result = JavaCodec.EMPTY;
                break;
            default:
                throw new UnsupportedDataType(type);
        }
        return result;
    }

    @Override
    public Object decode(final byte[] bytes, final DataType type) {
        final Object result;
        switch (type) {
            case BOOL:
                result = bytes[0] != 0;
                break;
            case CHAR:
                result = ByteBuffer.wrap(bytes).getChar();
                break;
            case BYTE:
                result = ByteBuffer.wrap(bytes).get();
                break;
            case SHORT:
                result = ByteBuffer.wrap(bytes).getShort();
                break;
            case INT:
                result = (int) ByteBuffer.wrap(bytes).getLong();
                break;
            case LONG:
                result = ByteBuffer.wrap(bytes).getLong();
                break;
            case FLOAT:
                result = ByteBuffer.wrap(bytes).getFloat();
                break;
            case DOUBLE:
                result = ByteBuffer.wrap(bytes).getDouble();
                break;
            case STRING:
                result = Optional.ofNullable(bytes)
                    .map(JavaCodec::decoded)
                    .orElse("");
                break;
            case BYTES:
                result = bytes;
                break;
            case NULL:
                result = null;
                break;
            default:
                throw new UnsupportedDataType(type);
        }
        return result;
    }

    private static byte[] encoded(final String text) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
        int idx = 0;
        while (idx < text.length()) {
            final int point = text.codePointAt(idx);
            if (point < 0x80) {
                out.write(point);
            } else if (point < 0x800) {
                out.write(0xC0 | point >> 6);
                out.write(0x80 | point & 0x3F);
            } else if (point < 0x10000) {
                out.write(0xE0 | point >> 12);
                out.write(0x80 | point >> 6 & 0x3F);
                out.write(0x80 | point & 0x3F);
            } else {
                out.write(0xF0 | point >> 18);
                out.write(0x80 | point >> 12 & 0x3F);
                out.write(0x80 | point >> 6 & 0x3F);
                out.write(0x80 | point & 0x3F);
            }
            idx += Character.charCount(point);
        }
        return out.toByteArray();
    }

    private static String decoded(final byte[] bytes) {
        final StringBuilder out = new StringBuilder(bytes.length);
        int idx = 0;
        while (idx < bytes.length) {
            final int lead = bytes[idx] & 0xFF;
            final int size;
            final int mask;
            if (lead < 0x80) {
                size = 1;
                mask = 0x7F;
            } else if (lead < 0xE0) {
                size = 2;
                mask = 0x1F;
            } else if (lead < 0xF0) {
                size = 3;
                mask = 0x0F;
            } else {
                size = 4;
                mask = 0x07;
            }
            if (idx + size > bytes.length) {
                throw new IllegalArgumentException(
                    String.format("Truncated UTF-8 sequence at byte %d of %d", idx, bytes.length)
                );
            }
            int point = lead & mask;
            for (int pos = 1; pos < size; ++pos) {
                point = point << 6 | bytes[idx + pos] & 0x3F;
            }
            out.appendCodePoint(point);
            idx += size;
        }
        return out.toString();
    }

    private static byte[] booleanBytes(final Object value) {
        final byte[] result;
        if (value instanceof Integer) {
            result = JavaCodec.hexBoolean((int) value != 0);
        } else {
            result = JavaCodec.hexBoolean(Boolean.class.cast(value));
        }
        return result;
    }

    private static byte[] charBytes(final Object value) {
        final char val;
        if (value instanceof Integer) {
            val = (char) (int) value;
        } else {
            val = (char) value;
        }
        return ByteBuffer.allocate(Character.BYTES).putChar(val).array();
    }

    private static byte[] hexBoolean(final boolean data) {
        final byte[] result;
        if (data) {
            result = new byte[]{0x01};
        } else {
            result = new byte[]{0x00};
        }
        return result;
    }
}
