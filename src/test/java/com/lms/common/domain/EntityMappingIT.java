package com.lms.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;

/**
 * Checks the entities against the REAL database — what schema validation
 * alone cannot catch.
 *
 * <p>{@code ddl-auto=validate} confirms that tables and columns exist with
 * compatible types. It does not read any rows, so it cannot tell you that an
 * enum is missing a value the CHECK constraint allows, or that a derived
 * query produces SQL that SQL Server rejects. This test does both.
 *
 * <p><b>Needs SQL Server running with the schema and seed data applied.</b>
 * Named {@code *IT} so a plain {@code mvnw test} skips it and stays
 * database-free. Run it explicitly after any change to 01_schema.sql or to
 * an entity:
 * <pre>
 *   mvnw.cmd test -Dtest=EntityMappingIT -Dsurefire.failIfNoSpecifiedTests=false
 * </pre>
 * Read-only: the test transaction is rolled back.
 */
@SpringBootTest
@Transactional
class EntityMappingIT {

    @Autowired private EntityManager em;
    @Autowired private ApplicationContext context;
    @Autowired private AppUserRepository appUsers;
    @Autowired private StaffProfileRepository staffProfiles;
    @Autowired private MemberRepository members;
    @Autowired private BookRepository books;
    @Autowired private BookCopyRepository copies;

    /** Every row of every entity loads, so every enum and converter accepts every stored value. */
    @Test
    void everyRowOfEveryEntityLoads() {
        // 31, not 30: UC-01 added PasswordResetToken (database/01_schema.sql,
        // com.lms.common.domain.PasswordResetToken) for the email-token
        // password reset flow — not in the original EER, documented in
        // 01_schema.sql's header comment.
        assertThat(em.getMetamodel().getEntities()).hasSize(31);

        for (EntityType<?> entity : em.getMetamodel().getEntities()) {
            String table = entity.getJavaType().getAnnotation(Table.class).name();
            long jpaCount = em.createQuery("select e from " + entity.getName() + " e", entity.getJavaType())
                    .getResultList().size();
            long sqlCount = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM dbo.[" + table + "]")
                    .getSingleResult()).longValue();
            assertThat(jpaCount).as("rows loaded from %s", table).isEqualTo(sqlCount);
        }
    }

    /** The two tables mapped without their own entity class. */
    @Test
    void joinTableAndElementCollectionMatchTheDatabase() {
        long keywords = 0;
        long categoryLinks = 0;
        for (Book book : books.findAll()) {
            keywords += book.getKeywords().size();
            categoryLinks += book.getCategories().size();
        }
        assertThat(keywords).isEqualTo(sqlCount("BookKeyword"));
        assertThat(categoryLinks).isEqualTo(sqlCount("BookCategory"));
    }

    /** Strategy A, overlapping: one AppUser can hold both subtype rows through separate UserID links. */
    @Test
    void appUserSubtypesAreSeparateEntitiesAndMayOverlap() {
        long overlapping = appUsers.findAll().stream()
                .filter(u -> staffProfiles.existsByUserUserId(u.getUserId())
                        && members.existsByUserUserId(u.getUserId()))
                .count();
        long expected = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM dbo.StaffProfile s JOIN dbo.Member m ON m.UserID = s.UserID")
                .getSingleResult()).longValue();
        assertThat(overlapping).isEqualTo(expected);

        members.findAll().forEach(m ->
                assertThat(m.getMemberId()).as("MemberID is its own key, not the UserID")
                        .isNotNull());
    }

    /** Strategy C: academic staff are Members distinguished only by MemberType. */
    @Test
    void academicStaffIsAMemberTypeValue() {
        long expected = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM dbo.Member WHERE MemberType = N'Academic Staff'")
                .getSingleResult()).longValue();
        assertThat(members.findByMemberType(MemberType.AcademicStaff)).hasSize((int) expected);
    }

    /** Spot-checks against known seed data (02_seed_02_catalogue.sql). */
    @Test
    void seedDataReadsBackCorrectly() {
        Book atomicHabits = books.findByIsbn13("9780735211292").orElseThrow();
        assertThat(copies.countByBookBookIdAndStatus(atomicHabits.getBookId(), BookCopyStatus.OnLoan))
                .as("every copy of Atomic Habits is on loan").isEqualTo(4);
        assertThat(books.findDistinctByActiveTrueAndKeywords("sql"))
                .extracting(Book::getIsbn13).containsExactly("9780078022159");
        assertThat(appUsers.existsByRoleAssignmentsRoleRoleName("Library Administrator")).isTrue();
    }

    /**
     * Calls every derived query method on every repository once, with
     * placeholder arguments. Spring Data only parses method names at startup;
     * this proves each generated query actually executes on SQL Server.
     */
    @Test
    void everyDerivedQueryExecutes() throws Exception {
        List<String> called = new ArrayList<>();
        for (Map.Entry<String, JpaRepository> bean : context.getBeansOfType(JpaRepository.class).entrySet()) {
            Class<?> repositoryInterface = findDomainInterface(bean.getValue().getClass());
            for (Method method : repositoryInterface.getDeclaredMethods()) {
                if (method.isDefault() || method.isSynthetic()) {
                    continue;
                }
                Object[] args = placeholderArgs(method);
                method.invoke(bean.getValue(), args);
                called.add(repositoryInterface.getSimpleName() + "." + method.getName());
            }
        }
        assertThat(called).hasSizeGreaterThan(80);
    }

    // ------------------------------------------------------------------ helpers

    private long sqlCount(String table) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM dbo.[" + table + "]")
                .getSingleResult()).longValue();
    }

    private static Class<?> findDomainInterface(Class<?> proxyClass) {
        for (Class<?> candidate : proxyClass.getInterfaces()) {
            if (candidate.getPackageName().equals(EntityMappingIT.class.getPackageName())) {
                return candidate;
            }
        }
        throw new IllegalStateException("No domain repository interface on " + proxyClass);
    }

    private static Object[] placeholderArgs(Method method) {
        Type[] types = method.getGenericParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            args[i] = placeholder(types[i]);
        }
        return args;
    }

    private static Object placeholder(Type type) {
        if (type instanceof ParameterizedType parameterized
                && Collection.class.isAssignableFrom((Class<?>) parameterized.getRawType())) {
            return List.of(placeholder(parameterized.getActualTypeArguments()[0]));
        }
        Class<?> c = (Class<?>) type;
        if (c == Integer.class) return 1;
        if (c == Long.class) return 1L;
        // Added for BookRepository.search(..., boolean availableOnly, ...) — UC-02's combined
        // catalogue search/filter query (com.lms.catalogue.BookService).
        if (c == boolean.class || c == Boolean.class) return false;
        if (c == String.class) return "x";
        if (c == LocalDateTime.class) return LocalDateTime.now();
        if (c == LocalDate.class) return LocalDate.now();
        if (c == Pageable.class) return PageRequest.of(0, 5);
        if (c.isEnum()) return c.getEnumConstants()[0];
        throw new IllegalArgumentException("No placeholder for " + type);
    }
}
