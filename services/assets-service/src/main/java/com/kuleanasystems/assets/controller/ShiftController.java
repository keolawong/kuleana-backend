package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.entity.Shift;
import com.kuleanasystems.assets.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping
    public ResponseEntity<List<Shift>> getShifts(
            @RequestParam(required = false) UUID operationId,
            @RequestParam(required = false) String status) {
        List<Shift> shifts = shiftService.getShifts(operationId, status);
        return ResponseEntity.ok(shifts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shift> getShiftById(@PathVariable UUID id) {
        return shiftService.getShiftById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Shift> createShift(@RequestBody Shift shift) {
        Shift saved = shiftService.saveShift(shift);
        return ResponseEntity.ok(saved);
    }
}
