package com.profession.suggest.dto.pupil;

import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PupilMapper {
    private final PsychTestMapper psychTestMapper;

    public PupilMapper(PsychTestMapper psychTestMapper) {
        this.psychTestMapper = psychTestMapper;
    }

    public Pupil fromDTO(PupilDTO dto) {
        Pupil pupil = new Pupil();
        pupil.setName(dto.getName());
        pupil.setSurname(dto.getSurname());
        pupil.setPatronymic(dto.getPatronymic());
        pupil.setBirthday(dto.getBirthday());
        pupil.setSchool(dto.getSchool());
        pupil.setHealthCondition(dto.getHealthCondition());
        pupil.setNationality(dto.getNationality());
        pupil.setExtraActivities(dto.getExtraActivities());
        pupil.setClassNumber(dto.getClassNumber());
        pupil.setClassLabel(dto.getClassLabel());
        return pupil;
    }
    public PupilDTO toDTO(Pupil pupil) {
        PupilDTO dto = new PupilDTO();
        dto.setId(pupil.getId());
        dto.setName(pupil.getName());
        dto.setSurname(pupil.getSurname());
        dto.setPatronymic(pupil.getPatronymic());
        dto.setBirthday(pupil.getBirthday());
        if (pupil.getEducationalOrganization() != null) {
            dto.setSchoolId(pupil.getEducationalOrganization().getId());
            dto.setSchool(pupil.getEducationalOrganization().getName());
        } else {
            dto.setSchool(pupil.getSchool());
        }
        dto.setHealthCondition(pupil.getHealthCondition());
        dto.setNationality(pupil.getNationality());
        dto.setExtraActivities(pupil.getExtraActivities());
        dto.setClassNumber(pupil.getClassNumber());
        dto.setClassLabel(pupil.getClassLabel());
        if (pupil.getGender() != null)
            dto.setGender(pupil.getGender().getName());
        dto.setCreatedAt(pupil.getCreatedAt());
        return dto;
    }
    public Pupil updateFromDTO(Pupil pupil, PupilDTO dto) {
        Optional.ofNullable(dto.getName()).ifPresent(pupil::setName);
        Optional.ofNullable(dto.getSurname()).ifPresent(pupil::setSurname);
        Optional.ofNullable(dto.getPatronymic()).ifPresent(pupil::setPatronymic);
        Optional.ofNullable(dto.getBirthday()).ifPresent(pupil::setBirthday);
        Optional.ofNullable(dto.getSchool()).ifPresent(pupil::setSchool);
        Optional.ofNullable(dto.getHealthCondition()).ifPresent(pupil::setHealthCondition);
        Optional.ofNullable(dto.getNationality()).ifPresent(pupil::setNationality);
        Optional.ofNullable(dto.getExtraActivities()).ifPresent(pupil::setExtraActivities);
        Optional.ofNullable(dto.getClassNumber()).ifPresent(pupil::setClassNumber);
        Optional.ofNullable(dto.getClassLabel()).ifPresent(pupil::setClassLabel);
        return pupil;
    }
    public PupilResponseDTO toResponseDTO(Pupil pupil) {
        PupilResponseDTO response = new PupilResponseDTO();
        response.setPupilDTO(toDTO(pupil));
        response.setEmail(pupil.getAccount() != null ? pupil.getAccount().getEmail() : null);
        return response;
    }
    public PupilCompleteDTO toCompleteDTO(Pupil pupil) {
        PupilCompleteDTO dto = new PupilCompleteDTO();
        PupilDTO pupilDTO = toDTO(pupil);
        dto.setPupil(pupilDTO);
        dto.setAccountId(pupil.getAccount().getId());
        dto.setRoles(pupil.getAccount().getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        dto.setPsychTests(pupil.getPsychTests().stream().map(psychTestMapper::toDTO).collect(Collectors.toList()));
        return dto;
    }
}
