package com.matuconnect.controller;


import com.matuconnect.model.JourneySearch;
import com.matuconnect.model.User;
import com.matuconnect.repository.JourneySearchRepository;
import com.matuconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The signed-in user's own data.
 * <p>
 * History is read by resolving the authenticated principal rather than by
 * accepting a user id from the caller: an endpoint that took
 * {@code ?userId=} would let any signed-in user read anyone else's journeys
 * simply by changing the number.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int MAX_HISTORY_LIMIT = 200;

    private final UserRepository userRepository;
    private final JourneySearchRepository journeySearchRepository;

    @GetMapping("/history")
    public List<JourneySearchDto> history(
            Authentication authentication,
            @RequestParam(defaultValue = "" + DEFAULT_HISTORY_LIMIT) int limit) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        int capped = Math.clamp(limit, 1, MAX_HISTORY_LIMIT);

        return journeySearchRepository
                .findByUser_IdOrderBySearchedAtDesc(user.getId(), PageRequest.of(0, capped))
                .stream()
                .map(JourneySearchDto::from)
                .toList();
    }
}
