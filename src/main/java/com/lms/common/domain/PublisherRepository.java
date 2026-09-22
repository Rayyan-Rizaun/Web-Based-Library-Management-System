package com.lms.common.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Aggregate root {@link Publisher}. UC-02 catalogue data entry.
 */
public interface PublisherRepository extends JpaRepository<Publisher, Integer> {

    Optional<Publisher> findByPublisherNameIgnoreCase(String publisherName);

    List<Publisher> findByActiveTrueOrderByPublisherName();
}
