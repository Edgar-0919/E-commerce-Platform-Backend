package com.ecommerce.core.model;

import lombok.Data;

/**
 * 统一响应封装类
 * 所有API接口返回此格式，确保响应结构一致
 * 
 * @param <T> 响应数据泛型
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;
    // 时间戳在构造时自动设置，避免调用方遗漏
    private long timestamp;

    // 私有构造器，强制通过静态工厂方法创建，确保格式统一
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "操作成功";
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "操作成功";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = data;
        return r;
    }
}
