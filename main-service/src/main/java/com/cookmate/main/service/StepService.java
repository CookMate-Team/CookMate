package com.cookmate.main.service;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.exception.StepNotFoundException;
import com.cookmate.main.mapper.StepMapper;
import com.cookmate.main.model.Step;
import com.cookmate.main.repository.StepRepository;
import org.springframework.stereotype.Service;

@Service
public class StepService {

    private final StepRepository stepRepository;
    private final StepMapper stepMapper;

    public StepService(StepRepository stepRepository, StepMapper stepMapper) {
        this.stepRepository = stepRepository;
        this.stepMapper = stepMapper;
    }

    public StepDTO getStep(Long stepId) {
        Step step = stepRepository.findById(stepId)
            .orElseThrow(() -> new StepNotFoundException(stepId));
        return stepMapper.toDTO(step);
    }
}
