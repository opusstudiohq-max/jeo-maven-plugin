/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.jeo.representation.bytecode;

import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test case for {@link JavaCodec}.
 *
 * @since 0.8
 */
final class JavaCodecTest {

    @ParameterizedTest
    @MethodSource("mapping")
    void encodesSuccessfully(final Object value, final DataType type, final byte[] bytes) {
        MatcherAssert.assertThat(
            "Can't encode value to the correct byte array",
            new JavaCodec().encode(value, type),
            Matchers.equalTo(bytes)
        );
    }

    @ParameterizedTest
    @MethodSource("mapping")
    void decodesSuccessfully(final Object value, final DataType type, final byte[] bytes) {
        MatcherAssert.assertThat(
            "Can't decode byte array to the correct value",
            new JavaCodec().decode(bytes, type),
            Matchers.equalTo(value)
        );
    }

    private static Object[][] mapping() {
        return new Object[][]{
            {null, DataType.NULL, new byte[0]},
            {true, DataType.BOOL, new byte[]{1}},
            {'a', DataType.CHAR, new byte[]{0, 97}},
            {new byte[]{0, 1, 2, 3}, DataType.BYTES, new byte[]{0, 1, 2, 3}},
            {"hello, world!", DataType.STRING, "hello, world!".getBytes(StandardCharsets.UTF_8)},
            {
                String.valueOf(new char[]{'a', (char) 0xD800, 'b'}),
                DataType.STRING,
                new byte[]{97, (byte) 0xED, (byte) 0xA0, (byte) 0x80, 98},
            },
            {
                String.valueOf(new char[]{'a', (char) 0xDC00}),
                DataType.STRING,
                new byte[]{97, (byte) 0xED, (byte) 0xB0, (byte) 0x80},
            },
            {
                String.valueOf(new char[]{'a', (char) 0xD83D, (char) 0xDE00, 'b'}),
                DataType.STRING,
                new byte[]{97, (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80, 98},
            },
            {
                String.valueOf(new char[]{'a', (char) 0, 'b'}),
                DataType.STRING,
                new byte[]{97, 0, 98},
            },
        };
    }
}
