package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.dto.PersonStickerDto;
import com.kuleanasystems.assets.dto.PersonStickerPatchDto;
import com.kuleanasystems.assets.entity.PersonSticker;
import com.kuleanasystems.assets.service.PersonStickerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/person-stickers")
@RequiredArgsConstructor
public class PersonStickerController {

    private final PersonStickerService personStickerService;

    @PostMapping
    public ResponseEntity<PersonSticker> createPersonSticker(@Valid @RequestBody PersonStickerDto personStickerDto) {
        PersonSticker createdPersonSticker = personStickerService.createPersonSticker(personStickerDto);
        return ResponseEntity.created(URI.create("/api/person-stickers/" + createdPersonSticker.getPersonStickerId())).body(createdPersonSticker);
    }

    @PatchMapping("/{personStickerId}")
    public ResponseEntity<PersonSticker> updatePersonSticker(@PathVariable UUID personStickerId, @RequestBody PersonStickerPatchDto personStickerPatchDto) {
        PersonSticker updatedPersonSticker = personStickerService.updatePersonSticker(personStickerId, personStickerPatchDto);
        return ResponseEntity.ok(updatedPersonSticker);
    }

    @GetMapping
    public ResponseEntity<List<PersonSticker>> getPersonStickers(
            @RequestParam(required = false) UUID personStickerId,
            @RequestParam(required = false) UUID personId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID gateId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdAt,
            @RequestParam(required = false) String updatedBy,
            @RequestParam(required = false) Instant updatedAt) {

        List<PersonSticker> personStickers = personStickerService.searchPersonStickers(
                personStickerId, personId, shiftId, gateId,
                createdBy, createdAt, updatedBy, updatedAt);

        if (personStickers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(personStickers);
    }
}
