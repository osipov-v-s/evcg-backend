package com.profession.suggest.dto.education;

import com.profession.suggest.dto.auth.AccountRegisterRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuratorCreateRequestDTO {
    @Valid
    @NotNull
    private AccountRegisterRequestDTO account;

    @NotBlank
    private String name;

    @NotBlank
    private String surname;

    private String patronymic;

    @NotNull
    private Long schoolId;
}
