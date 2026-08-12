package com.profession.suggest.database.services.education;

import com.profession.suggest.database.entities.education.School;
import com.profession.suggest.database.repositories.education.SchoolRepository;
import com.profession.suggest.dto.education.SchoolDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolService {
    private final SchoolRepository repository;

    @Transactional(readOnly = true)
    public List<SchoolDTO> getAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public School getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("School not found: " + id));
    }

    @Transactional(readOnly = true)
    public School findByName(String name) {
        if (name == null || name.isBlank())
            return null;
        return repository.findByNameIgnoreCase(name.trim()).orElse(null);
    }

    @Transactional
    public SchoolDTO create(SchoolDTO dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank())
            throw new IllegalArgumentException("School name is required");
        if (repository.findByNameIgnoreCase(dto.getName().trim()).isPresent())
            throw new IllegalArgumentException("School already exists: " + dto.getName());

        School school = new School();
        apply(school, dto);
        return toDTO(repository.save(school));
    }

    @Transactional
    public SchoolDTO update(Long id, SchoolDTO dto) {
        School school = getById(id);
        apply(school, dto);
        return toDTO(repository.save(school));
    }

    public SchoolDTO toDTO(School school) {
        return SchoolDTO.builder()
                .id(school.getId())
                .name(school.getName())
                .address(school.getAddress())
                .email(school.getEmail())
                .phone(school.getPhone())
                .build();
    }

    private void apply(School school, SchoolDTO dto) {
        if (dto.getName() != null && !dto.getName().isBlank())
            school.setName(dto.getName().trim());
        school.setAddress(dto.getAddress());
        school.setEmail(dto.getEmail());
        school.setPhone(dto.getPhone());
    }
}
