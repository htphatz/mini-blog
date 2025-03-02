package com.htphatz.chat_service.service;

import com.htphatz.chat_service.document.User;
import com.htphatz.chat_service.dto.response.PageDto;
import com.htphatz.chat_service.enums.Status;
import com.htphatz.chat_service.exception.AppException;
import com.htphatz.chat_service.exception.ErrorCode;
import com.htphatz.chat_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void saveUser(User user) {
        user.setStatus(Status.ONLINE);
        userRepository.save(user);
    }

    public void disconnectUser(User user) {
        var storedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (storedUser != null) {
            storedUser.setStatus(Status.OFFLINE);
            userRepository.save(storedUser);
        }
    }

    public PageDto<User> findConnectedUsers(Integer pageNumber, Integer pageSize) {
        pageNumber--;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<User> users = userRepository.findAllByStatus(Status.ONLINE, pageable);
        return PageDto.of(users);
    }
}
