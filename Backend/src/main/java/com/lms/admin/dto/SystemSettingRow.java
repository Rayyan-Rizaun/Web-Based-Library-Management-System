package com.lms.admin.dto;

import java.time.LocalDateTime;

public record SystemSettingRow(String settingKey, String settingValue, String dataType, String description,
        String updatedByName, LocalDateTime updatedAt) {
}
