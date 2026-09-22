package com.lms.common.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link SystemSetting}, keyed by setting name. Use the inherited
 * {@code findById("Loan.PeriodDays")}; no extra queries are needed (CLAUDE.md rule 9).
 */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {

    List<SystemSetting> findAllByOrderBySettingKey();
}
