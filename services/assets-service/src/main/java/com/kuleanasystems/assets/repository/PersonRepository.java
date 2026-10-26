package com.kuleanasystems.assets.repository;

import com.kuleanasystems.assets.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends JpaRepository<Person, UUID>, JpaSpecificationExecutor<Person> {
    List<Person> findByShift_ShiftId(UUID shiftId);
    List<Person> findByShift_ShiftIdAndIsActiveTrue(UUID shiftId);
    List<Person> findByShift_ShiftIdAndGate_GateId(UUID shiftId, UUID gateId);
    Optional<Person> findFirstByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndShift_ShiftId(String firstName, String lastName, UUID shiftId);
}
