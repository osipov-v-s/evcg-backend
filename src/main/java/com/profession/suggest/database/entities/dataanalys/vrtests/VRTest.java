package com.profession.suggest.database.entities.dataanalys.vrtests;

import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"type", "pupil", "specialist", "profession", "answers"})  // ? Exclude relationships
@EqualsAndHashCode(exclude = {"type", "pupil", "specialist", "profession", "answers"})  // ? Exclude relationships
@Table(name = "vr_test")
public class VRTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "score")
    private Double score;
    @Column(name = "completion_time_seconds")
    private Double completionTimeSeconds;
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vr_test_type_id")
    private VRTestType type;
    //who is complete test (pupil or specialist)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pupil_id")
    private Pupil pupil;
    //Test completed by specialist
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_id")
    private Specialist specialist;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profession_id")
    private Profession profession;
    @OneToMany(mappedBy = "vrTest")
    private Set<VRTestAnswer> answers = new HashSet<>();

}
