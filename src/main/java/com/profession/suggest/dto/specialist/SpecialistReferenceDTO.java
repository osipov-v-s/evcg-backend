package com.profession.suggest.dto.specialist;

import com.profession.suggest.dto.dataanalys.psychtests.PsychTestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialistReferenceDTO {
    private Long specialistId;
    private String profession;
    private Map<String, PsychTestDTO> psychTests;
}
