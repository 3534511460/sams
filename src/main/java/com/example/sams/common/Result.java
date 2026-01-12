package com.example.sams.common;



import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 200成功，500失败
    private String msg;   // 提示信息
    private T data;       // 返回的数据

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    public static Result error(String msg) {
        Result r = new Result();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }
}