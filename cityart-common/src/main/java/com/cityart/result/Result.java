package com.cityart.result;


import com.cityart.constant.MessageConstant;
import lombok.Data;

import java.io.Serializable;

@Data
public class Result< T > implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success(){
        Result<T> result = new Result<>();
        result.msg= MessageConstant.SUCCESS;
        result.code=1;
        return result;
    }

    public static <T> Result<T> success(T object){
        Result<T> result = new Result<>();
        result.code=1;
        result.msg= MessageConstant.SUCCESS;
        result.data=object;
        return result;

    }

    /**
     * 成功，但提示文案不是「操作成功」（例如「下单成功」「已取消」），且不返回数据
     */
    public static <T> Result<T> successMsg(String msg){
        Result<T> result = new Result<>();
        result.code=1;
        result.msg=msg;
        return result;
    }

    /**
     * 成功，自定义提示文案 + 返回数据
     */
    public static <T> Result<T> success(String msg, T object){
        Result<T> result = new Result<>();
        result.code=1;
        result.msg=msg;
        result.data=object;
        return result;
    }

    public static <T> Result<T> error(String msg){
        Result result = new Result<>();
        result.code=0;
        result.msg=msg;
        return result;
    }

    /**
     * 失败时也想返回数据（例如「艺览智荐」失败时返回空数组 []，前端直接隐藏该区块）
     */
    public static <T> Result<T> error(String msg, T object){
        Result<T> result = new Result<>();
        result.code=0;
        result.msg=msg;
        result.data=object;
        return result;
    }


}
