package org.example.utils;

import org.slf4j.helpers.SubstituteServiceProvider;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse yaml by snakeyaml:
 *
 * @author zhaoshuai
 */
// https://github.com/snakeyaml/snakeyaml
@SuppressWarnings("unused")
public class YamlUtils {
    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYaml(String path) {
        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        var resolver = new NoImplicitResolver();
        var yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);

        return ClassPathUtils.readInputStream(path, (input) -> {
            return (Map<String, Object>) yaml.load(input);
        });
    }

    /**
     * 读取一个YAML文件，并返回Map
     *
     * @param path yaml文件路径
     * @return Map
     */
    public static Map<String, Object> loadYamlAsPlainMap(String path) {
        Map<String, Object> data = loadYaml(path);
        Map<String, Object> plain = new LinkedHashMap<>();
        convertTo(data, "", plain);
        return plain;
    }

    /**
     * 将嵌套的Map结构转换为扁平化的键值对形式
     * 例如: {a: {b: c}} 转换为 {a.b: c}
     * @param source 原始的嵌套Map数据
     * @param prefix 当前处理的键前缀
     * @param plain  存储扁平化结果的Map
     */
    static void convertTo(Map<String, Object> source, String prefix, Map<String, Object> plain) {
        for (String key : source.keySet()) {
            Object value = source.get(key);
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) value;
                convertTo(subMap, prefix + key + ".", plain);
            } else if (value instanceof List) {
                plain.put(prefix + key, value);
            } else {
                plain.put(prefix + key, value.toString());
            }
        }
    }


}


/**
 * Disable All implicit convert and treat all values as string.
 */
class NoImplicitResolver extends Resolver {
    public NoImplicitResolver() {
        super();
        super.yamlImplicitResolvers.clear();
    }
}
