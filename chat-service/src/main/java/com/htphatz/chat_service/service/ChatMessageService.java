package com.htphatz.chat_service.service;

import com.htphatz.chat_service.document.ChatMessage;
import com.htphatz.chat_service.dto.response.PageDto;
import com.htphatz.chat_service.exception.AppException;
import com.htphatz.chat_service.exception.ErrorCode;
import com.htphatz.chat_service.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;

    public ChatMessage createChatMessage(ChatMessage chatMessage) {
        var chatId = chatRoomService
                .getChatRoomId(chatMessage.getSenderId(), chatMessage.getRecipientId(), true)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_NOT_FOUND));
        chatMessage.setChatId(chatId);
        chatMessage.setTimestamp(Instant.now());
        chatMessageRepository.save(chatMessage);
        return chatMessage;
    }

    public PageDto<ChatMessage> findChatMessages(Integer pageNumber, Integer pageSize, String senderId, String recipientId) {
        pageNumber--;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("timestamp").ascending());
        var chatId = chatRoomService.getChatRoomId(senderId, recipientId, false);
        Page<ChatMessage> chatMessages = chatMessageRepository.findByChatId(String.valueOf(chatId), pageable);
        return PageDto.of(chatMessages);
    }
}

