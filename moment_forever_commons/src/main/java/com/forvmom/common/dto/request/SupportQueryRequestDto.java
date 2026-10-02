package com.forvmom.common.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

/**
 * Submission body for the public contact/support form.
 *
 * <p>
 * {@code name}, {@code email} and {@code phone} are required only for guest
 * submissions (no {@code X-User-Id} header present); for a logged-in user they
 * are auto-filled from the profile and any values supplied here are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class SupportQueryRequestDto {

    private String name;

    private String email;

    private String phone;

    private String subject;

    @NotBlank(message = "Message is required")
    private String message;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
