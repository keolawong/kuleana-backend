package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.TicketBook;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketBookSpecification {

    public static Specification<TicketBook> search(
            UUID ticketBookId, String scanCode, Integer startNum, Integer endNum,
            Integer nextTicket, Boolean isReusable, String status, UUID operationId,
            UUID shiftId, UUID gateId, String deviceId, UUID parentTicketBookId,
            UUID processId, UUID personStickerId, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ticketBookId != null) {
                predicates.add(cb.equal(root.get("ticketBookId"), ticketBookId));
            }
            if (scanCode != null && !scanCode.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("scanCode"), scanCode));
            }
            if (startNum != null) {
                predicates.add(cb.equal(root.get("startNum"), startNum));
            }
            if (endNum != null) {
                predicates.add(cb.equal(root.get("endNum"), endNum));
            }
            if (nextTicket != null) {
                predicates.add(cb.equal(root.get("nextTicket"), nextTicket));
            }
            if (isReusable != null) {
                predicates.add(cb.equal(root.get("isReusable"), isReusable));
            }
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (operationId != null) {
                predicates.add(cb.equal(root.join("operation").get("operationId"), operationId));
            }
            if (shiftId != null) {
                predicates.add(cb.equal(root.join("shift").get("shiftId"), shiftId));
            }
            if (gateId != null) {
                predicates.add(cb.equal(root.join("gate").get("gateId"), gateId));
            }
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("deviceId"), deviceId));
            }
            if (parentTicketBookId != null) {
                predicates.add(cb.equal(root.get("parentTicketBookId"), parentTicketBookId));
            }
            if (processId != null) {
                predicates.add(cb.equal(root.get("processId"), processId));
            }
            if (personStickerId != null) {
                predicates.add(cb.equal(root.join("person").get("personId"), personStickerId));
            }
            if (createdBy != null && !createdBy.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            if (createdAt != null) {
                predicates.add(cb.equal(root.get("createdAt"), createdAt));
            }
            if (updatedBy != null && !updatedBy.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("updatedBy"), updatedBy));
            }
            if (updatedAt != null) {
                predicates.add(cb.equal(root.get("updatedAt"), updatedAt));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}