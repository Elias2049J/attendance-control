package com.elias.attendancecontrol.controller;
import com.elias.attendancecontrol.config.SecurityUtils;
import com.elias.attendancecontrol.model.entity.Attendance;
import com.elias.attendancecontrol.model.entity.AttendanceStatus;
import com.elias.attendancecontrol.model.entity.QRToken;
import com.elias.attendancecontrol.model.entity.Session;
import com.elias.attendancecontrol.model.entity.User;
import com.elias.attendancecontrol.persistence.repository.QrTokenRepository;
import com.elias.attendancecontrol.service.AttendanceService;
import com.elias.attendancecontrol.service.EnrollmentService;
import com.elias.attendancecontrol.service.SessionService;
import com.elias.attendancecontrol.service.TokenService;
import com.elias.attendancecontrol.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final UserService userService;
    private final SessionService sessionService;
    private final SecurityUtils securityUtils;
    private final TokenService tokenService;
    private final QrTokenRepository qrTokenRepository;
    private final EnrollmentService enrollmentService;

    @GetMapping("/org/{orgSlug}/attendance/verify")
    public String verifyQRAndRegister(
            @PathVariable String orgSlug,
            @RequestParam String token,
            Model model,
            RedirectAttributes redirectAttributes) {
        log.debug("Verifying attendance with token: {} for organization slug: {}", token, orgSlug);
        try {
            User currentUser = securityUtils.getCurrentUserOrThrow();
            log.debug("User {} attempting to register attendance", currentUser.getUsername());

            if (!tokenService.validateQR(token)) {
                log.warn("Invalid or expired QR token: {}", token);
                redirectAttributes.addFlashAttribute("error", "Código QR inválido o expirado");
                return "redirect:/";
            }

            QRToken qrToken = qrTokenRepository.findByToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Token QR no encontrado"));

            Session session = qrToken.getSession();
            String sessionOrgSlug = session.getActivity().getOrganization().getSlug();

            if (!sessionOrgSlug.equals(orgSlug)) {
                log.warn("Organization slug mismatch. Expected: {}, Got: {}", sessionOrgSlug, orgSlug);
                redirectAttributes.addFlashAttribute("error", "Organización no válida para esta sesión");
                return "redirect:/";
            }

            Attendance attendance = attendanceService.registerAttendance(currentUser.getId(), token);

            model.addAttribute("attendance", attendance);
            model.addAttribute("session", session);
            model.addAttribute("activity", session.getActivity());
            model.addAttribute("user", currentUser);
            model.addAttribute("registrationTime", LocalDateTime.now());
            model.addAttribute("organizationSlug", orgSlug);

            log.info("Attendance registered successfully via QR link for user: {} in session: {} (org: {})",
                    currentUser.getUsername(), session.getId(), orgSlug);

            return "attendance/verify";
        } catch (IllegalArgumentException e) {
            log.error("Error verifying QR: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/";
        } catch (IllegalStateException e) {
            log.error("Cannot register attendance: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "No se puede registrar asistencia: " + e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            log.error("Unexpected error during QR verification", e);
            redirectAttributes.addFlashAttribute("error", "Error inesperado al procesar la solicitud");
            return "redirect:/";
        }
    }
    @GetMapping("/manual/{sessionId}")
    public String showManualRegistrationForm(@PathVariable Long sessionId, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing manual attendance registration form for session: {}", sessionId);
        try {
            Session session = sessionService.getSessionById(sessionId);

            if (session.getStatus() == com.elias.attendancecontrol.model.entity.SessionStatus.CLOSED) {
                redirectAttributes.addFlashAttribute("error", "La sesión está cerrada, no se puede editar");
                return "redirect:/sessions";
            }

            if (session.getStatus() != com.elias.attendancecontrol.model.entity.SessionStatus.ACTIVE) {
                redirectAttributes.addFlashAttribute("error", "La sesión no está activa");
                return "redirect:/sessions";
            }

            List<User> enrolledUsers;
            if (session.getActivity().getRequiresEnrollment() != null && session.getActivity().getRequiresEnrollment()) {
                enrolledUsers = enrollmentService.getEnrolledParticipants(session.getActivity().getId());
            } else {
                enrolledUsers = userService.listUsers();
            }

            List<Attendance> existingAttendances = attendanceService.getAttendanceBySession(sessionId);

            Map<Long, Attendance> existingAttendanceMap = new HashMap<>();
            for (Attendance att : existingAttendances) {
                existingAttendanceMap.put(att.getUser().getId(), att);
            }

            model.addAttribute("session", session);
            model.addAttribute("activity", session.getActivity());
            model.addAttribute("users", enrolledUsers);
            model.addAttribute("existingAttendances", existingAttendances);
            model.addAttribute("existingAttendanceMap", existingAttendanceMap);
            model.addAttribute("attendanceStatuses", AttendanceStatus.values());

            return "attendance/manual-registration";
        } catch (Exception e) {
            log.error("Error showing manual registration form: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/sessions";
        }
    }

    @PostMapping("/manual-registration")
    public String manualRegistrationBatch(@RequestParam Long sessionId,
                                          @RequestParam Map<String, String> allParams,
                                          RedirectAttributes redirectAttributes) {
        log.debug("Manual batch attendance registration for session: {}", sessionId);
        try {
            Map<Long, AttendanceStatus> userAttendances = new HashMap<>();

            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("attendance_")) {
                    Long userId = Long.parseLong(key.substring("attendance_".length()));
                    AttendanceStatus status = AttendanceStatus.valueOf(entry.getValue());
                    userAttendances.put(userId, status);
                }
            }

            if (userAttendances.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Debe marcar al menos una asistencia");
                return "redirect:/attendance/manual/" + sessionId;
            }

            List<Attendance> attendances = attendanceService.manualRegistrationBatch(sessionId, userAttendances);
            redirectAttributes.addFlashAttribute("success",
                String.format("Asistencia registrada exitosamente para %d usuario(s)", attendances.size()));

            return "redirect:/sessions";
        } catch (IllegalArgumentException e) {
            log.error("Error in manual batch registration: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/attendance/manual/" + sessionId;
        } catch (IllegalStateException e) {
            log.error("Cannot register batch attendance: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "No se puede registrar: " + e.getMessage());
            return "redirect:/attendance/manual/" + sessionId;
        } catch (Exception e) {
            log.error("Unexpected error in manual batch registration", e);
            redirectAttributes.addFlashAttribute("error", "Error inesperado: " + e.getMessage());
            return "redirect:/attendance/manual/" + sessionId;
        }
    }
    @GetMapping("/history")
    public String getAttendanceHistory(@RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) Long sessionId,
                                      Model model) {
        log.debug("Getting attendance history with filters - userId: {}, sessionId: {}", userId, sessionId);
        User currentUser = securityUtils.getCurrentUserOrThrow();
        List<Attendance> attendances;
        if (securityUtils.isSystemAdmin() || securityUtils.isOrganizationOwnerOrAdmin()) {
            attendances = attendanceService.listAttendance();
            model.addAttribute("users", userService.listUsers());
            model.addAttribute("canViewAll", true);
        } else {
            attendances = attendanceService.getAttendanceByUser(currentUser.getId());
            model.addAttribute("canViewAll", false);
        }
        model.addAttribute("attendances", attendances);
        model.addAttribute("sessions", sessionService.listSessions());
        model.addAttribute("currentUser", currentUser);
        return "attendance/history";
    }
}
