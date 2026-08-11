package com.profession.suggest.database.entities.users.pupil;

import com.profession.suggest.database.entities.dataanalys.simulation.Simulation;
import com.profession.suggest.database.entities.gender.Gender;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.psychtests.PsychTest;
import com.profession.suggest.database.entities.users.User;
import com.profession.suggest.database.entities.users.pupil.subject.PupilGrade;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "pupil")
@Getter
@Setter
@ToString(exclude = {"account", "gender"})
@EqualsAndHashCode(exclude = {"account", "gender"})
@NoArgsConstructor
@AllArgsConstructor
public class Pupil implements User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "surname", nullable = false, length = 50)
    private String surname;

    @Column(name = "patronymic", length = 50, nullable = false)
    private String patronymic;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "school", length = 200)
    private String school;

    @Column(name = "health_condition", columnDefinition = "TEXT")
    private String healthCondition;
    @Column(name = "nationality", length = 50)
    private String nationality;
    @Column(name = "extra_activities", columnDefinition = "TEXT")
    private String extraActivities;
    @Column(name = "class_number")
    private Integer classNumber;
    @Column(name = "class_label")
    private String classLabel;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gender_id")
    private Gender gender;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true)
    private Account account;
    //OneToMany List PsychTests
    @OneToMany(mappedBy = "pupil", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PsychTest> psychTests;
    @OneToMany(mappedBy = "pupil")
    private List<Simulation> simulations;
    @OneToMany(mappedBy = "pupil", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PupilGrade> pupilGrades;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }
}
