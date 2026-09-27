package com.profession.suggest.dto.dataanalys.vrtests;

import com.profession.suggest.database.entities.dataanalys.vrtests.VRTest;
import com.profession.suggest.database.entities.dataanalys.vrtests.VRTestAnswer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VRTestMapper {
    public VRTestDTO toDTO(VRTest test) {
        VRTestDTO dto = new VRTestDTO();
        dto.setId(test.getId());
        dto.setScore(test.getScore());
        dto.setCompletionTimeSeconds(test.getCompletionTimeSeconds());
        dto.setTypeName(test.getType().getName());
        dto.setProfessionId(test.getProfession().getId());
        if (test.getPupil() != null)
            dto.setPupilId(test.getPupil().getId());
        else if (test.getSpecialist() != null)
            dto.setSpecialistId(test.getSpecialist().getId());
        List<AnswerDTO> answersDTOs = new ArrayList<>();
        for(VRTestAnswer answer: test.getAnswers()) {
            AnswerDTO answerDTO = new AnswerDTO();
            answerDTO.setAnswerText(answer.getAnswerText());
            answerDTO.setQuestionText(answer.getQuestionText());
            answerDTO.setAnswerScore(answer.getAnswerScore());
            answersDTOs.add(answerDTO);
        }
        dto.setAnswers(answersDTOs);
        dto.setCreatedAt(test.getCreatedAt());
        return dto;
    }
}
