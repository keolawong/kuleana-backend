package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.CashBank;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CashBankSpecification {

    public static Specification<CashBank> search(
            UUID cashBankId, String bankName, BigDecimal startingAmount, BigDecimal officialCount,
            Integer count100s, Integer count50s, Integer count20s, Integer count10s,
            Integer count5s, Integer count1s, Integer count1cs, String status, UUID operationId,
            UUID shiftId, UUID gateId, String deviceId, UUID parentCashBankId,
            UUID processId, UUID personStickerId, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (cashBankId != null) {
                predicates.add(cb.equal(root.get("cashBankId"), cashBankId));
            }
            if (bankName != null && !bankName.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));
            }
            if (startingAmount != null) {
                predicates.add(cb.equal(root.get("startingAmount"), startingAmount));
            }
            if (officialCount != null) {
                predicates.add(cb.equal(root.get("officialCount"), officialCount));
            }
            if (count100s != null) {
                predicates.add(cb.equal(root.get("count100s"), count100s));
            }
            if (count50s != null) {
                predicates.add(cb.equal(root.get("count50s"), count50s));
            }
            if (count20s != null) {
                predicates.add(cb.equal(root.get("count20s"), count20s));
            }
            if (count10s != null) {
                predicates.add(cb.equal(root.get("count10s"), count10s));
            }
            if (count5s != null) {
                predicates.add(cb.equal(root.get("count5s"), count5s));
            }
            if (count1s != null) {
                predicates.add(cb.equal(root.get("count1s"), count1s));
            }
            if (count1cs != null) {
                predicates.add(cb.equal(root.get("count1cs"), count1cs));
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
            if (parentCashBankId != null) {
                predicates.add(cb.equal(root.get("parentCashBankId"), parentCashBankId));
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