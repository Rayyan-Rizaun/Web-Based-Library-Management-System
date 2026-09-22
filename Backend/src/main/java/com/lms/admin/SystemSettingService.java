package com.lms.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.admin.dto.SystemSettingForm;
import com.lms.admin.dto.SystemSettingGroup;
import com.lms.admin.dto.SystemSettingRow;
import com.lms.common.domain.AppUserRepository;
import com.lms.common.domain.SettingDataType;
import com.lms.common.domain.SystemSetting;
import com.lms.common.domain.SystemSettingRepository;
import com.lms.common.security.AuditAction;

@Service
@Transactional
public class SystemSettingService {

    private static final String ADMIN_ROLE = "hasAuthority('Library Administrator')";

    private final SystemSettingRepository settings;
    private final AppUserRepository appUsers;

    public SystemSettingService(SystemSettingRepository settings, AppUserRepository appUsers) {
        this.settings = settings;
        this.appUsers = appUsers;
    }

    @PreAuthorize(ADMIN_ROLE)
    @Transactional(readOnly = true)
    public List<SystemSettingGroup> grouped() {
        Map<String, List<SystemSettingRow>> byPrefix = new LinkedHashMap<>();
        for (SystemSetting setting : settings.findAllByOrderBySettingKey()) {
            String prefix = setting.getSettingKey().split("\\.")[0];
            byPrefix.computeIfAbsent(prefix, key -> new ArrayList<>()).add(toRow(setting));
        }
        return byPrefix.entrySet().stream()
                .map(e -> new SystemSettingGroup(e.getKey(), e.getValue()))
                .toList();
    }

    private SystemSettingRow toRow(SystemSetting setting) {
        String updatedByName = setting.getUpdatedBy() == null ? "System"
                : setting.getUpdatedBy().getFirstName() + " " + setting.getUpdatedBy().getLastName();
        return new SystemSettingRow(setting.getSettingKey(), setting.getSettingValue(), setting.getSettingDataType().name(),
                setting.getDescription(), updatedByName, setting.getUpdatedAt());
    }

    @PreAuthorize(ADMIN_ROLE)
    @AuditAction(action = "UPDATE", entity = "SystemSetting")
    public SystemSetting update(String settingKey, SystemSettingForm form, Integer staffUserId) {
        SystemSetting setting = settings.findById(settingKey)
                .orElseThrow(() -> new NoSuchElementException("Setting not found"));
        String value = form.getValue().trim();
        validate(setting.getSettingDataType(), value);
        setting.setSettingValue(value);
        setting.setUpdatedBy(appUsers.getReferenceById(staffUserId));
        return settings.save(setting);
    }

    private static void validate(SettingDataType dataType, String value) {
        switch (dataType) {
            case Int -> {
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new SystemSettingException("This setting requires a whole number.");
                }
            }
            case Decimal -> {
                BigDecimal decimal;
                try {
                    decimal = new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw new SystemSettingException("This setting requires a decimal number.");
                }
                if (decimal.compareTo(BigDecimal.ZERO) < 0) {
                    throw new SystemSettingException("This setting cannot be negative.");
                }
            }
            case Bool -> {
                if (!"0".equals(value) && !"1".equals(value)) {
                    throw new SystemSettingException("This setting must be 0 or 1.");
                }
            }
            case String -> {
            }
        }
    }
}
