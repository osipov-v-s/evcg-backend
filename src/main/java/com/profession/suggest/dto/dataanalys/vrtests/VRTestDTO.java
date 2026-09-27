package com.profession.suggest.dto.dataanalys.vrtests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VRTestDTO {
    private Long id;
    private Double score;
    private Long professionId;
    private Long pupilId;
    private String typeName;
    private Long specialistId;
    private Double completionTimeSeconds;
    private List<AnswerDTO> answers;
    private LocalDateTime createdAt;
}
