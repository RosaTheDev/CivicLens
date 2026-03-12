package com.civiclens.service;

import com.civiclens.domain.User;
import com.civiclens.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        Object details = auth.getDetails();
        if (details instanceof Long userId) {
            return userId;
        }
        String email = auth.getName();
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    public User getCurrentUser() {
        Long id = getCurrentUserId();
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }
}
