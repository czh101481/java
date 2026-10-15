package com.fenglin.springboottest.common;

import lombok.Data;

/**
 * 统一接口返回体
 * 前端约定：code=200 表示成功，data 为业务数据
 */
@Data
public class Result<T> {

    /** 200=成功，其余为错误码 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
