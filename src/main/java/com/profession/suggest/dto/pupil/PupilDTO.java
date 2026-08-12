package com.profession.suggest.dto.pupil;

import com.profession.suggest.database.entities.gender.GenderEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PupilDTO {
    private Long id;

    private String name;

    private String surname;

    private String patronymic;

    private LocalDate birthday;

    private String school;

    private Long schoolId;

    private String healthCondition;

    private String nationality;

    private String extraActivities;
    private Integer classNumber;
    private String classLabel;
    private GenderEnum gender;
    private LocalDate createdAt;
}
