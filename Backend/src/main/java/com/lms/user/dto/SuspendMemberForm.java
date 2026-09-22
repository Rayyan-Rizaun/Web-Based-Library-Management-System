package com.lms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/** Suspend an Active member: a reason is required — the task's own words. */
@Getter
@Setter
public class SuspendMemberForm {

    @NotBlank(message = "Enter a reason for suspending this member")
    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;
}
