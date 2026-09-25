package com.example.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获参数校验异常、业务异常与系统异常，转换为 Result 格式响应，
 * 避免异常堆栈直接暴露给前端
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @RequestBody 请求体参数校验失败异常
     *
     * @param e 请求体参数校验异常
     * @return 统一返回结果（400，提示第一条校验错误信息）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String errorMsg = fieldError != null ? fieldError.getDefaultMessage() : ResultCodeEnum.PARAM_ERROR.getMsg();
        log.warn("请求体参数校验失败：{}", errorMsg);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), errorMsg);
    }

    /**
     * 处理表单对象绑定参数校验失败异常
     *
     * @param e 表单绑定校验异常
     * @return 统一返回结果（400，提示第一条校验错误信息）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String errorMsg = fieldError != null ? fieldError.getDefaultMessage() : ResultCodeEnum.PARAM_ERROR.getMsg();
        log.warn("参数绑定校验失败：{}", errorMsg);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), errorMsg);
    }

    /**
     * 处理单个参数校验失败异常（类上标注 @Validated 时对方法参数的校验）
     *
     * @param e 约束校验异常
     * @return 统一返回结果（400，提示全部校验错误信息）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("方法参数校验失败：{}", errorMsg);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), errorMsg);
    }

    /**
     * 处理上传文件超过大小限制异常
     * <p>
     * 该异常由 Spring 在进入 Controller 之前抛出，若不单独处理会被兜底成
     * 501「系统内部错误」，前端只能提示一句无从下手的"上传失败"
     *
     * @param e 文件超限异常
     * @return 统一返回结果（400，提示单张图片的大小上限）
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制：{}", e.getMessage());
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), "图片大小超过限制，单张不能超过 5MB");
    }

    /**
     * 处理自定义业务异常（业务逻辑中主动抛出）
     *
     * @param e 业务异常
     * @return 统一返回结果（业务异常携带的状态码与提示信息）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理未知系统异常（兜底，避免堆栈信息泄露给前端）
     *
     * @param e 系统异常
     * @return 统一返回结果（501 系统内部错误）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR);
    }
}
