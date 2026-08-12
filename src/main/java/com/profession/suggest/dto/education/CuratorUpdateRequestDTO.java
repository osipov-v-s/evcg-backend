package com.profession.suggest.dto.education;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuratorUpdateRequestDTO {
    @NotBlank
    private String name;
    @NotBlank
    private String surname;
    private String patronymic;
    @Email
    @NotBlank
    private String email;
    @NotNull
    private Long schoolId;
}
