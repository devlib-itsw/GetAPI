package com.getapi.ai.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiCheckResponse {

    @JsonProperty("is_censored")
    private boolean censored;

    @JsonProperty("detected_labels")
    private List<String> detectedLabels;

    private Map<String, Double> scores;

    @JsonProperty("chunk_count")
    private int chunkCount;
}
