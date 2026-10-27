package com.woori.wonnie.api;

import com.woori.wonnie.api.ApiDtos.DashboardResponse;
import com.woori.wonnie.auth.CurrentCustomer;
import com.woori.wonnie.dashboard.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentCustomer currentCustomer;

    public DashboardController(DashboardService dashboardService, CurrentCustomer currentCustomer) {
        this.dashboardService = dashboardService;
        this.currentCustomer = currentCustomer;
    }

    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard(currentCustomer.id());
    }
}
