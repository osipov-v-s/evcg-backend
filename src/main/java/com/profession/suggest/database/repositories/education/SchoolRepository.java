package com.profession.suggest.database.repositories.education;

import com.profession.suggest.database.entities.education.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByNameIgnoreCase(String name);
}
