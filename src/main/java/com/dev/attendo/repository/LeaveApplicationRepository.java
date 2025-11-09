package com.dev.attendo.repository;

import com.dev.attendo.model.LeaveApplication;
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
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    @Query("SELECT l FROM LeaveApplication l JOIN l.user u JOIN u.role r JOIN u.store s " +
            "WHERE s.id = :storeId " +
            "AND l.status = :status " +
            "AND (:type IS NULL OR l.type = :type) " +
            "AND LOWER(u.username) LIKE LOWER(:keyword) " +
            "AND ((:currentRole = 'ROLE_OWNER') OR (:currentRole = 'ROLE_ADMIN' AND r.name = 'ROLE_EMPLOYEE')) " +
            "AND (:selectedStartDate IS NULL OR l.endDate >= :selectedStartDate) " +
            "AND (:selectedEndDate IS NULL OR l.startDate <= :selectedEndDate)")
    Page<LeaveApplication> findByCurrentUserRoleAndStoreAndNameAndStatusAndType(String currentRole, Long storeId, String keyword, ApprovalStatusEnum status, LeaveTypeEnum type, LocalDate selectedStartDate, LocalDate selectedEndDate, Pageable pageDetails);

    @Query("SELECT l FROM LeaveApplication l JOIN l.user u JOIN u.store s " +
            "WHERE u.id = :userId " +
            "AND s.id = :storeId " +
            "AND l.status = :status " +
            "AND (:type IS NULL OR l.type = :type) " +
            "AND (:selectedStartDate IS NULL OR l.endDate >= :selectedStartDate) " +
            "AND (:selectedEndDate IS NULL OR l.startDate <= :selectedEndDate)")
    Page<LeaveApplication> findByUserIdAndStoreAndNameAndStatusAndType(Long userId, Long storeId, LocalDate selectedStartDate, LocalDate selectedEndDate, ApprovalStatusEnum status, LeaveTypeEnum type, Pageable pageDetails);

    @Query("""
                SELECT 
                    YEAR(l.startDate) AS year,
                    MONTH(l.startDate) AS month,
                    l.type AS leaveType,
                    COUNT(l.id) AS totalLeaveRequests,
                    COALESCE(SUM(DATEDIFF(l.endDate, l.startDate) + 1), 0) AS totalLeaveDays
                FROM LeaveApplication l
                WHERE l.store.id = :storeId
                AND l.status = 'APPROVED'
                AND DATE(l.updatedDate) BETWEEN :startDate AND :endDate
                GROUP BY year, month, leaveType
                ORDER BY year, month
            """)
    List<Object[]> getLeaveOvertimeSummary(Long storeId, LocalDate startDate, LocalDate endDate);
}
