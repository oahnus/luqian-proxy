package com.github.oahnus.proxycommon;

import lombok.Data;

/**
 * Created by oahnus on 2019/9/20
 * 16:36.
 */
@Data
public class RespData<T> {
    private int code;
    private String msg;
    private T data;

    public RespData() {
        this.code = 0;
        this.msg = "success";
    }

    public static <T> RespData<T> success(T data) {
        RespData<T> respData = new RespData<>();
        respData.setData(data);
        return respData;
    }
    public static <T> RespData<T> success() {
        RespData<T> respData = new RespData<>();
        respData.setData(null);
        return respData;
    }

    public static RespData error(int code, String msg) {
        RespData respData = new RespData();
        respData.setCode(code);
        respData.setMsg(msg);
        return respData;
    }

    public RespData<T> data(T data) {
        this.data = data;
        return this;
    }
}
