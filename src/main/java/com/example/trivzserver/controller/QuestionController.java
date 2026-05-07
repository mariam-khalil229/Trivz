package com.example.trivzserver.controller;

import com.example.trivzserver.dto.QuestionRequest;
import com.example.trivzserver.dto.QuestionResponse;
import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.QuestionAnswer;
import com.example.trivzserver.repository.QuestionRepository;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionRepository questionRepository;

    public QuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @GetMapping
    public List<QuestionResponse> getAll() {
        return questionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public QuestionResponse getById(@PathVariable Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return toResponse(question);
    }

    @PostMapping
    @Transactional
    public QuestionResponse create(@RequestBody QuestionRequest request) {
        Question question = new Question();
        applyRequest(question, request);
        Question saved = questionRepository.save(question);
        return toResponse(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public QuestionResponse update(@PathVariable Long id, @RequestBody QuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        applyRequest(question, request);
        Question saved = questionRepository.save(question);
        return toResponse(saved);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        questionRepository.deleteById(id);
    }

    private void applyRequest(Question question, QuestionRequest request) {
        question.setPrompt(request.getPrompt());
        question.setCategory(request.getCategory());
        question.setDifficulty(request.getDifficulty());
        question.setTimeLimitSeconds(
                request.getTimeLimitSeconds() != null ? request.getTimeLimitSeconds() : 20
        );

        question.getAcceptedAnswers().clear();
        if (request.getAcceptedAnswers() != null) {
            for (String answerText : request.getAcceptedAnswers()) {
                QuestionAnswer answer = new QuestionAnswer();
                answer.setAnswerText(answerText);
                answer.setQuestion(question);
                question.getAcceptedAnswers().add(answer);
            }
        }
    }

    private QuestionResponse toResponse(Question question) {
        List<String> answers = question.getAcceptedAnswers().stream()
                .map(QuestionAnswer::getAnswerText)
                .toList();

        return new QuestionResponse(
                question.getId(),
                question.getPrompt(),
                question.getCategory(),
                question.getDifficulty(),
                question.getTimeLimitSeconds(),
                answers
        );
    }
}