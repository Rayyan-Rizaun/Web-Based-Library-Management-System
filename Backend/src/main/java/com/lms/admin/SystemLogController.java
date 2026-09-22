package com.lms.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lms.common.domain.BackupStatus;
import com.lms.common.domain.BackupType;
import com.lms.common.web.DataTableColumn;
import com.lms.common.web.SelectOption;

@Controller
@PreAuthorize("hasAuthority('Library Administrator')")
public class SystemLogController {

    private final SystemLogService systemLogService;

    public SystemLogController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @GetMapping("/admin/logs")
    public String index(@RequestParam(defaultValue = "audit") String tab,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        model.addAttribute("pageTitle", "System Logs");
        model.addAttribute("tab", tab);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("statusOptions", statusOptions());
        model.addAttribute("typeOptions", typeOptions());

        LocalDateTime fromAt = startOf(from);
        LocalDateTime toAt = endOf(to);

        switch (tab) {
            case "failed-logins" -> {
                Page<?> result = systemLogService.failedLogins(q, fromAt, toAt, page);
                model.addAttribute("columns", List.of(
                        DataTableColumn.left("Email tried"),
                        DataTableColumn.left("Reason"),
                        DataTableColumn.left("IP address"),
                        DataTableColumn.right("Attempted at")));
                populatePage(model, result);
            }
            case "backups" -> {
                Page<?> result = systemLogService.backupLogs(status, type, fromAt, toAt, page);
                model.addAttribute("columns", List.of(
                        DataTableColumn.left("Type"),
                        DataTableColumn.left("Status"),
                        DataTableColumn.right("Started at"),
                        DataTableColumn.right("Duration"),
                        DataTableColumn.left("Error")));
                populatePage(model, result);
            }
            default -> {
                model.addAttribute("tab", "audit");
                Page<?> result = systemLogService.auditLog(q, fromAt, toAt, page);
                model.addAttribute("columns", List.of(
                        DataTableColumn.left("User"),
                        DataTableColumn.left("Action"),
                        DataTableColumn.left("Entity"),
                        DataTableColumn.left("Entity ID"),
                        DataTableColumn.right("Occurred at")));
                populatePage(model, result);
            }
        }

        return "admin/logs";
    }

    private static void populatePage(Model model, Page<?> result) {
        model.addAttribute("rows", result.getContent());
        long totalItems = result.getTotalElements();
        int totalPages = Math.max(result.getTotalPages(), 1);
        int page = result.getNumber() + 1;
        int pageSize = result.getSize();
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("firstItem", totalItems == 0 ? 0 : (long) (page - 1) * pageSize + 1);
        model.addAttribute("lastItem", Math.min((long) page * pageSize, totalItems));
    }

    private static LocalDateTime startOf(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date).atStartOfDay();
    }

    private static LocalDateTime endOf(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date).atTime(LocalTime.MAX).withNano(0).plusSeconds(1);
    }

    private static List<SelectOption> statusOptions() {
        List<SelectOption> options = new ArrayList<>();
        options.add(new SelectOption("", "All statuses"));
        for (BackupStatus s : BackupStatus.values()) {
            options.add(new SelectOption(s.name(), s.name()));
        }
        return options;
    }

    private static List<SelectOption> typeOptions() {
        List<SelectOption> options = new ArrayList<>();
        options.add(new SelectOption("", "All types"));
        for (BackupType t : BackupType.values()) {
            options.add(new SelectOption(t.name(), t.name()));
        }
        return options;
    }
}
