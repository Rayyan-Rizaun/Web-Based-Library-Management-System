package com.lms.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemSettingForm {

    @NotBlank(message = "Enter a value for this setting")
    @Size(max = 255, message = "Value must be at most 255 characters")
    private String value;
}
