package com.profession.suggest.dto.dataanalys.psychtests;

import com.profession.suggest.database.entities.auth.role.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class AccountTestsDTO {
    private Long accountId;
    private String email;
    private String fullName;
    private String name;
    private String surname;
    private String patronymic;
    private String school;
    private Integer classNumber;
    private String classLabel;
    private String gender;
    private LocalDate birthday;
    private String profession;
    private String company;
    private Set<RoleEnum> roles;
    private List<PsychTestDTO> psychTests;
    public AccountTestsDTO(Long accountId, String email, String fullName,
                           String name, String surname, String patronymic,
                           Long testId, Double completionTimeSeconds,
                           String testTypeName, LocalDateTime createdAt) {
        this.accountId = accountId;
        this.email = email;
        this.fullName = fullName;
        this.name = name;
        this.surname = surname;
        this.patronymic = patronymic;
        // Initialize psychTests as empty list, you'll need to group them later
        this.psychTests = new ArrayList<>();
    }
}
