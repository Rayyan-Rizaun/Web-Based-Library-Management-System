package com.lms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/** Reject a pending registration: a reason is required — the task's own words. */
@Getter
@Setter
public class RejectMemberForm {

    @NotBlank(message = "Enter a reason for rejecting this registration")
    @Size(max = 500, message = "Reason must be at most 500 characters")
    private String reason;
}
