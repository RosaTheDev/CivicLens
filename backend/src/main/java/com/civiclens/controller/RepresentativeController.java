package com.civiclens.controller;

import com.civiclens.domain.DonorSummary;
import com.civiclens.domain.Representative;
import com.civiclens.domain.Stance;
import com.civiclens.domain.User;
import com.civiclens.domain.UserStance;
import com.civiclens.service.CurrentUserService;
import com.civiclens.service.DonorSummaryService;
import com.civiclens.service.RecentBillsService;
import com.civiclens.service.RepresentativeService;
import com.civiclens.service.UserStanceService;
import com.civiclens.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RepresentativeController {

    private final CurrentUserService currentUserService;
    private final RepresentativeService representativeService;
    private final WatchlistService watchlistService;
    private final DonorSummaryService donorSummaryService;
    private final UserStanceService userStanceService;
    private final RecentBillsService recentBillsService;

    public RepresentativeController(
            CurrentUserService currentUserService,
            RepresentativeService representativeService,
            WatchlistService watchlistService,
            DonorSummaryService donorSummaryService,
            UserStanceService userStanceService,
            RecentBillsService recentBillsService
    ) {
        this.currentUserService = currentUserService;
        this.representativeService = representativeService;
        this.watchlistService = watchlistService;
        this.donorSummaryService = donorSummaryService;
        this.userStanceService = userStanceService;
        this.recentBillsService = recentBillsService;
    }

    public record UserStanceDto(Long representativeId, Stance stance, String note) {}

    @GetMapping("/me")
    public User me() {
        return currentUserService.getCurrentUser();
    }

    @GetMapping("/representatives")
    public List<Representative> getRepresentativesByZip(@RequestParam("zip") String zip) {
        return representativeService.getRepresentativesByZip(zip);
    }

    @GetMapping("/representatives/{id}")
    public ResponseEntity<Representative> getRepresentative(@PathVariable("id") Long id) {
        Representative rep = representativeService.getById(id);
        if (rep == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(rep);
    }

    @GetMapping("/representatives/{id}/recent-bills")
    public ResponseEntity<List<RecentBillsService.RecentBill>> getRecentBills(@PathVariable("id") Long id) {
        Representative rep = representativeService.getById(id);
        if (rep == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(recentBillsService.getRecentBillsForRepresentative(rep));
    }

    @GetMapping("/watchlist")
    public List<Representative> myWatchlist() {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        return watchlistService.getWatchlist(userId);
    }

    @PostMapping("/watchlist/{representativeId}")
    public ResponseEntity<Representative> addToWatchlist(@PathVariable("representativeId") Long representativeId) {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Representative rep = watchlistService.addToWatchlist(userId, representativeId);
        return ResponseEntity.ok(rep);
    }

    @DeleteMapping("/watchlist/{representativeId}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable("representativeId") Long representativeId) {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        watchlistService.removeFromWatchlist(userId, representativeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/donor-summaries")
    public ResponseEntity<DonorSummary> donorSummary(
            @RequestParam("representativeId") Long representativeId,
            @RequestParam(value = "cycleYear", required = false) Integer cycleYear
    ) {
        DonorSummary summary = donorSummaryService.getForRepresentative(representativeId, cycleYear);
        if (summary == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/stances")
    public ResponseEntity<UserStance> setStanceOnRepresentative(
            @RequestParam("representativeId") Long representativeId,
            @RequestParam("stance") Stance stance,
            @RequestParam(value = "note", required = false) String note
    ) {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserStance result = userStanceService.setStanceOnRepresentative(userId, representativeId, stance, note);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stances/{representativeId}")
    public ResponseEntity<UserStanceDto> getStanceForRepresentative(@PathVariable("representativeId") Long representativeId) {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserStance stance = userStanceService.getStanceForRepresentative(userId, representativeId);
        if (stance == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(new UserStanceDto(
                representativeId,
                stance.getStance(),
                stance.getNote()
        ));
    }

    @GetMapping("/stances")
    public ResponseEntity<List<UserStanceDto>> getStancesForUser() {
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<UserStanceDto> result = userStanceService.getStancesForUser(userId).stream()
                .filter(us -> us.getRepresentative() != null && us.getRepresentative().getId() != null)
                .map(us -> new UserStanceDto(
                        us.getRepresentative().getId(),
                        us.getStance(),
                        us.getNote()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}

