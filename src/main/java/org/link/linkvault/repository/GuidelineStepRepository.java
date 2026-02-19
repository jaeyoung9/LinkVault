package org.link.linkvault.repository;

import org.link.linkvault.entity.GuidelineStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuidelineStepRepository extends JpaRepository<GuidelineStep, Long> {

    List<GuidelineStep> findByScreenOrderByDisplayOrderAsc(String screen);

    List<GuidelineStep> findAllByOrderByScreenAscDisplayOrderAsc();

    List<GuidelineStep> findByScreenAndEnabledTrueOrderByDisplayOrderAsc(String screen);

    void deleteByScreen(String screen);
}
