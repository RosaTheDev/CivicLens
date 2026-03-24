package com.civiclens.controller;

import com.civiclens.service.CivicAssistantService;
import com.civiclens.service.ElectionsService;
import com.civiclens.service.VotingLocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CivicInfoController {

    private final CivicAssistantService civicAssistantService;
    private final ElectionsService electionsService;
    private final VotingLocationService votingLocationService;

    public CivicInfoController(
            CivicAssistantService civicAssistantService,
            ElectionsService electionsService,
            VotingLocationService votingLocationService
    ) {
        this.civicAssistantService = civicAssistantService;
        this.electionsService = electionsService;
        this.votingLocationService = votingLocationService;
    }

    @PostMapping("/assistant/ask")
    public ResponseEntity<CivicAssistantService.AssistantAnswer> askAssistant(@Valid @RequestBody AssistantAskRequest request) {
        return ResponseEntity.ok(civicAssistantService.answer(request.question(), request.state()));
    }

    @GetMapping("/elections")
    public ResponseEntity<List<ElectionsService.ElectionDto>> getUpcomingElections(
            @RequestParam(value = "state", required = false) String state
    ) {
        return ResponseEntity.ok(electionsService.getUpcomingElections(state));
    }

    @GetMapping("/voting-locations")
    public ResponseEntity<VotingLocationService.VotingLocationsResponse> getVotingLocations(
            @RequestParam(value = "address", required = false) String address
    ) {
        return ResponseEntity.ok(votingLocationService.getVotingLocations(address));
    }

    public record AssistantAskRequest(
            @NotBlank(message = "question is required")
            String question,
            String state
    ) {}
}
