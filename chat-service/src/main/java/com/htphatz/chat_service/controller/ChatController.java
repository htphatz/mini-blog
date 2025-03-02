package com.htphatz.chat_service.controller;

import com.htphatz.chat_service.document.ChatMessage;
import com.htphatz.chat_service.dto.response.APIResponse;
import com.htphatz.chat_service.dto.response.PageDto;
import com.htphatz.chat_service.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        ChatMessage message = chatMessageService.createChatMessage(chatMessage);
        messagingTemplate.convertAndSendToUser(message.getRecipientId(), "/queue/messages", message);
    }

    @GetMapping("messages/{senderId}/{recipientId}")
    public APIResponse<PageDto<ChatMessage>> findChatMessages(
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @PathVariable("senderId") String senderId,
            @PathVariable("recipientId") String recipientId) {
        PageDto<ChatMessage> result = chatMessageService.findChatMessages(pageNumber, pageSize, senderId, recipientId);
        return APIResponse.<PageDto<ChatMessage>>builder().result(result).build();
    }
}
