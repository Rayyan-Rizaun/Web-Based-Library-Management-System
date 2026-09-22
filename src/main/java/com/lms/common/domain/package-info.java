/**
 * Shared JPA entities and Spring Data repositories (CLAUDE.md rule 2).
 *
 * <p>The schema is defined ONLY in {@code database/01_schema.sql} (CLAUDE.md
 * rule 1). Every class here is mapped to that file, never the other way
 * round: {@code spring.jpa.hibernate.ddl-auto=validate} stops the application
 * from starting if an entity and a table disagree. To change a column, edit
 * the SQL and re-run it, then edit the entity.
 *
 * <h2>Naming</h2>
 * Tables and columns are PascalCase in SQL and are named explicitly with
 * {@code @Table} / {@code @Column}; Java fields are camelCase (CLAUDE.md
 * rule 8). {@code com.lms.common.config.PersistenceConfig} turns off Spring
 * Boot's default snake_case renaming, so "AppUser" is not looked up as
 * "app_user".
 *
 * <h2>Enums</h2>
 * Every column with a value-list CHECK constraint is a Java enum. The
 * constants spell the stored values exactly — which is why they are written
 * {@code Active}, not {@code ACTIVE}: {@code @Enumerated(EnumType.STRING)}
 * stores the constant's name. Five domains contain a space ("On Loan",
 * "Academic Staff", …) that no Java constant can hold; those enums implement
 * {@link com.lms.common.domain.DbValueEnum} and use a converter instead.
 *
 * <h2>ISA hierarchies (database/00_relational_mapping.md §1)</h2>
 * <ul>
 *   <li><b>APP_USER → {STAFF_PROFILE, MEMBER}</b> — Strategy A, overlapping.
 *       Three separate entities. Each subtype has its OWN identity key
 *       (StaffID, MemberID) and reaches {@link com.lms.common.domain.AppUser}
 *       through a {@code @OneToOne} on the UNIQUE NOT NULL UserID column.
 *       No {@code @Inheritance}: JOINED inheritance needs the subtype's
 *       primary key to BE the supertype's key, which this schema deliberately
 *       does not do. One AppUser may have both a StaffProfile and a
 *       Member.</li>
 *   <li><b>MEMBER → ACADEMIC_STAFF</b> — Strategy C. Not a table and not a
 *       class: {@link com.lms.common.domain.MemberType#AcademicStaff} on
 *       {@link com.lms.common.domain.Member}.</li>
 * </ul>
 *
 * <h2>Aggregates — one repository per root</h2>
 * Child rows are saved through their root's collection (cascade), not
 * through a repository of their own:
 * <ul>
 *   <li>AppUser → UserRole</li>
 *   <li>Book → BookAuthor, BookCategory (join table), BookKeyword (element collection)</li>
 *   <li>Loan → LoanRenewal</li>
 *   <li>Fine → FineAppeal, FinePayment</li>
 *   <li>BookReview → ReviewFlag, ReviewModerationHistory</li>
 *   <li>MemberFeedback → FeedbackHistory</li>
 * </ul>
 * Every other entity is a root on its own. Queries that return aggregate
 * figures (average rating, revenue, most borrowed) are not derived queries;
 * they belong in {@code @Query} methods or the D4/D5 SQL.
 *
 * <h2>Defaults and timestamps</h2>
 * Where a column has a SQL DEFAULT, the entity field starts at the same
 * value, or a {@code @PrePersist} method fills in the timestamp. Hibernate
 * inserts every mapped column, so a Java null would be sent as NULL rather
 * than letting the SQL default apply. Timestamps are truncated to whole
 * seconds to match DATETIME2(0).
 *
 * <h2>Business rules live elsewhere</h2>
 * Entities hold state only. Rules such as borrowing limits and fine status
 * changes belong in services and the D5 procedures and triggers (CLAUDE.md
 * rule 5).
 */
package com.lms.common.domain;
