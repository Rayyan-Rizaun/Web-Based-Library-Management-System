package com.lms.admin;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lms.admin.dto.SystemSettingForm;
import com.lms.common.security.AppUserPrincipal;
import com.lms.common.web.DataTableColumn;

@Controller
@PreAuthorize("hasAuthority('Library Administrator')")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/admin/settings")
    public String index(Model model) {
        model.addAttribute("pageTitle", "System Settings");
        model.addAttribute("groups", systemSettingService.grouped());
        model.addAttribute("columns", List.of(
                DataTableColumn.left("Key"),
                DataTableColumn.right("Value"),
                DataTableColumn.left("Type"),
                DataTableColumn.left("Description"),
                DataTableColumn.left("Updated by"),
                DataTableColumn.right("Updated at"),
                DataTableColumn.left("")));
        return "admin/settings";
    }

    @PostMapping("/admin/settings/{key}")
    public String update(@PathVariable String key, @Valid @ModelAttribute("settingForm") SystemSettingForm form,
            BindingResult bindingResult, @AuthenticationPrincipal AppUserPrincipal principal,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashErrorTitle", "Cannot update this setting");
            redirectAttributes.addFlashAttribute("flashErrorMessage", "Enter a value for this setting.");
            return "redirect:/admin/settings";
        }
        try {
            systemSettingService.update(key, form, principal.userId());
            redirectAttributes.addFlashAttribute("flashSuccessTitle", "Setting updated");
        } catch (SystemSettingException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("flashErrorTitle", "Cannot update this setting");
            redirectAttributes.addFlashAttribute("flashErrorMessage", e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String notFound(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("flashErrorTitle", "Setting not found");
        redirectAttributes.addFlashAttribute("flashErrorMessage", "That setting may no longer exist.");
        return "redirect:/admin/settings";
    }
}
