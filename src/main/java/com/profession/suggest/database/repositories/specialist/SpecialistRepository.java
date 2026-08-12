package com.profession.suggest.database.repositories.specialist;

import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.dto.specialist.SpecialistDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SpecialistRepository extends JpaRepository<Specialist, Long>, JpaSpecificationExecutor<Specialist> {
    @Query("SELECT new com.profession.suggest.dto.specialist.SpecialistDTO( " +
            "s.id, a.email, s.name, s.surname, s.patronymic, s.contactEmail, " +
            "s.contactPhone, s.experience, s.jobSatisfaction, p.name, g.name, c.id, c.name) " +
            "FROM Specialist s " +
            "LEFT JOIN Account a ON s.account.id = a.id " +
            "LEFT JOIN Gender g ON s.gender.id = g.id " +
            "LEFT JOIN s.profession p " +
            "LEFT JOIN s.company c")
    Page<SpecialistDTO> findSpecialists(Pageable pageable);

    @Query("SELECT DISTINCT s FROM Specialist s " +
            "LEFT JOIN FETCH s.account a " +
            "LEFT JOIN FETCH a.roles " +
            "LEFT JOIN FETCH s.gender " +
            "LEFT JOIN FETCH s.profession " +
            "WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<Specialist> findByAccountCreatedAtBetween(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT s FROM Specialist s " +
            "LEFT JOIN FETCH s.account a " +
            "LEFT JOIN FETCH a.roles " +
            "LEFT JOIN FETCH s.profession " +
            "LEFT JOIN FETCH s.psychTests pt " +
            "LEFT JOIN FETCH pt.psychTestType " +
            "LEFT JOIN FETCH pt.psychParams")
    List<Specialist> findAllForPredictionReference();
}
