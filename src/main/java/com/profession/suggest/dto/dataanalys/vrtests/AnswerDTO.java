package com.profession.suggest.dto.dataanalys.vrtests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerDTO {
    private String questionText;
    private String answerText;
    private Double answerScore;
}
