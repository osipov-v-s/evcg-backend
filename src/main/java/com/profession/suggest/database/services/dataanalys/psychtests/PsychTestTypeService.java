package com.profession.suggest.database.services.dataanalys.psychtests;

import com.profession.suggest.database.entities.dataanalys.psychtests.PsychTestType;
import com.profession.suggest.database.repositories.dataanalys.psychtests.PsychTestRepository;
import com.profession.suggest.database.repositories.dataanalys.psychtests.PsychTestTypeRepository;
import org.springframework.stereotype.Service;
import com.profession.suggest.dto.dataanalys.TestTypeStatusDTO;

import java.util.List;

@Service
public class PsychTestTypeService {
    private final PsychTestTypeRepository repository;

    public PsychTestTypeService(PsychTestTypeRepository repository) {
        this.repository = repository;
    }

    public PsychTestType getPsychTestTypeByName(String name) {
        PsychTestType type = repository.findByName(name);
        if (type == null || !Boolean.TRUE.equals(type.getIsActive()))
            throw new IllegalArgumentException("Psychological test type is inactive or missing: " + name);
        return type;
    }
    public List<TestTypeStatusDTO> getTypes() {
        return repository.findAll().stream()
                .map(type -> new TestTypeStatusDTO(type.getId(), type.getName(), type.getIsActive()))
                .toList();
    }
    public TestTypeStatusDTO setActive(Long id, boolean active) {
        PsychTestType type = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Psychological test type not found"));
        type.setIsActive(active);
        type = repository.save(type);
        return new TestTypeStatusDTO(type.getId(), type.getName(), type.getIsActive());
    }
}
