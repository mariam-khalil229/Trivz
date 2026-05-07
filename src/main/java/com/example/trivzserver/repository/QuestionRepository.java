package com.example.trivzserver.repository;

import com.example.trivzserver.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}