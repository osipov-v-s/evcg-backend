package com.profession.suggest.database.services.dataanalys.vrtests;

import com.profession.suggest.database.entities.dataanalys.vrtests.VRTestType;
import com.profession.suggest.database.repositories.dataanalys.vrtests.VRTestTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import com.profession.suggest.dto.dataanalys.TestTypeStatusDTO;

import java.util.List;

@Service
@AllArgsConstructor
public class VRTestTypeService {
    private final VRTestTypeRepository repository;

    public VRTestType getByName(String name) {
        VRTestType type = repository.findByName(name)
                .orElseThrow(()-> new IllegalArgumentException("No such type"));
        if (!Boolean.TRUE.equals(type.getIsActive()))
            throw new IllegalArgumentException("VR test type is inactive: " + name);
        return type;
    }
    public List<TestTypeStatusDTO> getTypes() {
        return repository.findAll().stream()
                .map(type -> new TestTypeStatusDTO(type.getId(), type.getName(), type.getIsActive()))
                .toList();
    }
    public TestTypeStatusDTO setActive(Long id, boolean active) {
        VRTestType type = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("VR test type not found"));
        type.setIsActive(active);
        type = repository.save(type);
        return new TestTypeStatusDTO(type.getId(), type.getName(), type.getIsActive());
    }
}
