package com.ecommerce.core.util;

import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean复制工具类
 * 基于Spring BeanUtils的封装，提供对象和集合的复制功能
 * 注意：复杂类型转换建议使用MapStruct
 */
public class BeanCopyUtils {

    public static <T> T copy(Object source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean复制失败", e);
        }
    }

    public static <T> List<T> copyList(List<?> sourceList, Class<T> targetClass) {
        if (sourceList == null) return List.of();
        return sourceList.stream()
                .map(s -> copy(s, targetClass))
                .collect(Collectors.toList());
    }
}
