package com.lms.common.web;

import java.math.BigDecimal;
import java.util.List;

public record ChartDataset(String label, List<BigDecimal> values) {
}
