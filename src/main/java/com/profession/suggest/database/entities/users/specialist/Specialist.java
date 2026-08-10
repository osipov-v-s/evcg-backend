package com.profession.suggest.database.entities.users.specialist;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.dataanalys.psychtests.PsychTest;
import com.profession.suggest.database.entities.gender.Gender;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "specialist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Specialist implements User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "surname")
    private String surname;
    @Column(name = "patronymic")
    private String patronymic;
    @Column(name = "contactEmail")
    private String contactEmail;
    @Column(name = "contactPhone")
    private String contactPhone;
    @Column(name = "experience")
    private String experience;
    @Column(name = "jobSatisfaction")
    private String jobSatisfaction;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profession_id")
    private Profession profession;
    @ManyToOne()
    @JoinColumn(name = "gender_id")
    private Gender gender;
    @OneToMany(mappedBy = "specialist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PsychTest> psychTests;
    @OneToMany(mappedBy = "nearestSpecialist")
    private List<Prediction> predictions;
    @ManyToOne()
    @JoinColumn(name = "company_id")
    private Company company;
    @OneToMany(mappedBy = "invitedBy")
    private List<Invitation> sentInvitations;
}
