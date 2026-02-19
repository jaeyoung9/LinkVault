package org.link.linkvault.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuidelineStepRequestDto {

    @NotBlank
    private String screen;

    @NotBlank
    private String targetElement;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    @Min(0)
    private Integer displayOrder;
}
