package org.link.linkvault.repository;

import org.link.linkvault.entity.TransparencyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransparencyReportRepository extends JpaRepository<TransparencyReport, Long> {

    @Query("SELECT r FROM TransparencyReport r WHERE r.published = true ORDER BY r.publishedAt DESC")
    List<TransparencyReport> findByPublishedTrue();

    @Query("SELECT r FROM TransparencyReport r ORDER BY r.createdAt DESC")
    Page<TransparencyReport> findAllOrderByCreatedAtDesc(Pageable pageable);
}
