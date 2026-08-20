package com.sk.skala.day1.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.skala.day1.chat.AnswerDto;
import com.sk.skala.day1.chat.HelpDeskService;
import com.sk.skala.day1.chat.HistoryMessage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lab3")
@RequiredArgsConstructor
public class ChatController {

    private final HelpDeskService helpDesk;

    @PostMapping("/chat")
    public AnswerDto chat(@RequestBody ChatRequest request) {
        return helpDesk.chat(request.userId(), request.sessionId(), request.message());
    }

    @GetMapping("/chat/history")
    public List<HistoryMessage> history(
            @RequestParam String userId,
            @RequestParam String sessionId) {
        return helpDesk.history(userId, sessionId);
    }

    public record ChatRequest(String userId, String sessionId, String message) {
    }
}
