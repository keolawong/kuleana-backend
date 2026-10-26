package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.entity.Gate;
import com.kuleanasystems.assets.repository.GateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GateService {

    private final GateRepository gateRepository;

    public List<Gate> getActiveGates() {
        return gateRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }
}
