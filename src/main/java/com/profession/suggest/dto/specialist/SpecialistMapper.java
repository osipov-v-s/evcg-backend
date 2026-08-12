package com.profession.suggest.dto.specialist;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SpecialistMapper {
    private final PsychTestMapper psychTestMapper;

    public SpecialistMapper(PsychTestMapper psychTestMapper) {
        this.psychTestMapper = psychTestMapper;
    }

    public SpecialistDTO toDTO(Specialist specialist, Account account) {
        SpecialistDTO dto = new SpecialistDTO();
        dto.setId(specialist.getId());
        dto.setEmail(account.getEmail());
        dto.setName(specialist.getName());
        dto.setSurname(specialist.getSurname());
        dto.setPatronymic(specialist.getPatronymic());
        dto.setContactEmail(specialist.getContactEmail());
        dto.setContactPhone(specialist.getContactPhone());
        if (specialist.getProfession() != null)
            dto.setProfession(specialist.getProfession().getName());
        if (specialist.getGender() != null)
            dto.setGender(specialist.getGender().getName());
        if (specialist.getCompany() != null) {
            dto.setCompanyId(specialist.getCompany().getId());
            dto.setCompanyName(specialist.getCompany().getName());
        }
        dto.setJobSatisfaction(specialist.getJobSatisfaction());
        dto.setExperience(specialist.getExperience());
        return dto;
    }
    public Specialist fromDTO(SpecialistDTO dto) {
        Specialist specialist = new Specialist();
        specialist.setName(dto.getName());
        specialist.setSurname(dto.getSurname());
        specialist.setPatronymic(dto.getPatronymic());
        specialist.setContactEmail(dto.getContactEmail());
        specialist.setContactPhone(dto.getContactPhone());
        specialist.setExperience(dto.getExperience());
        specialist.setJobSatisfaction(dto.getJobSatisfaction());
        return specialist;
    }
    public SpecialistCompleteDTO toCompleteDTO(Specialist specialist) {
        SpecialistCompleteDTO dto = new SpecialistCompleteDTO();
        SpecialistDTO specialistDTO = toDTO(specialist, specialist.getAccount());

        dto.setSpecialist(specialistDTO);
        dto.setAccountId(specialist.getAccount().getId());
        dto.setRoles(specialist.getAccount().getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        dto.setPsychTests(specialist.getPsychTests().stream().map(psychTestMapper::toDTO).collect(Collectors.toList()));
        return dto;
    }
}
