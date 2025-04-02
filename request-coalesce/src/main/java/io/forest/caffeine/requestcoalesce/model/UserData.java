package io.forest.caffeine.requestcoalesce.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserData {
    @NotBlank(message = "ID is required")
    private String id;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Role is required")
    private String role;
    
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;
    
    @NotBlank(message = "Address is required")
    private String address;
} 