package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.service.AuthenticationService;
import com.elias.attendancecontrol.service.LogService;
import com.elias.attendancecontrol.service.TokenService;
import com.elias.attendancecontrol.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;
    private final LogService logService;
    private final TokenService tokenService;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("error", "Credenciales inválidas. Por favor, intente nuevamente.");
        }
        if (logout != null) {
            model.addAttribute("logout", true);
        }
        return "auth/login";
    }
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        securityUtils.getCurrentUser().ifPresent(user -> {
            log.info("User logging out: {}", user.getUsername());
            try {
                tokenService.invalidateUserSessionTokens(user.getUsername());
                log.debug("Session tokens invalidated for user: {}", user.getUsername());
                logService.log(builder -> builder
                    .eventType("USER_LOGOUT")
                    .description("Usuario cerró sesión")
                    .user(user)
                    .ipAddress(request.getRemoteAddr())
                );
            } catch (Exception e) {
                log.error("Error invalidating session tokens for user: {}", user.getUsername(), e);
            }
        });
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        redirectAttributes.addFlashAttribute("logout", true);
        return "redirect:/auth/login";
    }
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        log.debug("Showing registration form");
        model.addAttribute("user", new User());
        return "auth/register";
    }
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user,
                              @RequestParam String confirmPassword,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        log.debug("Attempting to register new user: {}", user.getUsername());
        try {
            if (!user.getPassword().equals(confirmPassword)) {
                model.addAttribute("error", "Las contraseñas no coinciden");
                model.addAttribute("user", user);
                return "auth/register";
            }
            userService.registerUser(user);
            log.info("User registered successfully: {}", user.getUsername());
            redirectAttributes.addFlashAttribute("success",
                "Registro exitoso. Por favor, inicie sesión con sus credenciales.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed for user: {} - {}", user.getUsername(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            return "auth/register";
        } catch (Exception e) {
            log.error("Error during registration for user: {}", user.getUsername(), e);
            model.addAttribute("error", "Error al registrar usuario. Por favor, intente nuevamente.");
            model.addAttribute("user", user);
            return "auth/register";
        }
    }
}
