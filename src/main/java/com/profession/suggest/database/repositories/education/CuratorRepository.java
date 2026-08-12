package com.profession.suggest.database.repositories.education;

import com.profession.suggest.database.entities.education.Curator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuratorRepository extends JpaRepository<Curator, Long> {
    Optional<Curator> findByAccountId(Long accountId);
    List<Curator> findBySchoolId(Long schoolId);
}
