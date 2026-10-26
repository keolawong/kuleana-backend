package com.kuleanasystems.assets.controller;

import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.service.GateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gates")
@RequiredArgsConstructor
public class GateController {

    private final GateService gateService;

    @GetMapping
    public ResponseEntity<List<Gate>> getGates() {
        List<Gate> gates = gateService.getActiveGates();
        return ResponseEntity.ok(gates);
    }
}
