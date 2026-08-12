package com.profession.suggest.dto.pupil;

import com.profession.suggest.database.entities.gender.GenderEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PupilResponseDTO {
    public PupilDTO pupilDTO;
    public String email;
    public PupilResponseDTO(Long id, String name, String surname, String patronymic,
                            LocalDate birthday, String school, String healthCondition,
                            String nationality, String extraActivities, Integer classNumber, String classLabel, GenderEnum gender, String email, LocalDate createdAt) {
        this.pupilDTO = new PupilDTO();
        this.pupilDTO.setId(id);
        this.pupilDTO.setName(name);
        this.pupilDTO.setSurname(surname);
        this.pupilDTO.setPatronymic(patronymic);
        this.pupilDTO.setBirthday(birthday);
        this.pupilDTO.setSchool(school);
        this.pupilDTO.setHealthCondition(healthCondition);
        this.pupilDTO.setNationality(nationality);
        this.pupilDTO.setExtraActivities(extraActivities);
        this.pupilDTO.setClassNumber(classNumber);
        this.pupilDTO.setClassLabel(classLabel);
        this.pupilDTO.setGender(gender);
        this.pupilDTO.setCreatedAt(createdAt);
        this.email = email;
    }
}
