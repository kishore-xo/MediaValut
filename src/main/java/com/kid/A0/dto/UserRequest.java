package com.kid.A0.dto;


import com.kid.A0.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRequest {

    @Size(min = 3, max = 20, message = "Name must between 3 to 20 characters")
    String username;

    @Size(min = 8, message = "password must be at least 8 character")
    String password;

    @Email(message = "Invalid email format")
    String email;

    Role role;
}
