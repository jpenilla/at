/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Architectury
 * Copyright (c) 2018 Minecrell (https://github.com/Minecrell)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dev.architectury.at.io.fml;

import dev.architectury.at.AccessChange;
import dev.architectury.at.AccessTransform;
import dev.architectury.at.AccessTransformSet;
import dev.architectury.at.ModifierChange;
import org.cadixdev.bombe.type.signature.MethodSignature;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

final class FmlReader {

    private static final char COMMENT_PREFIX = '#';
    private static final char WILDCARD = '*';

    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    static void read(BufferedReader reader, AccessTransformSet set) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = substringBefore(line, COMMENT_PREFIX).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = SPACE_PATTERN.split(line);
            if (parts.length != 2 && parts.length != 3) {
                throw new IllegalArgumentException("Invalid FML access transformer line: " + line);
            }

            AccessTransform transform = parseAccessTransform(parts[0]);

            AccessTransformSet.Class classSet = set.getOrCreateClass(parts[1]);

            if (parts.length == 2) {
                // Class
                classSet.merge(transform);
            } else {
                String name = parts[2];
                int methodIndex = name.indexOf('(');

                if (name.charAt(0) == WILDCARD) {
                    // Wildcard
                    if (methodIndex != -1) {
                        classSet.mergeAllMethods(transform);
                    } else {
                        classSet.mergeAllFields(transform);
                    }
                } else if (methodIndex >= 0) {
                    classSet.mergeMethod(MethodSignature.of(name.substring(0, methodIndex), name.substring(methodIndex)), transform);
                } else {
                    classSet.mergeField(name, transform);
                }
            }
        }
    }

    private static AccessTransform parseAccessTransform(String access) {
        int last = access.length() - 1;
        if (last < 2) {
            throw new IllegalArgumentException("Invalid access transformer: " + access);
        }

        ModifierChange finalChange;
        if (access.charAt(last) == 'f') {
            finalChange = parseFinalModifier(access.charAt(--last));
            access = access.substring(0, last);
        } else {
            finalChange = ModifierChange.NONE;
        }

        return AccessTransform.of(parseAccess(access), finalChange);
    }

    private static AccessChange parseAccess(String access) {
        switch (access) {
            case "public":
                return AccessChange.PUBLIC;
            case "protected":
                return AccessChange.PROTECTED;
            case "default":
                return AccessChange.PACKAGE_PRIVATE;
            case "private":
                return AccessChange.PRIVATE;
            case "":
                return AccessChange.NONE;
            default:
                throw new IllegalArgumentException("Invalid access modifier: " + access);

        }
    }

    private static ModifierChange parseFinalModifier(char m) {
        switch (m) {
            case '-':
                return ModifierChange.REMOVE;
            case '+':
                return ModifierChange.ADD;
            default:
                throw new IllegalArgumentException("Invalid final modifier: '" + m);
        }
    }

    private static String substringBefore(String s, char c) {
        int pos = s.indexOf(c);
        return pos >= 0 ? s.substring(0, pos) : s;
    }

    private FmlReader() {
    }

}
