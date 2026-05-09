package com.example.trivzserver.service;

import com.example.trivzserver.dto.QuestionRequest;
import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.QuestionAnswer;
import com.example.trivzserver.repository.QuestionRepository;
import com.example.trivzserver.repository.RoomGameQuestionRepository;
import com.example.trivzserver.repository.ScoreRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Question CRUD with the rules that aren't pure plumbing: duplicate-prompt rejection
 * and cascade-delete that's safe even when a question has Score / RoomGameQuestion
 * rows pointing at it. Pulls these decisions out of QuestionController so the controller
 * is just a thin HTTP shell.
 */
@Service
public class QuestionAdminService {

    private final QuestionRepository questionRepository;
    private final ScoreRepository scoreRepository;
    private final RoomGameQuestionRepository roomGameQuestionRepository;

    public QuestionAdminService(QuestionRepository questionRepository,
                                ScoreRepository scoreRepository,
                                RoomGameQuestionRepository roomGameQuestionRepository) {
        this.questionRepository = questionRepository;
        this.scoreRepository = scoreRepository;
        this.roomGameQuestionRepository = roomGameQuestionRepository;
    }

    @Transactional
    public Question create(QuestionRequest request) {
        validatePrompt(request.getPrompt());
        if (questionRepository.existsByPromptIgnoreCase(request.getPrompt().trim())) {
            throw new RuntimeException("A question with this prompt already exists");
        }
        Question question = new Question();
        applyRequest(question, request);
        return questionRepository.save(question);
    }

    @Transactional
    public Question update(Long id, QuestionRequest request) {
        validatePrompt(request.getPrompt());
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Keeping your own prompt is fine; reject only if a *different* question owns it.
        Optional<Question> existing = questionRepository.findByPromptIgnoreCase(request.getPrompt().trim());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new RuntimeException("Another question with this prompt already exists");
        }

        applyRequest(question, request);
        return questionRepository.save(question);
    }

    @Transactional
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new RuntimeException("Question not found");
        }
        // Refuse only if a live round is showing this question — otherwise the
        // GameTimerService would crash on its next tick. FINISHED rounds are fair game.
        if (roomGameQuestionRepository.countByQuestionAndRoomStatus(id, "IN_PROGRESS") > 0) {
            throw new RuntimeException(
                    "Cannot delete — this question is currently being used in an active game. " +
                    "Wait for the round to finish and try again.");
        }
        scoreRepository.deleteByQuestionId(id);
        roomGameQuestionRepository.deleteByQuestionId(id);
        questionRepository.deleteById(id);
        questionRepository.flush();
    }

    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new RuntimeException("Prompt is required");
        }
    }

    private void applyRequest(Question question, QuestionRequest request) {
        question.setPrompt(request.getPrompt().trim());
        question.setDifficulty(request.getDifficulty());

        question.getAcceptedAnswers().clear();
        if (request.getAcceptedAnswers() != null) {
            for (String raw : request.getAcceptedAnswers()) {
                if (raw == null) continue;
                String text = raw.trim();
                if (text.isEmpty()) continue;
                QuestionAnswer answer = new QuestionAnswer();
                answer.setAnswerText(text);
                answer.setQuestion(question);
                question.getAcceptedAnswers().add(answer);
            }
        }
    }
}
