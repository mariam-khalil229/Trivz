package com.example.trivzclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class

LiveClient {

    private final WebSocketStompClient client;
    private volatile StompSession session;

    public LiveClient() {
        WebSocketClient ws = new StandardWebSocketClient();
        WebSocketStompClient c = new WebSocketStompClient(ws);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter conv = new MappingJackson2MessageConverter();
        conv.setObjectMapper(mapper);
        c.setMessageConverter(conv);

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("trivz-stomp-");
        scheduler.initialize();
        c.setTaskScheduler(scheduler);

        this.client = c;
    }

    public void connect() {
        try {
            session = client.connectAsync(ClientSession.wsUrl, new StompSessionHandlerAdapter() {})
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("WebSocket connect failed: " + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    public <T> StompSession.Subscription subscribe(String topic, Class<T> type, Consumer<T> handler) {
        if (session == null) throw new IllegalStateException("Not connected");
        return session.subscribe(topic, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return type; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                Platform.runLater(() -> handler.accept(type.cast(payload)));
            }
        });
    }

    public void disconnect() {
        if (session != null) {
            try { session.disconnect(); } catch (Exception ignored) {}
            session = null;
        }
    }
}
