package com.lms.admin.dto;

import java.util.List;

public record SystemSettingGroup(String prefix, List<SystemSettingRow> rows) {
}
