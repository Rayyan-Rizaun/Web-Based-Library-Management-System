package com.lms.user;

import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lms.common.web.DataTableColumn;
import com.lms.user.dto.AllMemberRow;
import com.lms.user.dto.PendingMemberRow;
import com.lms.user.dto.RejectMemberForm;
import com.lms.user.dto.SuspendMemberForm;

/**
 * UC-01, backlog item PB-09 — the "Users &amp; Roles" section: member
 * approval and the full member list. Library-Administrator-only throughout,
 * matching the sidebar's own gate on this whole section and {@link
 * MemberAdminService}'s {@code @PreAuthorize} on every method here.
 */
@Controller
@PreAuthorize("hasAuthority('Library Administrator')")
public class MemberAdminController {

    private final MemberAdminService memberAdminService;

    public MemberAdminController(MemberAdminService memberAdminService) {
        this.memberAdminService = memberAdminService;
    }

    /** The "Users & Roles" sidebar link lands here, then hands off to whichever screen needs attention. */
    @GetMapping("/users")
    public String index() {
        return memberAdminService.pendingCount() > 0 ? "redirect:/users/pending" : "redirect:/users/all";
    }

    @GetMapping("/users/pending")
    public String pending(@RequestParam(defaultValue = "pending") String view, Model model) {
        boolean rejectedView = "rejected".equals(view);
        model.addAttribute("pageTitle", "Membership Approval");
        model.addAttribute("view", rejectedView ? "rejected" : "pending");
        model.addAttribute("pendingCount", memberAdminService.pendingMembers().size());
        model.addAttribute("rejectedCount", memberAdminService.rejectedMembers().size());

        List<PendingMemberRow> rows = rejectedView ? memberAdminService.rejectedMembers() : memberAdminService.pendingMembers();
        model.addAttribute("columns", List.of(
                DataTableColumn.left("Name"),
                DataTableColumn.left("Membership no."),
                DataTableColumn.left("Email"),
                DataTableColumn.left("NIC"),
                DataTableColumn.left("Contact"),
                DataTableColumn.left("Member type"),
                DataTableColumn.right("Registered"),
                DataTableColumn.left("")));
        model.addAttribute("rows", rows);
        return "user/pending-members";
    }

    @PostMapping("/users/pending/{id}/approve")
    public String approve(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            memberAdminService.approve(id);
            redirectAttributes.addFlashAttribute("flashSuccessTitle", "Membership approved");
            redirectAttributes.addFlashAttribute("flashSuccessMessage", "The member can now borrow books.");
        } catch (MemberAdminException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("flashErrorTitle", "Cannot approve this registration");
            redirectAttributes.addFlashAttribute("flashErrorMessage", e.getMessage());
        }
        return "redirect:/users/pending";
    }

    @PostMapping("/users/pending/{id}/reject")
    public String reject(@PathVariable Integer id, @Valid @ModelAttribute("rejectForm") RejectMemberForm form,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashErrorTitle", "Cannot reject this registration");
            redirectAttributes.addFlashAttribute("flashErrorMessage", "A reason is required to reject a registration.");
            return "redirect:/users/pending";
        }
        try {
            memberAdminService.reject(id, form);
            redirectAttributes.addFlashAttribute("flashSuccessTitle", "Registration rejected");
        } catch (MemberAdminException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("flashErrorTitle", "Cannot reject this registration");
            redirectAttributes.addFlashAttribute("flashErrorMessage", e.getMessage());
        }
        return "redirect:/users/pending";
    }

    @GetMapping("/users/all")
    public String all(@RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String memberType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "joinedDate") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model) {

        Page<AllMemberRow> result = memberAdminService.allMembers(q, status, memberType, page, sort, dir);

        model.addAttribute("pageTitle", "All Members");
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("memberType", memberType);
        model.addAttribute("statusOptions", MemberAdminService.statusFilterOptions());
        model.addAttribute("memberTypeOptions", MemberAdminService.memberTypeFilterOptions());

        model.addAttribute("columns", List.of(
                DataTableColumn.left("Name"),
                DataTableColumn.left("Membership no.", "membershipNo"),
                DataTableColumn.left("Email"),
                DataTableColumn.left("Member type", "type"),
                DataTableColumn.left("Status", "status"),
                DataTableColumn.right("Registered", "joined"),
                DataTableColumn.left("")));
        model.addAttribute("rows", result.getContent());
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDir", dir);

        long totalItems = result.getTotalElements();
        int totalPages = Math.max(result.getTotalPages(), 1);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", MemberAdminService.PAGE_SIZE);
        model.addAttribute("firstItem", totalItems == 0 ? 0 : (long) (page - 1) * MemberAdminService.PAGE_SIZE + 1);
        model.addAttribute("lastItem", Math.min((long) page * MemberAdminService.PAGE_SIZE, totalItems));

        return "user/all-members";
    }

    @PostMapping("/users/all/{id}/suspend")
    public String suspend(@PathVariable Integer id, @Valid @ModelAttribute("suspendForm") SuspendMemberForm form,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashErrorTitle", "Cannot suspend this member");
            redirectAttributes.addFlashAttribute("flashErrorMessage", "A reason is required to suspend a member.");
            return "redirect:/users/all";
        }
        try {
            memberAdminService.suspend(id, form);
            redirectAttributes.addFlashAttribute("flashSuccessTitle", "Member suspended");
        } catch (MemberAdminException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("flashErrorTitle", "Cannot suspend this member");
            redirectAttributes.addFlashAttribute("flashErrorMessage", e.getMessage());
        }
        return "redirect:/users/all";
    }

    @PostMapping("/users/all/{id}/reactivate")
    public String reactivate(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            memberAdminService.reactivate(id);
            redirectAttributes.addFlashAttribute("flashSuccessTitle", "Member reactivated");
        } catch (MemberAdminException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("flashErrorTitle", "Cannot reactivate this member");
            redirectAttributes.addFlashAttribute("flashErrorMessage", e.getMessage());
        }
        return "redirect:/users/all";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String notFound(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("flashErrorTitle", "Member not found");
        redirectAttributes.addFlashAttribute("flashErrorMessage", "That member may no longer exist.");
        return "redirect:/users/all";
    }
}
