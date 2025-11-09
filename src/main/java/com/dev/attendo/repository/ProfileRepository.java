package com.dev.attendo.repository;

import com.dev.attendo.model.Profile;
import com.dev.attendo.utils.enums.RoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Profile findByName(String name);

    @Query("SELECT p FROM Profile p JOIN p.user u JOIN u.role r " +
            "WHERE u.store.id = :storeId " +
            "AND ((:currentRole = 'ROLE_OWNER' AND r.name IN ('ROLE_ADMIN', 'ROLE_EMPLOYEE')) OR (:currentRole = 'ROLE_ADMIN' AND r.name = 'ROLE_EMPLOYEE')) " +
            "AND LOWER(p.name) LIKE LOWER(:keyword)")
    Page<Profile> getAllAssociateProfile(String currentRole, Long storeId, String keyword, Pageable pageDetails);

    @Query("SELECT p FROM Profile p JOIN p.user u JOIN p.schedule s " +
            "WHERE u.store.id = :storeId " +
            "AND s.id = :scheduleId " +
            "AND u.isActive = :isActive")
    List<Profile> findAllProfileByStoreAndScheduleAndIsActive(Long storeId, Long scheduleId, boolean isActive);
}
