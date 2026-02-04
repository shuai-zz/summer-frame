package org.example.io;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * @author zhaoshuai
 */
public class PropertyResolver {
    Logger log = LoggerFactory.getLogger(getClass());

    Map<String, String> properties = new HashMap<>();
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        // 存入环境变量
        this.properties.putAll(System.getenv());
        // 存入Properties
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
        if (log.isDebugEnabled()) {
            ArrayList<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                log.debug("PropertyResolver: {} = {}", key, this.properties.get(key));
            }
        }

        // 添加converter
        converters.put(String.class, s -> s);
        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::valueOf);

        converters.put(byte.class, Byte::parseByte);
        converters.put(Byte.class, Byte::valueOf);

        converters.put(short.class, Short::parseShort);
        converters.put(Short.class, Short::valueOf);

        converters.put(int.class, Integer::parseInt);
        converters.put(Integer.class, Integer::valueOf);

        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::valueOf);

        converters.put(float.class, Float::parseFloat);
        converters.put(Float.class, Float::valueOf);

        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::valueOf);

        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Duration.class, Duration::parse);
        converters.put(ZoneId.class, ZoneId::of);
    }

    public boolean containsProperty(String key) {
        return this.properties.containsKey(key);
    }

    /**
     * 按key获取属性,实现查询${abc.xyz:defaultValue}
     *
     * @param key 属性名
     * @return 属性值
     */
    @Nullable
    public String getProperty(String key) {
        // 解析${abc.xyz:defaultValue}
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if (keyExpr != null) {
            if (keyExpr.defaultValue() != null) {
                // 存在defaultValue
                return getProperty(keyExpr.key(), keyExpr.defaultValue());
            } else {
                // 不存在defaultValue
                return getRequiredProperty(keyExpr.key());
            }
        }
        // 普通key查询
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return value;
    }

    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    /**
     * 根据指定的键获取属性值，并将其转换为目标类型
     *
     * @param key        属性键
     * @param targetType 目标类型
     * @param <T>        泛型参数，表示目标类型
     * @return 转换后的属性值，如果属性不存在则返回null
     */
    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(targetType, value);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }

    /**
     * 从String转换为指定类型
     *
     * @param clazz 目标类型
     * @param value 属性值
     * @param <T>   目标类型
     * @return 转换后的对象
     */
    @SuppressWarnings("unchecked")
    <T> T convert(Class<?> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }

    /**
     * 递归调用parseValue()，这样就可以支持嵌套的key，例如：
     * ${app.title:${APP_NAME:Summer}}
     *
     * @param value 属性值
     * @return value
     */
    String parseValue(String value) {
        PropertyExpr expr = parsePropertyExpr(value);
        if (expr == null) {
            return value;
        }
        if (expr.defaultValue() != null) {
            return getProperty(expr.key(), expr.defaultValue());
        } else {
            return getRequiredProperty(expr.key());
        }
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found,");
    }
    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }


    /**
     * 解析属性表达式
     *
     * @param key 表达式
     * @return 解析后的属性表达式
     */
    PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            // 是否存在defaultValue
            int n = key.indexOf(":");
            if (n == -1) {
                // 没有defaultValue: ${key}
                String k = notEmpty(key.substring(2, key.length() - 1));
                return new PropertyExpr(k, null);
            } else {
                // 有defaultValue: ${key:defaultValue}
                String k = notEmpty(key.substring(2, n));
                return new PropertyExpr(k, key.substring(n + 1, key.length() - 1));
            }
        }
        return null;
    }

    String notEmpty(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
        return key;
    }
}

/**
 * 把解析后的key和defaultValue存储起来
 *
 * @param key
 * @param defaultValue
 */
record PropertyExpr(String key, String defaultValue) {
}
