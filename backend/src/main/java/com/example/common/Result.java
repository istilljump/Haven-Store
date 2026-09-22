package com.example.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果类
 * <p>
 * 所有接口统一返回该结构：{code: 状态码, msg: 提示信息, data: 数据}
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Data
@ApiModel(value = "Result", description = "统一返回结果：{code, msg, data}")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 响应状态码：200 成功，其余为失败（见 ResultCodeEnum） */
    @ApiModelProperty(value = "响应状态码，200 表示成功", example = "200")
    private Integer code;

    /** 响应提示信息 */
    @ApiModelProperty(value = "响应提示信息", example = "成功")
    private String msg;

    /** 响应数据，无数据时为 null */
    @ApiModelProperty("响应数据")
    private T data;

    /**
     * 成功返回（无数据）
     *
     * @param <T> 数据泛型
     * @return 统一返回结果
     */
    public static <T> Result<T> success() {
        return build(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg(), null);
    }

    /**
     * 成功返回（携带数据）
     *
     * @param <T>  数据泛型
     * @param data 响应数据
     * @return 统一返回结果
     */
    public static <T> Result<T> success(T data) {
        return build(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg(), data);
    }

    /**
     * 失败返回（自定义状态码与提示信息）
     *
     * @param <T> 数据泛型
     * @param code 状态码
     * @param msg  提示信息
     * @return 统一返回结果
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        return build(code, msg, null);
    }

    /**
     * 失败返回（使用状态码枚举）
     *
     * @param <T>            数据泛型
     * @param resultCodeEnum 状态码枚举
     * @return 统一返回结果
     */
    public static <T> Result<T> fail(ResultCodeEnum resultCodeEnum) {
        return build(resultCodeEnum.getCode(), resultCodeEnum.getMsg(), null);
    }

    /**
     * 判断本次返回是否为成功状态
     * <p>
     * 便于业务代码与测试断言直接书写 {@code result.isSuccess()}，无需硬编码状态码 200
     *
     * @return true 表示状态码为 SUCCESS
     */
    public boolean isSuccess() {
        return ResultCodeEnum.SUCCESS.getCode().equals(code);
    }

    /**
     * 构建统一返回结果的私有工具方法
     *
     * @param <T>  数据泛型
     * @param code 状态码
     * @param msg  提示信息
     * @param data 响应数据
     * @return 统一返回结果
     */
    private static <T> Result<T> build(Integer code, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}
