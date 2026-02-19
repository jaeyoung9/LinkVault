package org.link.linkvault.service;

import lombok.RequiredArgsConstructor;
import org.link.linkvault.dto.GuidelineStepOrderDto;
import org.link.linkvault.dto.GuidelineStepRequestDto;
import org.link.linkvault.dto.GuidelineStepResponseDto;
import org.link.linkvault.entity.GuidelineStep;
import org.link.linkvault.exception.ResourceNotFoundException;
import org.link.linkvault.repository.GuidelineStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuidelineStepService {

    private final GuidelineStepRepository guidelineStepRepository;
    private final AuditLogService auditLogService;

    public List<GuidelineStepResponseDto> findAll() {
        return guidelineStepRepository.findAllByOrderByScreenAscDisplayOrderAsc().stream()
                .map(GuidelineStepResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<GuidelineStepResponseDto> findByScreen(String screen) {
        return guidelineStepRepository.findByScreenOrderByDisplayOrderAsc(screen).stream()
                .map(GuidelineStepResponseDto::from)
                .collect(Collectors.toList());
    }

    public List<GuidelineStepResponseDto> findEnabledByScreen(String screen) {
        return guidelineStepRepository.findByScreenAndEnabledTrueOrderByDisplayOrderAsc(screen).stream()
                .map(GuidelineStepResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public GuidelineStepResponseDto create(GuidelineStepRequestDto dto, String actorUsername) {
        GuidelineStep step = GuidelineStep.builder()
                .screen(dto.getScreen())
                .targetElement(dto.getTargetElement())
                .title(dto.getTitle())
                .content(dto.getContent())
                .displayOrder(dto.getDisplayOrder())
                .enabled(true)
                .build();

        step = guidelineStepRepository.save(step);
        auditLogService.log(actorUsername, AuditActionCodes.GUIDELINE_STEP_CREATE,
                "GuidelineStep", step.getId(),
                AuditDetailFormatter.format("screen", dto.getScreen(), "target", dto.getTargetElement()));
        return GuidelineStepResponseDto.from(step);
    }

    @Transactional
    public GuidelineStepResponseDto update(Long id, GuidelineStepRequestDto dto, String actorUsername) {
        GuidelineStep step = guidelineStepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guideline step not found: " + id));

        step.update(dto.getTargetElement(), dto.getTitle(), dto.getContent());
        if (dto.getDisplayOrder() != null) {
            step.setDisplayOrder(dto.getDisplayOrder());
        }

        auditLogService.log(actorUsername, AuditActionCodes.GUIDELINE_STEP_UPDATE,
                "GuidelineStep", id,
                AuditDetailFormatter.format("screen", dto.getScreen(), "target", dto.getTargetElement()));
        return GuidelineStepResponseDto.from(step);
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        GuidelineStep step = guidelineStepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guideline step not found: " + id));
        guidelineStepRepository.delete(step);
        auditLogService.log(actorUsername, AuditActionCodes.GUIDELINE_STEP_DELETE,
                "GuidelineStep", id, null);
    }

    @Transactional
    public GuidelineStepResponseDto toggleEnabled(Long id, String actorUsername) {
        GuidelineStep step = guidelineStepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guideline step not found: " + id));
        step.setEnabled(!step.isEnabled());
        auditLogService.log(actorUsername, AuditActionCodes.GUIDELINE_STEP_TOGGLE,
                "GuidelineStep", id,
                AuditDetailFormatter.format("enabled", String.valueOf(step.isEnabled())));
        return GuidelineStepResponseDto.from(step);
    }

    @Transactional
    public void reorder(List<GuidelineStepOrderDto> orders, String actorUsername) {
        for (GuidelineStepOrderDto order : orders) {
            GuidelineStep step = guidelineStepRepository.findById(order.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Guideline step not found: " + order.getId()));
            step.setDisplayOrder(order.getDisplayOrder());
        }
        auditLogService.log(actorUsername, AuditActionCodes.GUIDELINE_STEP_REORDER,
                "GuidelineStep", null,
                AuditDetailFormatter.format("count", String.valueOf(orders.size())));
    }
}
