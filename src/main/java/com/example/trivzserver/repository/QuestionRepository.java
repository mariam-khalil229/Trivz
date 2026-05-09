package com.example.trivzserver.repository;

import com.example.trivzserver.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByDifficultyIgnoreCase(String difficulty);

    boolean existsByPromptIgnoreCase(String prompt);

    Optional<Question> findByPromptIgnoreCase(String prompt);
}
