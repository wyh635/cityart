package com.cityart.handler;


import com.cityart.constant.MessageConstant;
import com.cityart.exception.BaseException;
import com.cityart.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理参数校验异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(MethodArgumentNotValidException ex){
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("参数校验异常：{}", message);
        return Result.error(message);
    }

    /**
     * 处理「查询参数绑定到 DTO」时的校验异常
     * （例如 /api/app/exhibitions/page?pageSize=999、status=xxx、type=abc）
     * 注意：DTO 参数上要有 @Validated / @Valid 才会触发
     */
    @ExceptionHandler
    public Result exceptionHandler(BindException ex){
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError == null) {
            return Result.error(MessageConstant.PARAM_ERROR);
        }
        // 类型不对（如 type=abc）时没有自定义提示语，统一给一句人话
        if ("typeMismatch".equals(fieldError.getCode())) {
            log.error("参数类型错误：{}", fieldError.getField());
            return Result.error(fieldError.getField() + " " + MessageConstant.PARAM_ERROR);
        }
        log.error("参数校验异常：{}", fieldError.getDefaultMessage());
        return Result.error(fieldError.getDefaultMessage());
    }

    /**
     * 处理 @RequestParam 上的校验异常（例如 limit 超过 20、city 太长）
     * 注意：Controller 类上必须加 @Validated 才会触发
     */
    @ExceptionHandler
    public Result exceptionHandler(ConstraintViolationException ex){
        String message = ex.getConstraintViolations().iterator().next().getMessage();
        log.error("参数校验异常：{}", message);
        return Result.error(message);
    }

    /**
     * 处理参数类型不匹配（例如 limit=abc）
     */
    @ExceptionHandler
    public Result exceptionHandler(MethodArgumentTypeMismatchException ex){
        log.error("参数类型错误：{}", ex.getMessage());
        return Result.error(ex.getName() + " " + MessageConstant.PARAM_ERROR);
    }

    /**
     * 兜底：其他未预料到的异常统一返回「系统异常」，不把堆栈暴露给前端
     */
    @ExceptionHandler
    public Result exceptionHandler(Exception ex){
        log.error("系统异常：{}", ex.getMessage(), ex);
        return Result.error(MessageConstant.SYSTEM_ERROR);
    }

    /**
     * 处理SQL异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        //Duplicate entry 'zhangsan' for key 'employee.idx_username'
        String message = ex.getMessage();
        if(message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }
}
