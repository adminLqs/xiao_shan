package com.xiaoshan.springbootdemo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.sql.SQLIntegrityConstraintViolationException;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 构建异常响应头（强制设置为 JSON 格式）
     */
    private HttpHeaders buildJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.inline().build());
        return headers;
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.error("文件上传大小超限: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "文件大小超过限制");
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .headers(buildJsonHeaders())
                .body(response);
    }

    /**
     * 处理参数校验异常（@Valid 校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError) {
                        return ((FieldError) error).getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining("；"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", errorMessage);
        
        return ResponseEntity.badRequest()
                .headers(buildJsonHeaders())
                .body(response);
    }

    /**
     * 处理 SQL 约束冲突异常
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<?> handleSQLConstraintViolation(SQLIntegrityConstraintViolationException ex) {
        log.error("数据关联冲突: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "数据关联冲突");
        
        return ResponseEntity.badRequest()
                .headers(buildJsonHeaders())
                .body(response);
    }

    /**
     * 处理请求参数格式错误异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("请求参数格式错误: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "请求参数格式错误");
        
        return ResponseEntity.badRequest()
                .headers(buildJsonHeaders())
                .body(response);
    }

    /**
     * 处理缺少必要参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.error("缺少必要参数: {}", ex.getParameterName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "缺少必要参数: " + ex.getParameterName());
        
        return ResponseEntity.badRequest()
                .headers(buildJsonHeaders())
                .body(response);
    }

    /**
     * 处理客户端连接断开异常（正常行为，不记录ERROR日志）
     * 
     * Connection reset by peer: 客户端主动断开连接
     * ClosedChannelException: 通道已关闭，无法写入响应
     * IOException: 网络IO异常，通常是客户端断开导致
     */
    @ExceptionHandler({ClosedChannelException.class, IOException.class})
    public ResponseEntity<?> handleClientDisconnect(Exception ex) {
        String message = ex.getMessage();
        
        // 仅在DEBUG级别记录，方便排查问题但不影响正常日志
        if (message != null && (message.contains("Connection reset by peer") || 
                                message.contains("Broken pipe") || 
                                message.contains("ClientAbortException"))) {
            log.debug("客户端断开连接: {}", message);
        } else if (ex instanceof ClosedChannelException) {
            log.debug("通道已关闭（客户端断开）");
        }
        
        // 返回空响应或204 No Content，避免尝试写入已关闭的连接
        return ResponseEntity.noContent().build();
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        // 过滤客户端断连导致的 RuntimeException（如 Undertow 的 ConnectionResetException）
        String message = ex.getMessage();
        if (message != null && (message.contains("Connection reset") || 
                                message.contains("ClosedChannelException") ||
                                message.contains("Broken pipe"))) {
            log.debug("客户端断开连接导致的运行时异常: {}", message);
            return ResponseEntity.noContent().build();
        }
        
        log.error("业务异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "操作失败");
        
        return ResponseEntity.ok()
                .headers(buildJsonHeaders())
                .body(response);
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        // 过滤已处理过的异常
        if (ex instanceof IOException || ex instanceof ClosedChannelException) {
            return ResponseEntity.noContent().build();
        }
        
        log.error("系统异常", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "系统繁忙，请稍后重试");
        
        return ResponseEntity.ok()
                .headers(buildJsonHeaders())
                .body(response);
    }
}