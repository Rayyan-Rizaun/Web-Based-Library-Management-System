package com.lms.common.domain;

/**
 * {@code MemberFeedback.Status} — CK_MemberFeedback_Status. Also used for
 * {@code FeedbackHistory.PreviousStatus / NewStatus}, whose checks share
 * this domain (business-rules §6 lifecycle).
 *
 * <p>Some stored values contain a space, which no Java constant name can,
 * so the column is mapped with {@link JpaConverter} rather than
 * {@code @Enumerated} — see {@link DbValueEnum}.
 */
public enum FeedbackStatus implements DbValueEnum {

    Submitted("Submitted"),
    UnderReview("Under Review"),
    InProgress("In Progress"),
    Resolved("Resolved"),
    Closed("Closed");

    private final String dbValue;

    FeedbackStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @jakarta.persistence.Converter
    public static class JpaConverter extends DbValueEnumConverter<FeedbackStatus> {
        public JpaConverter() {
            super(FeedbackStatus.class);
        }
    }
}
