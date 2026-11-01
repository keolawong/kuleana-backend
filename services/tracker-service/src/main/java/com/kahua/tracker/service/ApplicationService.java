package com.kahua.tracker.service;

import com.kahua.tracker.dto.application.ApplicationUpdateRequest;
import com.kahua.tracker.entity.Application;
import com.kahua.tracker.repository.ApplicationRepository;
import com.kahua.tracker.spec.ApplicationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<Application> searchApplications(String name, UUID writeRoleId, UUID readRoleId, Boolean isActive) {
        Specification<Application> spec = ApplicationSpecification.findByCriteria(name, writeRoleId, readRoleId, isActive);
        return applicationRepository.findAll(spec);
    }

    @Transactional
    public Optional<Application> updateApplication(UUID id, ApplicationUpdateRequest request) {
        Optional<Application> existingApplication = applicationRepository.findById(id);
        if (existingApplication.isEmpty()) {
            return Optional.empty();
        }

        Application applicationToUpdate = existingApplication.get();
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        request.getName().ifPresent(applicationToUpdate::setName);
        request.getWriteRoleId().ifPresent(applicationToUpdate::setWriteRoleId);
        request.getReadRoleId().ifPresent(applicationToUpdate::setReadRoleId);
        request.getIsActive().ifPresent(applicationToUpdate::setIsActive);
        applicationToUpdate.setUpdatedBy(currentUser);

        return Optional.of(applicationRepository.save(applicationToUpdate));
    }

    @Transactional
    public Application save(Application application) {
        return applicationRepository.save(application);
    }
}
