package com.profession.suggest.database.entities.education;

import com.profession.suggest.database.entities.users.pupil.Pupil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "school")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @OneToMany(mappedBy = "educationalOrganization")
    private List<Pupil> pupils = new ArrayList<>();

    @OneToMany(mappedBy = "school")
    private List<Curator> curators = new ArrayList<>();
}
