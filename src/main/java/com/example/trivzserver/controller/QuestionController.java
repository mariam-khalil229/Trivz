package com.example.trivzserver.controller;

import com.example.trivzserver.dto.QuestionRequest;
import com.example.trivzserver.dto.QuestionResponse;
import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.QuestionAnswer;
import com.example.trivzserver.repository.QuestionRepository;
import com.example.trivzserver.service.QuestionAdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final QuestionAdminService adminService;

    public QuestionController(QuestionRepository questionRepository,
                              QuestionAdminService adminService) {
        this.questionRepository = questionRepository;
        this.adminService = adminService;
    }

    @GetMapping
    public List<QuestionResponse> getAll(@RequestParam(value = "difficulty", required = false) String difficulty) {
        List<Question> source = (difficulty == null || difficulty.isBlank())
                ? questionRepository.findAll()
                : questionRepository.findByDifficultyIgnoreCase(difficulty.trim());
        return source.stream().map(QuestionController::toResponse).toList();
    }

    @GetMapping("/{id}")
    public QuestionResponse getById(@PathVariable Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return toResponse(question);
    }

    @PostMapping
    public QuestionResponse create(@RequestBody QuestionRequest request) {
        return toResponse(adminService.create(request));
    }

    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable Long id, @RequestBody QuestionRequest request) {
        return toResponse(adminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminService.delete(id);
    }

    private static QuestionResponse toResponse(Question question) {
        List<String> answers = question.getAcceptedAnswers().stream()
                .map(QuestionAnswer::getAnswerText)
                .toList();
        return new QuestionResponse(
                question.getId(),
                question.getPrompt(),
                question.getDifficulty(),
                answers
        );
    }
}
