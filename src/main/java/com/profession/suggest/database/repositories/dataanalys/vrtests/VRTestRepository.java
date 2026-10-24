package com.profession.suggest.database.repositories.dataanalys.vrtests;

import com.profession.suggest.database.entities.dataanalys.vrtests.VRTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VRTestRepository extends JpaRepository<VRTest, Long> {
    List<VRTest> findByPupilId(Long pupilId);

    List<VRTest> findBySpecialistId(Long specialistId);

    List<VRTest> findByTypeId(Long typeId);

    List<VRTest> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT v FROM VRTest v WHERE v.pupil.id = :pupilId AND v.type.id = :typeId")
    List<VRTest> findByPupilIdAndTypeId(@Param("pupilId") Long pupilId, @Param("typeId") Long typeId);

    @Query("SELECT COUNT(v) FROM VRTest v WHERE v.pupil.id = :pupilId AND v.profession.id = :professionId")
    long countByPupilIdAndProfessionId(@Param("pupilId") Long pupilId,
                                       @Param("professionId") Long professionId);
    @Query("SELECT COUNT(v) FROM VRTest v WHERE v.specialist.id = :specialistId AND v.profession.id = :professionId")
    long countBySpecialistIdAndProfessionId(@Param("specialistId") Long pupilId,
                                       @Param("professionId") Long professionId);
    @Query("SELECT v FROM VRTest v WHERE v.pupil.id = :pupilId AND v.profession.id = :professionId")
    List<VRTest> findByPupilIdAndProfessionId(@Param("pupilId") Long pupilId,
                                              @Param("professionId") Long professionId);
    @Query("SELECT v FROM VRTest v WHERE v.specialist.id = :specialistId AND v.profession.id = :professionId")
    List<VRTest> findBySpecialistIdAndProfessionId(@Param("specialistId") Long specialistId,
                                                   @Param("professionId") Long professionId);
    // Get tests by account ID (works for both pupil and specialist)
    @Query("SELECT v FROM VRTest v WHERE v.pupil.account.id = :accountId")
    List<VRTest> findByPupilAccountId(@Param("accountId") Long accountId);

    // ? Query for specialist tests
    @Query("SELECT v FROM VRTest v WHERE v.specialist.account.id = :accountId")
    List<VRTest> findBySpecialistAccountId(@Param("accountId") Long accountId);
    @Query("select distinct vrt from VRTest vrt " +
            "inner join vrt.pupil p " +
            "where vrt.createdAt >= :startDate and vrt.createdAt <= :endDate")
    List<VRTest> findByPupilAndDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    @Query("select distinct vrt from VRTest vrt " +
            "inner join vrt.specialist s " +
            "where vrt.createdAt >= :startDate and vrt.createdAt <= :endDate")
    List<VRTest> findBySpecialistAndDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    @Query("select distinct vrt from VRTest vrt " +
            "where vrt.createdAt >= :startDate and vrt.createdAt <= :endDate")
    List<VRTest> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
}
