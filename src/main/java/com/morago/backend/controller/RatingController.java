package com.morago.backend.controller;

import com.morago.backend.dto.RatingUpsertRequest;
import com.morago.backend.service.rating.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/translators/{translatorId}/rating")
@RequiredArgsConstructor
@Tag(
        name = "Ratings",
        description = "Rate translators. Access: [USER, ADMIN]"
)
public class RatingController {

    private final RatingService ratingService;

    @Operation(
            summary = "Rate translator",
            description = "Access: [USER, ADMIN]\nCreate or update a rating for a translator."
    )
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void upsert(@PathVariable Long translatorId,
                       @Valid @RequestBody RatingUpsertRequest dto) {
        ratingService.upsertMyRating(translatorId, dto);
    }

    @Operation(
            summary = "Delete rating",
            description = "Access: [USER, ADMIN]\nRemove your rating for a translator."
    )
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void delete(@PathVariable Long translatorId) {
        ratingService.deleteMyRating(translatorId);
    }
}
