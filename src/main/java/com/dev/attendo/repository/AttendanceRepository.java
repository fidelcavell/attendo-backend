package com.dev.attendo.repository;

import com.dev.attendo.model.Attendance;
import com.dev.attendo.utils.enums.AttendanceTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("SELECT a FROM Attendance a JOIN a.user u " +
            "WHERE YEAR(a.clockIn) = :year " +
            "AND MONTH(a.clockIn) = :month " +
            "AND u.id = :userId " +
            "ORDER BY a.clockIn ASC")
    List<Attendance> findByYearAndMonth(Long userId, int month, int year);

    @Query("SELECT a FROM Attendance a JOIN a.user u " +
            "WHERE a.store.id = :storeId " +
            "AND (:type IS NULL OR a.type = :type) " +
            "AND LOWER(u.username) LIKE LOWER(:keyword) " +
            "AND DATE(a.clockIn) = CURRENT_DATE")
    Page<Attendance> getTodayAttendances(Long storeId, AttendanceTypeEnum type, String keyword, Pageable pageDetails);

    @Query("SELECT SUM(a.deductionAmount) FROM Attendance a " +
            "WHERE YEAR(a.clockIn) = :year " +
            "AND MONTH(a.clockIn) = :month " +
            "AND a.user.id = :userId")
    Optional<Integer> getTotalDeductionByYearAndMonth(Long userId, int month, int year);

    @Query("SELECT COUNT(a) FROM Attendance a " +
            "WHERE YEAR(a.clockIn) = :year " +
            "AND MONTH(a.clockIn) = :month " +
            "AND a.user.id = :userId " +
            "AND a.type = 'DAILY' " +
            "AND a.status IN ('PRESENT', 'LATE')")
    Optional<Integer> countOnTimeAndLateByYearAndMonth(Long userId, Integer month, int year);

    @Query("SELECT a from Attendance a " +
            "WHERE a.user.id = :userId " +
            "AND a.type IN ('DAILY', 'LEAVE') " +
            "AND DATE(a.clockIn) = :currentDate"
    )
    Optional<Attendance> findByUserId(Long userId, LocalDate currentDate);

    @Query("SELECT SUM(o.overtimePay) FROM Attendance a JOIN a.overtimeApplication o " +
            "WHERE YEAR(a.clockIn) = :year " +
            "AND MONTH(a.clockIn) = :month " +
            "AND a.type = 'OVERTIME' " +
            "AND a.status IN ('PRESENT', 'LATE') " +
            "AND a.user.id = :userId")
    Optional<Integer> getTotalOvertimePayByYearAndMonth(Long userId, int month, int year);

    @Query("SELECT COUNT(a) FROM Attendance a " +
            "WHERE YEAR(a.clockIn) = :year " +
            "AND MONTH(a.clockIn) = :month " +
            "AND a.user.id = :userId " +
            "AND a.type = 'OVERTIME' " +
            "AND a.status IN ('PRESENT', 'LATE')")
    Optional<Integer> countOvertimeByYearAndMonth(Long userId, Integer month, int year);

    @Query("SELECT a from Attendance a JOIN a.overtimeApplication o " +
            "WHERE a.user.id = :userId " +
            "AND a.type = 'OVERTIME' " +
            "AND DATE(o.overtimeDate) = :currentDate " +
            "ORDER BY o.overtimeDate DESC LIMIT 1"
    )
    Optional<Attendance> findOvertimeAttendanceByUserId(Long userId, LocalDate currentDate);


    // EXPENSES REPORT QUERY
    @Query("""
            SELECT 
                a.user.id AS userId,
                YEAR(a.clockIn) AS year,
                MONTH(a.clockIn) AS month,
                COUNT(CASE WHEN a.type = 'DAILY' AND a.status IN ('PRESENT', 'LATE') THEN 1 END) AS totalAttendance,
                SUM(a.deductionAmount) AS totalDeduction,
                SUM(CASE WHEN o.id IS NOT NULL AND a.status IN ('PRESENT', 'LATE') THEN o.overtimePay ELSE 0 END) AS overtimePay,
                COUNT(CASE WHEN o.id IS NOT NULL AND a.status IN ('PRESENT', 'LATE') THEN 1 END) AS totalOvertime,
                (
                        SELECT s.amount
                        FROM Salary s
                        WHERE s.user.id = a.user.id
                          AND s.effectiveDate <= LAST_DAY(a.clockIn)
                        ORDER BY s.createdDate DESC
                        LIMIT 1
                    ) AS lastSalary
            FROM Attendance a
            LEFT JOIN a.overtimeApplication o
            WHERE a.store.id = :storeId
            AND DATE(a.clockIn) BETWEEN :startDate AND :endDate
            GROUP BY userId, year, month
            """)
    List<Object[]> getMonthlyAttendanceCountSummary(Long storeId, LocalDate startDate, LocalDate endDate);


    // FREQUENTLY LATE EMPLOYEE QUERY
    @Query("SELECT a.user.id AS userId, YEAR(a.clockIn) AS year, MONTH(a.clockIn) AS month, a.user.username AS username, COUNT(a.id) AS lateCount, SUM(a.lateInMinutes) AS lateMinutes FROM Attendance a " +
            "WHERE a.status = 'LATE' " +
            "AND a.store.id = :storeId " +
            "AND DATE(a.clockIn) BETWEEN :startDate AND :endDate " +
            "GROUP BY a.user.username, year, month " +
            "ORDER BY lateCount DESC")
    List<Object[]> getLateEmployeesCountSummary(Long storeId, LocalDate startDate, LocalDate endDate);
}
