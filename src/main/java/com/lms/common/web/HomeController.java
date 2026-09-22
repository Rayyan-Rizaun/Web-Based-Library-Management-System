package com.lms.common.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.lms.common.domain.MemberRepository;
import com.lms.common.security.AppUserPrincipal;
import com.lms.dashboard.AdminDashboardService;
import com.lms.dashboard.FinanceDashboardService;
import com.lms.dashboard.LibrarianDashboardService;
import com.lms.dashboard.MemberDashboardService;

@Controller
public class HomeController {

    private final AdminDashboardService adminDashboardService;
    private final LibrarianDashboardService librarianDashboardService;
    private final FinanceDashboardService financeDashboardService;
    private final MemberDashboardService memberDashboardService;
    private final MemberRepository members;

    public HomeController(AdminDashboardService adminDashboardService, LibrarianDashboardService librarianDashboardService,
            FinanceDashboardService financeDashboardService, MemberDashboardService memberDashboardService,
            MemberRepository members) {
        this.adminDashboardService = adminDashboardService;
        this.librarianDashboardService = librarianDashboardService;
        this.financeDashboardService = financeDashboardService;
        this.memberDashboardService = memberDashboardService;
        this.members = members;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("pageTitle", "Dashboard");

        if (principal.hasAnyRole("Library Administrator")) {
            model.addAttribute("dashboard", adminDashboardService.view());
            return "home/admin";
        }
        if (principal.hasAnyRole("Librarian")) {
            model.addAttribute("dashboard", librarianDashboardService.view());
            return "home/librarian";
        }
        if (principal.hasAnyRole("Finance Officer")) {
            model.addAttribute("dashboard", financeDashboardService.view());
            return "home/finance";
        }
        Integer memberId = members.findByUserUserId(principal.userId()).map(m -> m.getMemberId()).orElse(null);
        model.addAttribute("dashboard", memberId == null ? null : memberDashboardService.view(memberId));
        return "home/member";
    }
}
