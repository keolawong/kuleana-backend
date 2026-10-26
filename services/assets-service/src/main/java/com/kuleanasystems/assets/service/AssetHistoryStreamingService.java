package com.kuleanasystems.assets.service;

import com.kuleanasystems.assets.dto.StreamEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.kuleanasystems.assets.entity.TicketBook;
import com.kuleanasystems.assets.repository.TicketBookRepository;
import com.kuleanasystems.assets.dto.AssetSearchDto;
import com.kuleanasystems.assets.repository.CashBankRepository;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetHistoryStreamingService {

    private final TrackerClient trackerClient;
    private final TicketBookRepository ticketBookRepository;
    private final com.kuleanasystems.assets.repository.TicketBookHistoryRepository ticketBookHistoryRepository;
    private final CashBankRepository cashBankRepository;
    private final com.kuleanasystems.assets.repository.CashBankHistoryRepository cashBankHistoryRepository;

    public AssetHistoryStreamingService(TrackerClient trackerClient,
                                       TicketBookRepository ticketBookRepository,
                                       com.kuleanasystems.assets.repository.TicketBookHistoryRepository ticketBookHistoryRepository,
                                       CashBankRepository cashBankRepository,
                                       com.kuleanasystems.assets.repository.CashBankHistoryRepository cashBankHistoryRepository) {
        this.trackerClient = trackerClient;
        this.ticketBookRepository = ticketBookRepository;
        this.ticketBookHistoryRepository = ticketBookHistoryRepository;
        this.cashBankRepository = cashBankRepository;
        this.cashBankHistoryRepository = cashBankHistoryRepository;
    }

    public List<AssetSearchDto> searchAssetsForHistory(String search) {
        if (search == null || search.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<AssetSearchDto> results = new java.util.ArrayList<>();
        
        // Search Ticket Books
        org.springframework.data.jpa.domain.Specification<TicketBook> tbSpec = com.kuleanasystems.assets.repository.AssetTicketBookSpecification.searchForAsset(null, search);
        List<TicketBook> ticketBooks = ticketBookRepository.findAll(tbSpec);
        java.util.Set<UUID> seenTbIds = new java.util.HashSet<>();
        for (TicketBook tb : ticketBooks) {
            if (seenTbIds.add(tb.getTicketBookId())) {
                AssetSearchDto dto = new AssetSearchDto();
                dto.setAssetType(com.kuleanasystems.assets.dto.AssetType.TICKET_BOOK);
                dto.setAssetId(tb.getTicketBookId());
                dto.setScanCode(tb.getScanCode());
                dto.setStartNum(tb.getStartNum());
                dto.setEndNum(tb.getEndNum());
                dto.setNextTicket(tb.getNextTicket());
                dto.setParentTicketBookId(tb.getParentTicketBookId());
                if (tb.getOperation() != null) dto.setOperationId(tb.getOperation().getOperationId());
                results.add(dto);
            }
        }

        // Search Cash Banks
        org.springframework.data.jpa.domain.Specification<com.kuleanasystems.assets.entity.CashBank> cbSpec = com.kuleanasystems.assets.repository.AssetCashBankSpecification.searchForAsset(null, search);
        List<com.kuleanasystems.assets.entity.CashBank> cashBanks = cashBankRepository.findAll(cbSpec);
        java.util.Set<UUID> seenCbIds = new java.util.HashSet<>();
        for (com.kuleanasystems.assets.entity.CashBank cb : cashBanks) {
            if (seenCbIds.add(cb.getCashBankId())) {
                AssetSearchDto dto = new AssetSearchDto();
                dto.setAssetType(com.kuleanasystems.assets.dto.AssetType.CASH_BANK);
                dto.setAssetId(cb.getCashBankId());
                dto.setBankName(cb.getBankName());
                dto.setParentCashBankId(cb.getParentCashBankId());
                if (cb.getOperation() != null) dto.setOperationId(cb.getOperation().getOperationId());
                results.add(dto);
            }
        }

        if (results.size() > 1) {
            boolean allTicketBooks = results.stream().allMatch(r -> com.kuleanasystems.assets.dto.AssetType.TICKET_BOOK.equals(r.getAssetType()));
            if (allTicketBooks) {
                // If they all have the same ultimate root, reduce to the root ticket book
                java.util.Set<UUID> roots = new java.util.HashSet<>();
                for (AssetSearchDto dto : results) {
                    roots.add(getRootTicketBookId(dto.getAssetId()));
                }
                if (roots.size() == 1) {
                    UUID rootId = roots.iterator().next();
                    AssetSearchDto rootDto = results.stream()
                            .filter(r -> r.getAssetId().equals(rootId))
                            .findFirst()
                            .orElseGet(() -> {
                                return ticketBookRepository.findById(rootId).map(tb -> {
                                    AssetSearchDto dto = new AssetSearchDto();
                                    dto.setAssetType(com.kuleanasystems.assets.dto.AssetType.TICKET_BOOK);
                                    dto.setAssetId(tb.getTicketBookId());
                                    dto.setScanCode(tb.getScanCode());
                                    dto.setStartNum(tb.getStartNum());
                                    dto.setEndNum(tb.getEndNum());
                                    dto.setNextTicket(tb.getNextTicket());
                                    dto.setParentTicketBookId(tb.getParentTicketBookId());
                                    if (tb.getOperation() != null) dto.setOperationId(tb.getOperation().getOperationId());
                                    return dto;
                                }).orElse(results.get(0));
                            });
                    return java.util.Collections.singletonList(rootDto);
                }
            }

            boolean allCashBanks = results.stream().allMatch(r -> com.kuleanasystems.assets.dto.AssetType.CASH_BANK.equals(r.getAssetType()));
            if (allCashBanks) {
                // If they all have the same ultimate root, reduce to the root cash bank
                java.util.Set<UUID> roots = new java.util.HashSet<>();
                for (AssetSearchDto dto : results) {
                    roots.add(getRootCashBankId(dto.getAssetId()));
                }
                if (roots.size() == 1) {
                    UUID rootId = roots.iterator().next();
                    AssetSearchDto rootDto = results.stream()
                            .filter(r -> r.getAssetId().equals(rootId))
                            .findFirst()
                            .orElseGet(() -> {
                                return cashBankRepository.findById(rootId).map(cb -> {
                                    AssetSearchDto dto = new AssetSearchDto();
                                    dto.setAssetType(com.kuleanasystems.assets.dto.AssetType.CASH_BANK);
                                    dto.setAssetId(cb.getCashBankId());
                                    dto.setBankName(cb.getBankName());
                                    dto.setParentCashBankId(cb.getParentCashBankId());
                                    if (cb.getOperation() != null) dto.setOperationId(cb.getOperation().getOperationId());
                                    return dto;
                                }).orElse(results.get(0));
                            });
                    return java.util.Collections.singletonList(rootDto);
                }
            }
        }

        return results;
    }

    public UUID getRootTicketBookId(UUID ticketBookId) {
        UUID current = ticketBookId;
        java.util.Set<UUID> visited = new java.util.HashSet<>();
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            Optional<TicketBook> tbOpt = ticketBookRepository.findById(current);
            if (tbOpt.isPresent() && tbOpt.get().getParentTicketBookId() != null) {
                current = tbOpt.get().getParentTicketBookId();
            } else {
                break;
            }
        }
        return current;
    }

    public UUID getRootCashBankId(UUID cashBankId) {
        UUID current = cashBankId;
        java.util.Set<UUID> visited = new java.util.HashSet<>();
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            Optional<com.kuleanasystems.assets.entity.CashBank> cbOpt = cashBankRepository.findById(current);
            if (cbOpt.isPresent() && cbOpt.get().getParentCashBankId() != null) {
                current = cbOpt.get().getParentCashBankId();
            } else {
                break;
            }
        }
        return current;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void streamHistory(UUID assetId, String assetType, String token, SseEmitter emitter) {
        try {
            if ("CASH_BANK".equalsIgnoreCase(assetType) || "Cash Bank".equalsIgnoreCase(assetType)) {
                UUID rootId = getRootCashBankId(assetId);
                Optional<com.kuleanasystems.assets.entity.CashBank> cbOpt = cashBankRepository.findById(rootId);
                if (cbOpt.isEmpty()) {
                    emitter.send(SseEmitter.event()
                            .name(StreamEvent.NOT_FOUND)
                            .data(Map.of("assetId", assetId, "message", "Cash Bank asset not found")));
                    emitter.complete();
                    return;
                }

                java.util.Set<java.util.UUID> visited = new java.util.HashSet<>();
                processCashBankNode(cbOpt.get(), token, emitter, visited, 0, 5);

                emitter.send(SseEmitter.event()
                        .name(StreamEvent.STREAM_COMPLETE)
                        .data(Map.of("hasMore", false)));
                emitter.complete();
                return;
            }

            // Default to Ticket Book
            UUID rootId = getRootTicketBookId(assetId);
            Optional<TicketBook> tbOpt = ticketBookRepository.findById(rootId);
            if (tbOpt.isEmpty()) {
                emitter.send(SseEmitter.event()
                        .name(StreamEvent.NOT_FOUND)
                        .data(Map.of("assetId", assetId, "message", "Ticket Book asset not found")));
                emitter.complete();
                return;
            }

            java.util.Set<java.util.UUID> visited = new java.util.HashSet<>();
            processTicketBookNode(tbOpt.get(), token, emitter, visited, 0, 5);

            emitter.send(SseEmitter.event()
                    .name(StreamEvent.STREAM_COMPLETE)
                    .data(Map.of("hasMore", false)));
            
            emitter.complete();
            
        } catch (Exception e) {
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
        }
    }

    private void processTicketBookNode(TicketBook asset, String token, SseEmitter emitter, java.util.Set<java.util.UUID> visited, int currentDepth, int maxDepth) throws Exception {
        if (asset == null || currentDepth > maxDepth || visited.contains(asset.getTicketBookId())) {
            return;
        }
        
        visited.add(asset.getTicketBookId());
        String scanCode = asset.getScanCode() != null ? asset.getScanCode() : asset.getTicketBookId().toString();
        
        // Asset discovered
        Map<String, Object> discoveredData = new java.util.HashMap<>();
        discoveredData.put("scanCode", scanCode);
        discoveredData.put("assetId", asset.getTicketBookId());
        discoveredData.put("assetType", "Ticket Book");
        discoveredData.put("startNum", asset.getStartNum());
        discoveredData.put("endNum", asset.getEndNum());
        discoveredData.put("nextTicket", asset.getNextTicket() != null ? asset.getNextTicket() : asset.getStartNum());
        discoveredData.put("depth", currentDepth);
        if (asset.getParentTicketBookId() != null) {
            discoveredData.put("parentTicketBookId", asset.getParentTicketBookId());
        }
        if (asset.getStatus() != null) {
            discoveredData.put("status", asset.getStatus());
        }

        emitter.send(SseEmitter.event()
                .name(StreamEvent.ASSET_DISCOVERED)
                .data(discoveredData));
                
        List<com.kuleanasystems.assets.entity.TicketBookHistory> histories = ticketBookHistoryRepository.findByTicketBookIdOrderByCreatedAtAsc(asset.getTicketBookId());
        List<Map<String, Object>> historyActions = buildTicketBookActionsFromHistory(asset, histories);

        List<Object> timeline = null;
        boolean trackerAvailable = true;

        try {
            timeline = trackerClient.getTimeline(asset.getTicketBookId().toString(), token);
            if (timeline != null && !timeline.isEmpty()) {
                Map<UUID, com.kuleanasystems.assets.entity.TicketBookHistory> historyByProcessId = new java.util.HashMap<>();
                Map<String, com.kuleanasystems.assets.entity.TicketBookHistory> historyByStatus = new java.util.HashMap<>();
                for (com.kuleanasystems.assets.entity.TicketBookHistory h : histories) {
                    if (h.getProcessId() != null) {
                        historyByProcessId.putIfAbsent(h.getProcessId(), h);
                    }
                    if (h.getStatus() != null) {
                        historyByStatus.putIfAbsent(h.getStatus(), h);
                    }
                }

                for (Object obj : timeline) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> eventMap = (Map<String, Object>) obj;
                        enrichTicketBookEventMap(eventMap, historyByProcessId, historyByStatus, asset);

                        Object actionsObj = eventMap.get("actions");
                        List<Object> actionsList = actionsObj instanceof List ? (List<Object>) actionsObj : new java.util.ArrayList<>();
                        List<Map<String, Object>> mergedActions = mergeTicketBookActions(actionsList, historyActions, historyByProcessId, historyByStatus, asset);
                        eventMap.put("actions", mergedActions);
                    }
                }
            } else {
                timeline = createSyntheticTicketBookTimeline(discoveredData, asset, historyActions);
            }
        } catch (RuntimeException e) {
            trackerAvailable = false;
            timeline = createSyntheticTicketBookTimeline(discoveredData, asset, historyActions);
        }

        if (!trackerAvailable) {
            Map<String, Object> fallbackData = new java.util.HashMap<>(discoveredData);
            fallbackData.put("events", timeline);
            emitter.send(SseEmitter.event()
                    .name(StreamEvent.TIMELINE_LOADED)
                    .data(fallbackData));
            emitter.send(SseEmitter.event()
                    .name(StreamEvent.TIMELINE_UNAVAILABLE)
                    .data(Map.of(
                            "scanCode", scanCode,
                            "assetType", "Ticket Book",
                            "reason", "Tracker service unavailable",
                            "depth", currentDepth
                    )));
        } else {
            Map<String, Object> loadedData = new java.util.HashMap<>(discoveredData);
            loadedData.put("events", timeline);
            emitter.send(SseEmitter.event()
                    .name(StreamEvent.TIMELINE_LOADED)
                    .data(loadedData));
        }

        // Process Parent if unvisited
        if (asset.getParentTicketBookId() != null && !visited.contains(asset.getParentTicketBookId())) {
            Optional<TicketBook> parentOpt = ticketBookRepository.findById(asset.getParentTicketBookId());
            if (parentOpt.isPresent()) {
                processTicketBookNode(parentOpt.get(), token, emitter, visited, currentDepth + 1, maxDepth);
            }
        }

        // Process Children
        List<TicketBook> children = ticketBookRepository.findByParentTicketBookId(asset.getTicketBookId());
        for (TicketBook child : children) {
            if (!visited.contains(child.getTicketBookId())) {
                processTicketBookNode(child, token, emitter, visited, currentDepth + 1, maxDepth);
            }
        }
    }

    private void processCashBankNode(com.kuleanasystems.assets.entity.CashBank asset, String token, SseEmitter emitter, java.util.Set<java.util.UUID> visited, int currentDepth, int maxDepth) throws Exception {
        if (asset == null || currentDepth > maxDepth || visited.contains(asset.getCashBankId())) {
            return;
        }
        
        visited.add(asset.getCashBankId());
        String scanCode = asset.getBankName() != null ? asset.getBankName() : asset.getCashBankId().toString();
        
        // Asset discovered
        Map<String, Object> discoveredData = new java.util.HashMap<>();
        discoveredData.put("scanCode", scanCode);
        discoveredData.put("bankName", asset.getBankName());
        discoveredData.put("assetId", asset.getCashBankId());
        discoveredData.put("assetType", "Cash Bank");
        discoveredData.put("status", asset.getStatus());
        discoveredData.put("startingAmount", asset.getStartingAmount());
        discoveredData.put("officialCount", asset.getOfficialCount());
        discoveredData.put("count100s", asset.getCount100s());
        discoveredData.put("count50s", asset.getCount50s());
        discoveredData.put("count20s", asset.getCount20s());
        discoveredData.put("count10s", asset.getCount10s());
        discoveredData.put("count5s", asset.getCount5s());
        discoveredData.put("count1s", asset.getCount1s());
        discoveredData.put("count1cs", asset.getCount1cs());
        discoveredData.put("depth", currentDepth);
        if (asset.getParentCashBankId() != null) {
            discoveredData.put("parentCashBankId", asset.getParentCashBankId());
        }

        emitter.send(SseEmitter.event()
                .name(StreamEvent.ASSET_DISCOVERED)
                .data(discoveredData));
                
        List<com.kuleanasystems.assets.entity.CashBankHistory> histories = cashBankHistoryRepository.findByCashBankIdOrderByCreatedAtAsc(asset.getCashBankId());
        List<Map<String, Object>> historyActions = buildCashBankActionsFromHistory(asset, histories);

        List<Object> timeline = null;
        boolean trackerAvailable = true;

        try {
            timeline = trackerClient.getTimeline(asset.getCashBankId().toString(), token);
            if (timeline != null && !timeline.isEmpty()) {
                Map<UUID, com.kuleanasystems.assets.entity.CashBankHistory> historyByProcessId = new java.util.HashMap<>();
                Map<String, com.kuleanasystems.assets.entity.CashBankHistory> historyByStatus = new java.util.HashMap<>();
                for (com.kuleanasystems.assets.entity.CashBankHistory h : histories) {
                    if (h.getProcessId() != null) {
                        historyByProcessId.putIfAbsent(h.getProcessId(), h);
                    }
                    if (h.getStatus() != null) {
                        historyByStatus.putIfAbsent(h.getStatus(), h);
                    }
                }

                for (Object obj : timeline) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> eventMap = (Map<String, Object>) obj;
                        enrichCashBankEventMap(eventMap, historyByProcessId, historyByStatus, asset);

                        Object actionsObj = eventMap.get("actions");
                        List<Object> actionsList = actionsObj instanceof List ? (List<Object>) actionsObj : new java.util.ArrayList<>();
                        List<Map<String, Object>> mergedActions = mergeCashBankActions(actionsList, historyActions, historyByProcessId, historyByStatus, asset);
                        eventMap.put("actions", mergedActions);
                    }
                }
            } else {
                timeline = createSyntheticCashBankTimeline(discoveredData, asset, historyActions);
            }
        } catch (RuntimeException e) {
            trackerAvailable = false;
            timeline = createSyntheticCashBankTimeline(discoveredData, asset, historyActions);
        }

        if (!trackerAvailable) {
            Map<String, Object> fallbackData = new java.util.HashMap<>(discoveredData);
            fallbackData.put("events", timeline);
            emitter.send(SseEmitter.event()
                    .name(StreamEvent.TIMELINE_LOADED)
                    .data(fallbackData));
            emitter.send(SseEmitter.event()
                    .name(StreamEvent.TIMELINE_UNAVAILABLE)
                    .data(Map.of(
                            "scanCode", scanCode,
                            "assetType", "Cash Bank",
                            "reason", "Tracker service unavailable",
                            "depth", currentDepth
                    )));
        } else {
            Map<String, Object> loadedData = new java.util.HashMap<>(discoveredData);
            loadedData.put("events", timeline);
            emitter.send(SseEmitter.event()
                    .name(StreamEvent.TIMELINE_LOADED)
                    .data(loadedData));
        }

        // Process Parent if unvisited
        if (asset.getParentCashBankId() != null && !visited.contains(asset.getParentCashBankId())) {
            Optional<com.kuleanasystems.assets.entity.CashBank> parentOpt = cashBankRepository.findById(asset.getParentCashBankId());
            if (parentOpt.isPresent()) {
                processCashBankNode(parentOpt.get(), token, emitter, visited, currentDepth + 1, maxDepth);
            }
        }

        // Process Children
        List<com.kuleanasystems.assets.entity.CashBank> children = cashBankRepository.findByParentCashBankId(asset.getCashBankId());
        for (com.kuleanasystems.assets.entity.CashBank child : children) {
            if (!visited.contains(child.getCashBankId())) {
                processCashBankNode(child, token, emitter, visited, currentDepth + 1, maxDepth);
            }
        }
    }

    private void enrichTicketBookEventMap(Map<String, Object> eventMap,
                                          Map<UUID, com.kuleanasystems.assets.entity.TicketBookHistory> historyByProcessId,
                                          Map<String, com.kuleanasystems.assets.entity.TicketBookHistory> historyByStatus,
                                          com.kuleanasystems.assets.entity.TicketBook asset) {
        String flowIdStr = (String) eventMap.get("flowId");
        String status = (String) eventMap.get("status");

        com.kuleanasystems.assets.entity.TicketBookHistory matched = null;
        if (flowIdStr != null) {
            try {
                UUID flowId = UUID.fromString(flowIdStr);
                matched = historyByProcessId.get(flowId);
            } catch (IllegalArgumentException e) {
                // Ignore malformed UUID from external tracker
            }
        }
        if (matched == null && status != null) {
            matched = historyByStatus.get(status);
        }

        if (matched != null) {
            eventMap.put("startNum", matched.getStartNum());
            eventMap.put("endNum", matched.getEndNum());
            eventMap.put("nextTicket", matched.getNextTicket() != null ? matched.getNextTicket() : matched.getStartNum());
            if (matched.getGate() != null) eventMap.put("gateName", matched.getGate().getName());
            if (matched.getShift() != null) eventMap.put("shiftName", matched.getShift().getName());
        } else {
            eventMap.put("startNum", asset.getStartNum());
            eventMap.put("endNum", asset.getEndNum());
            eventMap.put("nextTicket", asset.getNextTicket() != null ? asset.getNextTicket() : asset.getStartNum());
            if (asset.getGate() != null) eventMap.put("gateName", asset.getGate().getName());
            if (asset.getShift() != null) eventMap.put("shiftName", asset.getShift().getName());
        }
    }

    private void enrichCashBankEventMap(Map<String, Object> eventMap,
                                        Map<UUID, com.kuleanasystems.assets.entity.CashBankHistory> historyByProcessId,
                                        Map<String, com.kuleanasystems.assets.entity.CashBankHistory> historyByStatus,
                                        com.kuleanasystems.assets.entity.CashBank asset) {
        String flowIdStr = (String) eventMap.get("flowId");
        String status = (String) eventMap.get("status");

        com.kuleanasystems.assets.entity.CashBankHistory matched = null;
        if (flowIdStr != null) {
            try {
                UUID flowId = UUID.fromString(flowIdStr);
                matched = historyByProcessId.get(flowId);
            } catch (IllegalArgumentException e) {
                // Ignore malformed UUID
            }
        }
        if (matched == null && status != null) {
            matched = historyByStatus.get(status);
        }

        if (matched != null) {
            eventMap.put("startingAmount", matched.getStartingAmount());
            eventMap.put("officialCount", matched.getOfficialCount());
            eventMap.put("count100s", matched.getCount100s());
            eventMap.put("count50s", matched.getCount50s());
            eventMap.put("count20s", matched.getCount20s());
            eventMap.put("count10s", matched.getCount10s());
            eventMap.put("count5s", matched.getCount5s());
            eventMap.put("count1s", matched.getCount1s());
            eventMap.put("count1cs", matched.getCount1cs());
            if (matched.getGate() != null) eventMap.put("gateName", matched.getGate().getName());
            if (matched.getShift() != null) eventMap.put("shiftName", matched.getShift().getName());
        } else {
            eventMap.put("startingAmount", asset.getStartingAmount());
            eventMap.put("officialCount", asset.getOfficialCount());
            eventMap.put("count100s", asset.getCount100s());
            eventMap.put("count50s", asset.getCount50s());
            eventMap.put("count20s", asset.getCount20s());
            eventMap.put("count10s", asset.getCount10s());
            eventMap.put("count5s", asset.getCount5s());
            eventMap.put("count1s", asset.getCount1s());
            eventMap.put("count1cs", asset.getCount1cs());
            if (asset.getGate() != null) eventMap.put("gateName", asset.getGate().getName());
            if (asset.getShift() != null) eventMap.put("shiftName", asset.getShift().getName());
        }
    }

    private List<Map<String, Object>> buildCashBankActionsFromHistory(
            com.kuleanasystems.assets.entity.CashBank asset,
            List<com.kuleanasystems.assets.entity.CashBankHistory> histories) {
        List<Map<String, Object>> actions = new java.util.ArrayList<>();
        if (histories == null || histories.isEmpty()) {
            Map<String, Object> creationAction = new java.util.HashMap<>();
            creationAction.put("id", UUID.randomUUID().toString());
            creationAction.put("flowName", "Asset-Action");
            creationAction.put("status", "Created");
            creationAction.put("createdAt", asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : java.time.Instant.now().toString());
            creationAction.put("createdBy", asset.getCreatedBy() != null ? asset.getCreatedBy() : "System");
            creationAction.put("startingAmount", asset.getStartingAmount());

            String amtStr = asset.getStartingAmount() != null ? String.format("%.2f", asset.getStartingAmount()) : "0.00";
            Map<String, Object> step = new java.util.HashMap<>();
            step.put("stepName", "CREATION");
            step.put("status", "Created");
            step.put("message", "Cash bank created with starting float of $" + amtStr);
            step.put("amount", asset.getStartingAmount());
            step.put("createdBy", asset.getCreatedBy() != null ? asset.getCreatedBy() : "System");
            step.put("createdAt", asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : java.time.Instant.now().toString());
            creationAction.put("steps", java.util.List.of(step));
            actions.add(creationAction);
            return actions;
        }

        // Entry 0: Creation
        com.kuleanasystems.assets.entity.CashBankHistory first = histories.get(0);
        Map<String, Object> creationAction = new java.util.HashMap<>();
        creationAction.put("id", first.getCashBankHistoryId() != null ? first.getCashBankHistoryId().toString() : UUID.randomUUID().toString());
        creationAction.put("flowName", "Asset-Action");
        creationAction.put("status", "Created");
        creationAction.put("createdAt", first.getCreatedAt() != null ? first.getCreatedAt().toString() : (asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : java.time.Instant.now().toString()));
        creationAction.put("createdBy", first.getCreatedBy() != null ? first.getCreatedBy() : (asset.getCreatedBy() != null ? asset.getCreatedBy() : "System"));
        creationAction.put("startingAmount", first.getStartingAmount() != null ? first.getStartingAmount() : asset.getStartingAmount());

        java.math.BigDecimal initialFloat = first.getStartingAmount() != null ? first.getStartingAmount() : asset.getStartingAmount();
        String initialFloatStr = initialFloat != null ? String.format("%.2f", initialFloat) : "0.00";
        Map<String, Object> createStep = new java.util.HashMap<>();
        createStep.put("stepName", "CREATION");
        createStep.put("status", "Created");
        createStep.put("message", "Cash bank created with starting float of $" + initialFloatStr);
        createStep.put("amount", initialFloat);
        createStep.put("createdBy", first.getCreatedBy() != null ? first.getCreatedBy() : "System");
        createStep.put("createdAt", first.getCreatedAt() != null ? first.getCreatedAt().toString() : java.time.Instant.now().toString());
        creationAction.put("steps", java.util.List.of(createStep));
        actions.add(creationAction);

        for (int i = 1; i < histories.size(); i++) {
            com.kuleanasystems.assets.entity.CashBankHistory prev = histories.get(i - 1);
            com.kuleanasystems.assets.entity.CashBankHistory curr = histories.get(i);

            String status = curr.getStatus() != null ? curr.getStatus() : "Updated";
            String prevStatus = prev.getStatus();
            String gateName = curr.getGate() != null ? curr.getGate().getName() : (prev.getGate() != null ? prev.getGate().getName() : null);
            String shiftName = curr.getShift() != null ? curr.getShift().getName() : (prev.getShift() != null ? prev.getShift().getName() : null);
            String stepName = "ASSIGNMENT";
            String message = "";
            String actionStatus = status;

            if ("Assigned".equalsIgnoreCase(status)) {
                actionStatus = "Assigned";
                stepName = "ASSIGNMENT";
                message = "Cash bank assigned" + (gateName != null ? " to " + gateName : "") + (shiftName != null ? " for shift " + shiftName : "");
            } else if (("Available".equalsIgnoreCase(status) || "Unassigned".equalsIgnoreCase(status)) && "Assigned".equalsIgnoreCase(prevStatus)) {
                actionStatus = "Unassigned";
                stepName = "ASSIGNMENT";
                message = "Cash bank unassigned" + (gateName != null ? " from " + gateName : "") + " and returned to available inventory";
            } else if ("Returned Unused".equalsIgnoreCase(status)) {
                actionStatus = "Returned Unused";
                stepName = "RETURN";
                message = "Cash bank marked as returned unused without being opened";
            } else if ("Returned".equalsIgnoreCase(status) && curr.getOfficialCount() == null) {
                actionStatus = "Returned";
                stepName = "RETURN";
                message = "Cash bank checked in for reconciliation";
            } else if (curr.getOfficialCount() != null || "Reconciled".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                actionStatus = "Counted";
                stepName = "RETURN";
                String countStr = curr.getOfficialCount() != null ? "$" + String.format("%.2f", curr.getOfficialCount()) : "recorded count";
                message = "Cash bank counted and reconciled at shift close. Final count: " + countStr;
            } else {
                actionStatus = "Updated";
                stepName = "EDIT";
                message = "Cash bank details updated";
            }

            Map<String, Object> action = new java.util.HashMap<>();
            action.put("id", curr.getCashBankHistoryId() != null ? curr.getCashBankHistoryId().toString() : UUID.randomUUID().toString());
            action.put("flowName", "Asset-Action");
            action.put("status", actionStatus);
            action.put("createdAt", curr.getCreatedAt() != null ? curr.getCreatedAt().toString() : java.time.Instant.now().toString());
            action.put("createdBy", curr.getCreatedBy() != null ? curr.getCreatedBy() : "System");
            action.put("startingAmount", curr.getStartingAmount() != null ? curr.getStartingAmount() : asset.getStartingAmount());
            action.put("officialCount", curr.getOfficialCount() != null ? curr.getOfficialCount() : asset.getOfficialCount());
            action.put("count100s", curr.getCount100s());
            action.put("count50s", curr.getCount50s());
            action.put("count20s", curr.getCount20s());
            action.put("count10s", curr.getCount10s());
            action.put("count5s", curr.getCount5s());
            action.put("count1s", curr.getCount1s());
            action.put("count1cs", curr.getCount1cs());
            if (gateName != null) action.put("gateName", gateName);
            if (shiftName != null) action.put("shiftName", shiftName);

            Map<String, Object> step = new java.util.HashMap<>();
            step.put("stepName", stepName);
            step.put("status", actionStatus);
            step.put("message", message);
            step.put("amount", curr.getOfficialCount() != null ? curr.getOfficialCount() : curr.getStartingAmount());
            step.put("createdBy", curr.getCreatedBy() != null ? curr.getCreatedBy() : "System");
            step.put("createdAt", curr.getCreatedAt() != null ? curr.getCreatedAt().toString() : java.time.Instant.now().toString());
            action.put("steps", java.util.List.of(step));
            actions.add(action);
        }

        return actions;
    }

    private List<Map<String, Object>> mergeCashBankActions(
            List<Object> trackerActions,
            List<Map<String, Object>> historyActions,
            Map<UUID, com.kuleanasystems.assets.entity.CashBankHistory> historyByProcessId,
            Map<String, com.kuleanasystems.assets.entity.CashBankHistory> historyByStatus,
            com.kuleanasystems.assets.entity.CashBank asset) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Set<String> presentStatuses = new java.util.HashSet<>();

        for (Object act : trackerActions) {
            if (act instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> actionMap = new java.util.HashMap<>((Map<String, Object>) act);
                enrichCashBankEventMap(actionMap, historyByProcessId, historyByStatus, asset);
                String status = (String) actionMap.get("status");
                if (status != null) {
                    presentStatuses.add(status.toLowerCase());
                }
                result.add(actionMap);
            }
        }

        // Merge any missing history actions (such as Created, Unassigned, Returned Unused, Counted)
        for (Map<String, Object> hAction : historyActions) {
            String status = (String) hAction.get("status");
            if (status != null) {
                String normalizedStatus = status.toLowerCase();
                if (normalizedStatus.contains("created") && !presentStatuses.contains("created")) {
                    result.add(hAction);
                    presentStatuses.add("created");
                } else if (normalizedStatus.contains("unassigned") && !presentStatuses.contains("unassigned")) {
                    result.add(hAction);
                    presentStatuses.add("unassigned");
                } else if (normalizedStatus.contains("returned unused") && !presentStatuses.contains("returned unused")) {
                    result.add(hAction);
                    presentStatuses.add("returned unused");
                } else if (!presentStatuses.contains(normalizedStatus)) {
                    result.add(hAction);
                    presentStatuses.add(normalizedStatus);
                }
            }
        }

        // Sort chronologically by createdAt
        result.sort((a, b) -> {
            String tA = (String) a.get("createdAt");
            String tB = (String) b.get("createdAt");
            if (tA == null && tB == null) return 0;
            if (tA == null) return -1;
            if (tB == null) return 1;
            return tA.compareTo(tB);
        });

        return result;
    }

    private List<Object> createSyntheticCashBankTimeline(
            Map<String, Object> discoveredData,
            com.kuleanasystems.assets.entity.CashBank asset,
            List<Map<String, Object>> historyActions) {
        Map<String, Object> parentEvent = new java.util.HashMap<>(discoveredData);
        parentEvent.put("id", UUID.randomUUID().toString());
        parentEvent.put("flowName", "Asset-Lifecycle");
        parentEvent.put("status", asset.getStatus() != null ? asset.getStatus().toUpperCase() : "ACTIVE");
        parentEvent.put("createdAt", asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : java.time.Instant.now().toString());
        parentEvent.put("actions", historyActions);
        return java.util.Collections.singletonList(parentEvent);
    }

    private List<Map<String, Object>> buildTicketBookActionsFromHistory(
            com.kuleanasystems.assets.entity.TicketBook asset,
            List<com.kuleanasystems.assets.entity.TicketBookHistory> histories) {
        List<Map<String, Object>> actions = new java.util.ArrayList<>();
        if (histories == null || histories.isEmpty()) {
            Map<String, Object> creationAction = new java.util.HashMap<>();
            creationAction.put("id", UUID.randomUUID().toString());
            creationAction.put("flowName", "Asset-Action");
            creationAction.put("status", "Created");
            creationAction.put("createdAt", asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : java.time.Instant.now().toString());
            creationAction.put("createdBy", asset.getCreatedBy() != null ? asset.getCreatedBy() : "System");
            creationAction.put("startNum", asset.getStartNum());
            creationAction.put("endNum", asset.getEndNum());
            creationAction.put("nextTicket", asset.getNextTicket() != null ? asset.getNextTicket() : asset.getStartNum());

            int totalCount = (asset.getEndNum() != null && asset.getStartNum() != null)
                    ? Math.max(0, asset.getEndNum() - asset.getStartNum() + 1)
                    : 0;
            Map<String, Object> step = new java.util.HashMap<>();
            step.put("stepName", "CREATION");
            step.put("status", "Created");
            step.put("message", "Ticket book created with tickets #" + asset.getStartNum() + " through #" + asset.getEndNum() + " (" + totalCount + " tickets)");
            step.put("count", totalCount);
            step.put("createdBy", asset.getCreatedBy() != null ? asset.getCreatedBy() : "System");
            step.put("createdAt", asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : java.time.Instant.now().toString());
            creationAction.put("steps", java.util.List.of(step));
            actions.add(creationAction);
            return actions;
        }

        // Entry 0: Creation
        com.kuleanasystems.assets.entity.TicketBookHistory first = histories.get(0);
        Map<String, Object> creationAction = new java.util.HashMap<>();
        creationAction.put("id", first.getTicketBookHistoryId() != null ? first.getTicketBookHistoryId().toString() : UUID.randomUUID().toString());
        creationAction.put("flowName", "Asset-Action");
        creationAction.put("status", "Created");
        creationAction.put("createdAt", first.getCreatedAt() != null ? first.getCreatedAt().toString() : (asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : java.time.Instant.now().toString()));
        creationAction.put("createdBy", first.getCreatedBy() != null ? first.getCreatedBy() : (asset.getCreatedBy() != null ? asset.getCreatedBy() : "System"));
        creationAction.put("startNum", first.getStartNum() != null ? first.getStartNum() : asset.getStartNum());
        creationAction.put("endNum", first.getEndNum() != null ? first.getEndNum() : asset.getEndNum());
        creationAction.put("nextTicket", first.getNextTicket() != null ? first.getNextTicket() : (asset.getNextTicket() != null ? asset.getNextTicket() : first.getStartNum()));

        int initialStart = first.getStartNum() != null ? first.getStartNum() : (asset.getStartNum() != null ? asset.getStartNum() : 0);
        int initialEnd = first.getEndNum() != null ? first.getEndNum() : (asset.getEndNum() != null ? asset.getEndNum() : 0);
        int totalCount = Math.max(0, initialEnd - initialStart + 1);

        Map<String, Object> createStep = new java.util.HashMap<>();
        createStep.put("stepName", "CREATION");
        createStep.put("status", "Created");
        createStep.put("message", "Ticket book created with tickets #" + initialStart + " through #" + initialEnd + " (" + totalCount + " tickets)");
        createStep.put("count", totalCount);
        createStep.put("createdBy", first.getCreatedBy() != null ? first.getCreatedBy() : "System");
        createStep.put("createdAt", first.getCreatedAt() != null ? first.getCreatedAt().toString() : java.time.Instant.now().toString());
        creationAction.put("steps", java.util.List.of(createStep));
        actions.add(creationAction);

        for (int i = 1; i < histories.size(); i++) {
            com.kuleanasystems.assets.entity.TicketBookHistory prev = histories.get(i - 1);
            com.kuleanasystems.assets.entity.TicketBookHistory curr = histories.get(i);

            String status = curr.getStatus() != null ? curr.getStatus() : "Updated";
            String prevStatus = prev.getStatus();
            String gateName = curr.getGate() != null ? curr.getGate().getName() : (prev.getGate() != null ? prev.getGate().getName() : null);
            String shiftName = curr.getShift() != null ? curr.getShift().getName() : (prev.getShift() != null ? prev.getShift().getName() : null);
            String stepName = "ASSIGNMENT";
            String message = "";
            String actionStatus = status;

            int startNum = curr.getStartNum() != null ? curr.getStartNum() : (prev.getStartNum() != null ? prev.getStartNum() : 0);
            int endNum = curr.getEndNum() != null ? curr.getEndNum() : (prev.getEndNum() != null ? prev.getEndNum() : 0);
            Integer nextTicket = curr.getNextTicket() != null ? curr.getNextTicket() : prev.getNextTicket();
            int soldCount = (nextTicket != null && nextTicket > startNum) ? (nextTicket - startNum) : 0;

            if ("Assigned".equalsIgnoreCase(status)) {
                actionStatus = "Assigned";
                stepName = "ASSIGNMENT";
                message = "Ticket book assigned" + (gateName != null ? " to " + gateName : "") + (shiftName != null ? " for shift " + shiftName : "");
            } else if (("Available".equalsIgnoreCase(status) || "Unassigned".equalsIgnoreCase(status)) && "Assigned".equalsIgnoreCase(prevStatus)) {
                actionStatus = "Unassigned";
                stepName = "ASSIGNMENT";
                message = "Ticket book unassigned" + (gateName != null ? " from " + gateName : "") + " and returned to available inventory";
            } else if ("Returned Unused".equalsIgnoreCase(status) || ("Returned".equalsIgnoreCase(status) && soldCount == 0)) {
                actionStatus = "Returned Unused";
                stepName = "RETURN";
                message = "Ticket book returned unused with 0 tickets sold";
            } else if ("Split".equalsIgnoreCase(status) || (prev.getEndNum() != null && curr.getEndNum() != null && !curr.getEndNum().equals(prev.getEndNum()))) {
                actionStatus = "Split";
                stepName = "SPLIT";
                int sold = (curr.getNextTicket() != null && curr.getStartNum() != null) ? Math.max(0, curr.getNextTicket() - curr.getStartNum()) : 0;
                message = "Ticket book split after selling " + sold + " ticket" + (sold == 1 ? "" : "s") + " (tickets #" + startNum + " to #" + (curr.getNextTicket() != null ? curr.getNextTicket() - 1 : endNum) + "). Remaining tickets #" + (curr.getNextTicket() != null ? curr.getNextTicket() : startNum) + " to #" + endNum + " split to new ticket book";
            } else if ("Returned".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status) || "Reconciled".equalsIgnoreCase(status)) {
                actionStatus = "Completed";
                stepName = "RETURN";
                int lastSold = (nextTicket != null && nextTicket > startNum) ? (nextTicket - 1) : 0;
                message = "Ticket book returned and reconciled at shift close. Tickets sold: " + soldCount + (lastSold > 0 ? " (last sold: #" + lastSold + ")" : "");
            } else {
                actionStatus = "Updated";
                stepName = "EDIT";
                message = "Ticket book details updated";
            }

            Map<String, Object> action = new java.util.HashMap<>();
            action.put("id", curr.getTicketBookHistoryId() != null ? curr.getTicketBookHistoryId().toString() : UUID.randomUUID().toString());
            action.put("flowName", "Asset-Action");
            action.put("status", actionStatus);
            action.put("createdAt", curr.getCreatedAt() != null ? curr.getCreatedAt().toString() : java.time.Instant.now().toString());
            action.put("createdBy", curr.getCreatedBy() != null ? curr.getCreatedBy() : "System");
            action.put("startNum", startNum);
            action.put("endNum", endNum);
            action.put("nextTicket", nextTicket);
            if (gateName != null) action.put("gateName", gateName);
            if (shiftName != null) action.put("shiftName", shiftName);

            Map<String, Object> step = new java.util.HashMap<>();
            step.put("stepName", stepName);
            step.put("status", actionStatus);
            step.put("message", message);
            step.put("count", soldCount);
            step.put("createdBy", curr.getCreatedBy() != null ? curr.getCreatedBy() : "System");
            step.put("createdAt", curr.getCreatedAt() != null ? curr.getCreatedAt().toString() : java.time.Instant.now().toString());
            action.put("steps", java.util.List.of(step));
            actions.add(action);
        }

        return actions;
    }

    private List<Map<String, Object>> mergeTicketBookActions(
            List<Object> trackerActions,
            List<Map<String, Object>> historyActions,
            Map<UUID, com.kuleanasystems.assets.entity.TicketBookHistory> historyByProcessId,
            Map<String, com.kuleanasystems.assets.entity.TicketBookHistory> historyByStatus,
            com.kuleanasystems.assets.entity.TicketBook asset) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Set<String> presentStatuses = new java.util.HashSet<>();

        for (Object act : trackerActions) {
            if (act instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> actionMap = new java.util.HashMap<>((Map<String, Object>) act);
                enrichTicketBookEventMap(actionMap, historyByProcessId, historyByStatus, asset);
                String status = (String) actionMap.get("status");
                if (status != null) {
                    presentStatuses.add(status.toLowerCase());
                }
                result.add(actionMap);
            }
        }

        // Merge any missing history actions (such as Created, Unassigned, Returned Unused, Split, Completed)
        for (Map<String, Object> hAction : historyActions) {
            String status = (String) hAction.get("status");
            if (status != null) {
                String normalizedStatus = status.toLowerCase();
                if (normalizedStatus.contains("created") && !presentStatuses.contains("created")) {
                    result.add(hAction);
                    presentStatuses.add("created");
                } else if (normalizedStatus.contains("unassigned") && !presentStatuses.contains("unassigned")) {
                    result.add(hAction);
                    presentStatuses.add("unassigned");
                } else if (normalizedStatus.contains("returned unused") && !presentStatuses.contains("returned unused")) {
                    result.add(hAction);
                    presentStatuses.add("returned unused");
                } else if (normalizedStatus.contains("split") && !presentStatuses.contains("split")) {
                    result.add(hAction);
                    presentStatuses.add("split");
                } else if (!presentStatuses.contains(normalizedStatus)) {
                    result.add(hAction);
                    presentStatuses.add(normalizedStatus);
                }
            }
        }

        // Sort chronologically by createdAt
        result.sort((a, b) -> {
            String tA = (String) a.get("createdAt");
            String tB = (String) b.get("createdAt");
            if (tA == null && tB == null) return 0;
            if (tA == null) return -1;
            if (tB == null) return 1;
            return tA.compareTo(tB);
        });

        return result;
    }

    private List<Object> createSyntheticTicketBookTimeline(
            Map<String, Object> discoveredData,
            com.kuleanasystems.assets.entity.TicketBook asset,
            List<Map<String, Object>> historyActions) {
        Map<String, Object> parentEvent = new java.util.HashMap<>(discoveredData);
        parentEvent.put("id", UUID.randomUUID().toString());
        parentEvent.put("flowName", "Asset-Lifecycle");
        parentEvent.put("status", asset.getStatus() != null ? asset.getStatus().toUpperCase() : "ACTIVE");
        parentEvent.put("createdAt", asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : java.time.Instant.now().toString());
        parentEvent.put("actions", historyActions);
        return java.util.Collections.singletonList(parentEvent);
    }
}
