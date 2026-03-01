package com.elias.attendancecontrol.config;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final SecurityUtils securityUtils;
    private final LogService logService;

    private static final String ATTR_PATH = "path";
    private static final String ATTR_METHOD = "method";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_MESSAGE = "message";
    private static final String VIEW_ERROR = "exceptions/error";
    private static final String VIEW_ACCESS_DENIED = "exceptions/403";

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String username = securityUtils.getCurrentUser()
                .map(User::getUsername)
                .orElse("anonymous");

        log.warn("Access denied for user {} at path: {}, message: {}", username, path, e.getMessage());

        logService.log(builder -> builder
                .eventType("ACCESS_DENIED")
                .description("Intento de acceso no autorizado a: " + path)
                .organization(securityUtils.getCurrentOrganization().orElse(null))
                .user(securityUtils.getCurrentUser().orElse(null))
                .ipAddress(request.getRemoteAddr())
                .details("Method: " + method + ", Message: " + e.getMessage())
        );

        model.addAttribute(ATTR_PATH, path);
        model.addAttribute(ATTR_METHOD, method);
        model.addAttribute(ATTR_TIMESTAMP, LocalDateTime.now());
        model.addAttribute(ATTR_ERROR, "Acceso Denegado");
        model.addAttribute(ATTR_MESSAGE, "No tiene permisos para acceder a este recurso");

        return VIEW_ACCESS_DENIED;
    }

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
