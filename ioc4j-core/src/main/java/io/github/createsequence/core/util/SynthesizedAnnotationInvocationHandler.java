package io.github.createsequence.core.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * 合成注解代理
 *
 * @author huangchengxing
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class SynthesizedAnnotationInvocationHandler implements InvocationHandler {

    private static final String EQUALS_METHOD = "equals";
    private static final String TO_STRING_METHOD = "toString";
    private static final String HASH_CODE_METHOD = "hashCode";
    private static final String ANNOTATION_TYPE_METHOD = "annotationType";

    @Getter
    @EqualsAndHashCode.Include
    private final Class<? extends Annotation> type;
    @Getter
    @EqualsAndHashCode.Include
    private final Map<String, Object> memberValues;
    private volatile String stringValue = null;

    /**
     * 目标注解是否为合成的注解
     *
     * @param annotation 注解
     * @return 是否
     */
    public static boolean isSynthesized(Annotation annotation) {
        return annotation instanceof SynthesizedAnnotation;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        return switch (method.getName()) {
            case EQUALS_METHOD -> this.equals(args[0]);
            case TO_STRING_METHOD -> this.toString();
            case HASH_CODE_METHOD -> this.hashCode();
            case ANNOTATION_TYPE_METHOD -> this.type;
            default -> memberValues.get(methodName);
        };
    }

    @Override
    public String toString() {
        if (Objects.isNull(stringValue)) {
            synchronized (this) {
                if (Objects.isNull(stringValue)) {
                    stringValue = getToStringValue();
                }
            }
        }
        return stringValue;
    }

    public String getToStringValue() {
        StringBuilder result = new StringBuilder(128);
        result.append('@');
        result.append(type.getName());
        result.append('(');
        boolean firstMember = true;
        Set<Map.Entry<String, Object>> entries = memberValues.entrySet();
        boolean loneValue = entries.size() == 1;
        for (Map.Entry<String, Object> e : entries) {
            if (firstMember) {
                firstMember = false;
            } else {
                result.append(", ");
            }
            String key = e.getKey();
            if (!loneValue || !"value".equals(key)) {
                result.append(key);
                result.append('=');
            }
            loneValue = false;
            result.append(memberValueToString(e.getValue()));
        }
        result.append(')');
        return result.toString();
    }

    private static String memberValueToString(Object value) {
        Class<?> type = value.getClass();
        if (!type.isArray()) {
            // primitive value, string, class, enum const, or annotation
            if (type == Class.class) {
                return toSourceString((Class<?>) value);
            } else if (type == String.class) {
                return toSourceString((String)value);
            }
            if (type == Character.class) {
                return toSourceString((char) value);
            } else if (type == Double.class) {
                return toSourceString((double)value);
            } else if (type == Float.class) {
                return toSourceString((float)value);
            } else if (type == Long.class) {
                return toSourceString((long)value);
            } else if (type == Byte.class) {
                return toSourceString((byte)value);
            } else {
                return value.toString();
            }
        } else {
            Stream<String> stringStream;
            if (type == byte[].class) {
                stringStream = convert((byte[]) value);
            } else if (type == char[].class) {
                stringStream = convert((char[])value);
            } else if (type == double[].class) {
                stringStream = DoubleStream.of((double[])value)
                    .mapToObj(SynthesizedAnnotationInvocationHandler::toSourceString);
            } else if (type == float[].class) {
                stringStream = convert((float[])value);
            } else if (type == int[].class) {
                stringStream = IntStream.of((int[])value)
                    .mapToObj(String::valueOf);
            } else if (type == long[].class) {
                stringStream = LongStream.of((long[])value)
                    .mapToObj(SynthesizedAnnotationInvocationHandler::toSourceString);
            } else if (type == short[].class) {
                stringStream = convert((short[])value);
            } else if (type == boolean[].class) {
                stringStream = convert((boolean[])value);
            } else if (type == Class[].class) {
                stringStream = Arrays.stream((Class<?>[])value)
                    .map(SynthesizedAnnotationInvocationHandler::toSourceString);
            } else if (type == String[].class) {
                stringStream = Arrays.stream((String[])value)
                    .map(SynthesizedAnnotationInvocationHandler::toSourceString);
            } else {
                stringStream = Arrays.stream((Object[])value)
                    .map(Objects::toString);
            }
            return stringStreamToString(stringStream);
        }
    }

    private static String toSourceString(Class<?> clazz) {
        Class<?> finalComponent = clazz;
        StringBuilder arrayBrackets = new StringBuilder();

        while(finalComponent.isArray()) {
            finalComponent = finalComponent.getComponentType();
            arrayBrackets.append("[]");
        }

        return finalComponent.getName() + arrayBrackets.toString() + ".class";
    }

    private static String toSourceString(float f) {
        if (Float.isFinite(f)) {
            return Float.toString(f) + "f";
        } else {
            if (Float.isInfinite(f)) {
                return (f < 0.0f) ? "-1.0f/0.0f" : "1.0f/0.0f";
            } else {
                return "0.0f/0.0f";
            }
        }
    }

    private static String toSourceString(double d) {
        if (Double.isFinite(d)) {
            return Double.toString(d);
        } else {
            if (Double.isInfinite(d)) {
                return (d < 0.0f) ? "-1.0/0.0" : "1.0/0.0";
            } else {
                return "0.0/0.0";
            }
        }
    }

    private static String toSourceString(char c) {
        return '\'' + quote(c) + '\'';
    }

    private static String quote(char ch) {
        return switch (ch) {
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\'' -> "\\'";
            case '\"' -> "\\\"";
            case '\\' -> "\\\\";
            default -> (isPrintableAscii(ch)) ? String.valueOf(ch) : String.format("\\u%04x", (int)ch);
        };
    }

    private static boolean isPrintableAscii(char ch) {
        return ch >= ' ' && ch <= '~';
    }

    private static String toSourceString(byte b) {
        return String.format("(byte)0x%02x", b);
    }

    private static String toSourceString(long ell) {
        return ell + "L";
    }

    /**
     * Return a string suitable for use in the string representation
     * of an annotation.
     */
    private static String toSourceString(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            sb.append(quote(s.charAt(i)));
        }
        sb.append('"');
        return sb.toString();
    }

    private static Stream<String> convert(byte[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (byte b : values) {
            list.add(toSourceString(b));
        }
        return list.stream();
    }

    private static Stream<String> convert(char[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (char c : values) {
            list.add(toSourceString(c));
        }
        return list.stream();
    }

    private static Stream<String> convert(float[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (float f : values) {
            list.add(toSourceString(f));
        }
        return list.stream();
    }

    private static Stream<String> convert(short[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (short s : values) {
            list.add(Short.toString(s));
        }
        return list.stream();
    }

    private static Stream<String> convert(boolean[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (boolean b : values) {
            list.add(Boolean.toString(b));
        }
        return list.stream();
    }

    private static String stringStreamToString(Stream<String> stream) {
        return stream.collect(Collectors.joining(", ", "{", "}"));
    }

    public interface SynthesizedAnnotation {}
}
