package com.lms.common.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code Book} — entity BOOK (a title; physical copies are
 * {@link BookCopy}). Aggregate root for three child tables:
 * <ul>
 *   <li>{@code BookAuthor} — M:N WRITES, which has its own attribute
 *       (AuthorOrder), so it is an entity: {@link #authors}.</li>
 *   <li>{@code BookCategory} — M:N CLASSIFIES, with no attributes, so it is
 *       a plain {@code @JoinTable}: {@link #categories}.</li>
 *   <li>{@code BookKeyword} — the multivalued attribute Keywords (R1), so it
 *       is an {@code @ElementCollection} of strings: {@link #keywords}.</li>
 * </ul>
 * No AverageRating or AvailableCopies field: both are computed (R23).
 */
@Entity
@Table(name = "Book")
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookID")
    @Setter(AccessLevel.NONE)
    private Integer bookId;

    @Column(name = "ISBN13", nullable = false, length = 13)
    private String isbn13;

    @Column(name = "ISBN10", length = 10)
    private String isbn10;

    @Column(name = "Title", nullable = false, length = 255)
    private String title;

    @Column(name = "Subtitle", length = 255)
    private String subtitle;

    /** 1:N PUBLISHED BY (R9). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PublisherID")
    private Publisher publisher;

    @Column(name = "PublicationYear")
    private Short publicationYear;

    @Column(name = "Edition", length = 50)
    private String edition;

    @Column(name = "LanguageCode", nullable = false, length = 3)
    private String languageCode = "en";

    /** NVARCHAR(MAX). */
    @Column(name = "Description")
    private String description;

    @Column(name = "IsActive", nullable = false)
    private boolean active = true;

    /** Table BookAuthor, in author order. Removing an entry deletes that row. */
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("authorOrder ASC")
    @Setter(AccessLevel.NONE)
    private List<BookAuthor> authors = new ArrayList<>();

    /** Table BookCategory. */
    @ManyToMany
    @JoinTable(name = "BookCategory",
            joinColumns = @JoinColumn(name = "BookID"),
            inverseJoinColumns = @JoinColumn(name = "CategoryID"))
    @Setter(AccessLevel.NONE)
    private Set<Category> categories = new HashSet<>();

    /** Table BookKeyword. A Set, because the table's primary key forbids duplicates. */
    @ElementCollection
    @CollectionTable(name = "BookKeyword", joinColumns = @JoinColumn(name = "BookID"))
    @Column(name = "Keyword", nullable = false, length = 50)
    @Setter(AccessLevel.NONE)
    private Set<String> keywords = new HashSet<>();
}
