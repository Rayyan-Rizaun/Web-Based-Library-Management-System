package com.lms.common.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Table {@code BookAuthor} — the M:N relationship WRITES (R5). Part of the
 * {@link Book} aggregate. An entity, not a plain join table, because the
 * relationship has its own attribute: the author's position on the title.
 */
@Entity
@Table(name = "BookAuthor")
@Getter
@Setter
@NoArgsConstructor
public class BookAuthor {

    @EmbeddedId
    @Setter(AccessLevel.NONE)
    private BookAuthorId id = new BookAuthorId();

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BookID")
    private Book book;

    @MapsId("authorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AuthorID")
    private Author author;

    /** TINYINT — declared explicitly because Java has no unsigned byte type. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "AuthorOrder", nullable = false)
    private Integer authorOrder;

    public BookAuthor(Book book, Author author, int authorOrder) {
        this.book = book;
        this.author = author;
        this.authorOrder = authorOrder;
    }
}
