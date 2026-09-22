package com.lms.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Table {@code Author} — entity AUTHOR. */
@Entity
@Table(name = "Author")
@Getter
@Setter
@NoArgsConstructor
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuthorID")
    @Setter(AccessLevel.NONE)
    private Integer authorId;

    @Column(name = "AuthorName", nullable = false, length = 150)
    private String authorName;

    /** NVARCHAR(MAX). */
    @Column(name = "Biography")
    private String biography;

    @Column(name = "IsActive", nullable = false)
    private boolean active = true;
}
