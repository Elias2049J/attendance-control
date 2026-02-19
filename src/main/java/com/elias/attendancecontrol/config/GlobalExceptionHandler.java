package com.elias.attendancecontrol.config;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDateTime;
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String ATTR_PATH = "path";
    private static final String ATTR_METHOD = "method";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_MESSAGE = "message";
    private static final String VIEW_ERROR = "exceptions/error";
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        model.addAttribute(ATTR_PATH, path);
        model.addAttribute(ATTR_METHOD, method);
        model.addAttribute(ATTR_TIMESTAMP, LocalDateTime.now());
        model.addAttribute(ATTR_ERROR, "Solicitud Inválida");
        model.addAttribute(ATTR_MESSAGE, e.getMessage());
        log.warn("IllegalArgumentException occurred at path: {}, message: {}", path, e.getMessage());
        return VIEW_ERROR;
    }
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException e, HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        model.addAttribute(ATTR_PATH, path);
        model.addAttribute(ATTR_METHOD, method);
        model.addAttribute(ATTR_TIMESTAMP, LocalDateTime.now());
        model.addAttribute(ATTR_ERROR, "Estado Conflictivo");
        model.addAttribute(ATTR_MESSAGE, e.getMessage());
        log.warn("IllegalStateException occurred at path: {}, message: {}", path, e.getMessage());
        return VIEW_ERROR;
    }
    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception e, HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        model.addAttribute(ATTR_PATH, path);
        model.addAttribute(ATTR_METHOD, method);
        model.addAttribute(ATTR_TIMESTAMP, LocalDateTime.now());
        model.addAttribute(ATTR_ERROR, "Error Interno del Servidor");
        model.addAttribute(ATTR_MESSAGE, e.getMessage());
        log.error("An Unexpected Exception occurred at path: {}, method: {}, error: {}", path, method, e.getMessage(), e);
        return VIEW_ERROR;
    }
}
