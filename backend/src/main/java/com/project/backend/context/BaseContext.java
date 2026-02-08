package com.project.backend.context;

/**
 * 用户上下文持有器
 * 使用 ThreadLocal 存储当前请求的用户信息
 */
public class BaseContext {

    private static final ThreadLocal<Long> currentId = new ThreadLocal<>();

    /**
     * 设置当前用户 ID
     */
    public static void setCurrentId(Long id) {
        currentId.set(id);
    }

    /**
     * 获取当前用户 ID
     */
    public static Long getCurrentId() {
        return currentId.get();
    }

    /**
     * 清除当前用户信息
     */
    public static void remove() {
        currentId.remove();
    }
}
