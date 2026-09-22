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

/** Table {@code Publisher} — entity PUBLISHER. */
@Entity
@Table(name = "Publisher")
@Getter
@Setter
@NoArgsConstructor
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PublisherID")
    @Setter(AccessLevel.NONE)
    private Integer publisherId;

    @Column(name = "PublisherName", nullable = false, length = 150)
    private String publisherName;

    @Column(name = "Website", length = 255)
    private String website;

    @Column(name = "IsActive", nullable = false)
    private boolean active = true;
}
