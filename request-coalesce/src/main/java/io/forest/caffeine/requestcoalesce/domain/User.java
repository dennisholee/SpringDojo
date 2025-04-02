package io.forest.caffeine.requestcoalesce.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    String id;
    String name;
    String email;
    String role;
    LocalDate dateOfBirth;
    String address;

    public boolean isAdult() {
        return dateOfBirth != null && 
               LocalDate.now().minusYears(18).isAfter(dateOfBirth);
    }

    public boolean hasValidEmail() {
        return email != null && 
               email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
} 