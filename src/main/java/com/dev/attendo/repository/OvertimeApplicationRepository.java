package com.dev.attendo.repository;

import com.dev.attendo.model.LeaveApplication;
import com.dev.attendo.model.OvertimeApplication;
import com.dev.attendo.utils.enums.ApprovalStatusEnum;
import com.dev.attendo.utils.enums.LeaveTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OvertimeApplicationRepository extends JpaRepository<OvertimeApplication, Long> {

    @Query("SELECT o FROM OvertimeApplication o JOIN o.user u JOIN u.role r JOIN u.store s " +
            "WHERE s.id = :storeId " +
            "AND o.status = :status " +
            "AND LOWER(u.username) LIKE LOWER(:keyword)" +
            "AND ((:currentRole = 'ROLE_OWNER') OR (:currentRole = 'ROLE_ADMIN' AND r.name = 'ROLE_EMPLOYEE')) " +
            "AND (:selectedStartDate IS NULL OR o.overtimeDate >= :selectedStartDate) " +
            "AND (:selectedEndDate IS NULL OR o.overtimeDate <= :selectedEndDate)")
    Page<OvertimeApplication> findByCurrentUserRoleAndStoreAndNameAndStatus(Long storeId, String currentRole, String keyword, ApprovalStatusEnum status, LocalDate selectedStartDate, LocalDate selectedEndDate, Pageable pageDetails);

    @Query("SELECT o FROM OvertimeApplication o JOIN o.user u JOIN u.store s " +
            "WHERE u.id = :userId " +
            "AND s.id = :storeId " +
            "AND o.status = :status " +
            "AND (:selectedStartDate IS NULL OR o.overtimeDate >= :selectedStartDate) " +
            "AND (:selectedEndDate IS NULL OR o.overtimeDate <= :selectedEndDate)")
    Page<OvertimeApplication> findByUserIdAndStoreAndNameAndStatus(Long userId, Long storeId, LocalDate selectedStartDate, LocalDate selectedEndDate, ApprovalStatusEnum status, Pageable pageDetails);


    @Query("SELECT YEAR(o.overtimeDate) AS year, MONTH(o.overtimeDate) AS month, COUNT(o) AS totalOvertimeDays FROM OvertimeApplication o " +
            "WHERE o.user.store.id = :storeId " +
            "AND o.status = 'APPROVED' " +
            "AND o.overtimeDate BETWEEN :startDate AND :endDate " +
            "GROUP BY MONTH(o.overtimeDate)")
    List<Object[]> getOvertimeCountSummary(Long storeId, LocalDate startDate, LocalDate endDate);

}
