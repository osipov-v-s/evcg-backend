package com.profession.suggest.dto.dataanalys.prediction;

import com.profession.suggest.dto.dataanalys.psychtests.PsychTestDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionRequest {
    private Long pupilId;
    private Map<String, PsychTestDTO> psychTests;
}
