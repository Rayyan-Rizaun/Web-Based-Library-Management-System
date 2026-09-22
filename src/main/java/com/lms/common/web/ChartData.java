package com.lms.common.web;

import java.util.List;

public record ChartData(List<String> labels, List<ChartDataset> datasets) {

    public boolean isEmpty() {
        return labels.isEmpty() || datasets.stream().allMatch(d -> d.values().stream().allMatch(v -> v.signum() == 0));
    }
}
