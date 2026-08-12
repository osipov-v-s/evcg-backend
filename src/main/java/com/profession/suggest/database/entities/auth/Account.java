package com.profession.suggest.database.entities.auth;

import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.education.Curator;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "account")
@Getter
@Setter
@ToString(exclude = {"pupil", "specialist", "curator", "roles"})
@EqualsAndHashCode(exclude = {"pupil", "specialist", "curator", "roles"})
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, length = 50, nullable = false)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;
    @Column(name = "created_at")
    private LocalDate createdAt;
    @Column(name = "first_login")
    private Boolean firstLogin;

    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
    private Pupil pupil;
    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
    private Specialist specialist;
    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
    private Curator curator;
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        if (firstLogin == null)
            firstLogin = true;
    }
}
