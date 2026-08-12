package com.profession.suggest.dto.education;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuratorDTO {
    private Long id;
    private Long accountId;
    private String email;
    private String name;
    private String surname;
    private String patronymic;
    private SchoolDTO school;
}
