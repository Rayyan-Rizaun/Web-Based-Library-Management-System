package com.lms.common.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Composite primary key of {@link BookAuthor}: PK_BookAuthor (BookID, AuthorID). */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BookAuthorId implements Serializable {

    @Column(name = "BookID")
    private Integer bookId;

    @Column(name = "AuthorID")
    private Integer authorId;
}
