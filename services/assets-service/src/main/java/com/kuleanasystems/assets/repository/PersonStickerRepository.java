package com.kuleanasystems.assets.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import org.springframework.lang.Nullable;

import com.kuleanasystems.assets.entity.PersonSticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PersonStickerRepository extends JpaRepository<PersonSticker, UUID>, JpaSpecificationExecutor<PersonSticker> {

    @Override
    @EntityGraph(attributePaths = {"person", "shift", "gate"})
    List<PersonSticker> findAll(@Nullable Specification<PersonSticker> spec);

    PersonSticker findFirstByPerson_PersonIdAndShift_ShiftId(UUID personId, UUID shiftId);

    @EntityGraph(attributePaths = {"person", "shift", "gate"})
    List<PersonSticker> findByShift_ShiftId(UUID shiftId);

    boolean existsByPerson_PersonIdAndShift_ShiftIdAndPersonStickerIdNot(UUID personId, UUID shiftId, UUID personStickerId);
}