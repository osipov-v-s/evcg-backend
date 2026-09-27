package com.profession.suggest.database.entities.dataanalys.vrtests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "vr_test_answer")
public class VRTestAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "question_text", length = 500)
    private String questionText;
    @Column(name = "answer_text")
    private String answerText;
    @Column(name = "answer_score")
    private Double answerScore;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vr_test_id", nullable = false)
    @JsonIgnore
    private VRTest vrTest;
}
