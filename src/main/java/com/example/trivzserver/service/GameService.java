package com.example.trivzserver.service;

import com.example.trivzserver.dto.GameQuestionResponse;
import com.example.trivzserver.entity.Question;
import com.example.trivzserver.entity.Room;
import com.example.trivzserver.entity.RoomGame;
import com.example.trivzserver.entity.RoomGameQuestion;
import com.example.trivzserver.repository.QuestionRepository;
import com.example.trivzserver.repository.RoomGameQuestionRepository;
import com.example.trivzserver.repository.RoomGameRepository;
import com.example.trivzserver.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class GameService {

    private final RoomRepository roomRepository;
    private final QuestionRepository questionRepository;
    private final RoomGameRepository roomGameRepository;
    private final RoomGameQuestionRepository roomGameQuestionRepository;

    public GameService(RoomRepository roomRepository,
                       QuestionRepository questionRepository,
                       RoomGameRepository roomGameRepository,
                       RoomGameQuestionRepository roomGameQuestionRepository) {
        this.roomRepository = roomRepository;
        this.questionRepository = questionRepository;
        this.roomGameRepository = roomGameRepository;
        this.roomGameQuestionRepository = roomGameQuestionRepository;
    }

    public GameQuestionResponse startGame(Long roomId, Integer questionCount) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!"LOBBY".equalsIgnoreCase(room.getStatus())) {
            throw new RuntimeException("Game already started or room not in lobby");
        }

        int count = (questionCount == null || questionCount <= 0) ? 10 : questionCount;

        List<Question> all = questionRepository.findAll();
        if (all.isEmpty()) {
            throw new RuntimeException("No questions available");
        }

        Collections.shuffle(all);
        if (count > all.size()) count = all.size();
        List<Question> selected = all.subList(0, count);

        RoomGame game = roomGameRepository.findByRoomId(roomId).orElse(null);
        if (game == null) {
            game = new RoomGame();
            game.setRoom(room);
        }

        game.setCurrentQuestionIndex(0);
        game.setStartedAt(LocalDateTime.now());
        game.setQuestionStartedAt(LocalDateTime.now());
        game = roomGameRepository.save(game);

        // reset old question order if restarting (optional)
        roomGameQuestionRepository.deleteAllByRoomGameId(game.getId());

        for (int i = 0; i < selected.size(); i++) {
            RoomGameQuestion rgq = new RoomGameQuestion();
            rgq.setRoomGame(game);
            rgq.setQuestion(selected.get(i));
            rgq.setPosition(i);
            roomGameQuestionRepository.save(rgq);
        }

        room.setStatus("IN_PROGRESS");
        roomRepository.save(room);

        return toResponse(room, game, selected.get(0), 0, selected.size());
    }

    public GameQuestionResponse getCurrentQuestion(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomGame game = roomGameRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Game not started"));

        RoomGameQuestion rgq = roomGameQuestionRepository
                .findByRoomGameIdAndPosition(game.getId(), game.getCurrentQuestionIndex())
                .orElseThrow(() -> new RuntimeException("Current question not found"));

        int total = roomGameQuestionRepository.findAllByRoomGameIdOrderByPositionAsc(game.getId()).size();

        return toResponse(room, game, rgq.getQuestion(), game.getCurrentQuestionIndex(), total);
    }

    public GameQuestionResponse nextQuestion(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomGame game = roomGameRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Game not started"));

        List<RoomGameQuestion> all = roomGameQuestionRepository.findAllByRoomGameIdOrderByPositionAsc(game.getId());
        int total = all.size();

        int nextIndex = game.getCurrentQuestionIndex() + 1;
        if (nextIndex >= total) {
            room.setStatus("FINISHED");
            roomRepository.save(room);
            throw new RuntimeException("Game finished");
        }

        game.setCurrentQuestionIndex(nextIndex);
        game.setQuestionStartedAt(LocalDateTime.now());
        game = roomGameRepository.save(game);

        Question q = all.get(nextIndex).getQuestion();
        return toResponse(room, game, q, nextIndex, total);
    }

    private GameQuestionResponse toResponse(Room room, RoomGame game, Question q, int index, int total) {
        return new GameQuestionResponse(
                room.getId(),
                q.getId(),
                q.getPrompt(),
                q.getCategory(),
                q.getDifficulty(),
                q.getTimeLimitSeconds(),
                index,
                total,
                game.getQuestionStartedAt()
        );
    }
}