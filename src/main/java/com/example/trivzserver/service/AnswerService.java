package com.example.trivzserver.service;

import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.QuestionAnswer;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {

    public boolean isCorrectAnswer(Question question, String playerInput) {
        if (question == null || playerInput == null) return false;

        String normalizedInput = normalize(playerInput);

        for (QuestionAnswer answer : question.getAcceptedAnswers()) {
            String normalizedAnswer = normalize(answer.getAnswerText());
            if (normalizedInput.equals(normalizedAnswer)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {
        return text
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9 ]", "");
    }
}