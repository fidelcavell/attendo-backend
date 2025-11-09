package com.dev.attendo.service.impl;

import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.Store;
import com.dev.attendo.repository.*;
import com.dev.attendo.service.ReportService;
import com.dev.attendo.utils.enums.LeaveTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    OvertimeApplicationRepository overtimeApplicationRepository;

    @Override
    public Map<String, Object> getExpensesReport(Long storeId, int period) {
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = currentDate.minusMonths(period - 1).withDayOfMonth(1);

        // Fetch data from database
        List<Object[]> result = attendanceRepository.getMonthlyAttendanceCountSummary(selectedStore.getId(), startDate, currentDate);

        // Map and merge value per month-year
        Map<YearMonth, Integer> monthlyTotals = new HashMap<>();
        for (Object[] row : result) {
            int year = ((Number) row[1]).intValue();
            int month = ((Number) row[2]).intValue();
            int totalAttendance = row[3] != null ? ((Number) row[3]).intValue() : 0;
            int totalDeduction = row[4] != null ? ((Number) row[4]).intValue() : 0;
            int overtimePay = row[5] != null ? ((Number) row[5]).intValue() : 0;
            int totalOvertime = row[6] != null ? ((Number) row[6]).intValue() : 0;
            int lastSalary = row[7] != null ? ((Number) row[7]).intValue() : 0;

            int monthlyExpenses = (totalAttendance * lastSalary) + (totalOvertime * overtimePay) - totalDeduction;
            YearMonth ym = YearMonth.of(year, month);
            monthlyTotals.merge(ym, monthlyExpenses, Integer::sum);
        }

        // Ensure all month is exists (if not, the data get filled by 0)
        LocalDate tempDate = startDate;
        while (!tempDate.isAfter(currentDate)) {
            monthlyTotals.putIfAbsent(YearMonth.of(tempDate.getYear(), tempDate.getMonthValue()), 0);
            tempDate = tempDate.plusMonths(1);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

        // Sort data by month and add additional key to support bar chart component.
        List<Map<String, Object>> sortedMonthlyTotals = monthlyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> obj = new LinkedHashMap<>();
                    obj.put("month", entry.getKey().format(formatter));
                    obj.put("amount", entry.getValue());
                    return obj;
                })
                .collect(Collectors.toList());

        // Count the total of all expenses during the period
        int totalExpenses = monthlyTotals.values().stream().mapToInt(Integer::intValue).sum();

        // Count average expenses during the period
        int averageExpenses = totalExpenses / period;

        // Get the highest expenses
        Optional<Map.Entry<YearMonth, Integer>> highestExpense = monthlyTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        // Get the lowest expenses
        Optional<Map.Entry<YearMonth, Integer>> lowestExpense = monthlyTotals.entrySet().stream()
                .min(Map.Entry.comparingByValue());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("monthlyTotals", sortedMonthlyTotals);
        summary.put("totalExpenses", totalExpenses);
        summary.put("averageExpenses", averageExpenses);
        summary.put("highestMonth", highestExpense.map(item -> Map.of(
                "month", item.getKey().format(formatter),
                "amount", item.getValue()
        )).orElse(null));
        summary.put("lowestMonth", lowestExpense.map(item -> Map.of(
                "month", item.getKey().format(formatter),
                "amount", item.getValue()
        )).orElse(null));

        return summary;
    }

    @Override
    public Map<String, Object> getFrequentlyLateReport(Long storeId, int period) {
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = currentDate.minusMonths(period - 1).withDayOfMonth(1);

        List<Object[]> result = attendanceRepository.getLateEmployeesCountSummary(selectedStore.getId(), startDate, currentDate);

        List<Map<String, Object>> employeeStats = new ArrayList<>();
        Map<String, Long> monthlyLateMapping = new LinkedHashMap<>();
        int totalLateCount = 0;
        int totalLateMinutes = 0;

        for (Object[] row : result) {
            Long userId = ((Number) row[0]).longValue();
            int year = ((Number) row[1]).intValue();
            int month = ((Number) row[2]).intValue();
            String username = (String) row[3];
            long lateCount = ((Number) row[4]).longValue();
            long lateInMinutes = ((Number) row[5]).longValue();

            totalLateCount += (int) lateCount;
            totalLateMinutes += (int) lateInMinutes;

            Map<String, Object> employee = new LinkedHashMap<>();
            employee.put("userId", userId);
            employee.put("username", username);
            employee.put("lateCount", lateCount);
            employee.put("lateInMinutes", lateInMinutes);
            employeeStats.add(employee);

            YearMonth ym = YearMonth.of(year, month);
            String monthLabel = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
            monthlyLateMapping.merge(monthLabel, lateCount, Long::sum);
        }

        List<Map<String, Object>> top5 = employeeStats.stream()
                .sorted((a, b) -> Long.compare(
                        (Long) b.get("lateCount"),
                        (Long) a.get("lateCount")
                ))
                .limit(5)
                .toList();

        LocalDate tempDate = startDate;
        while (!tempDate.isAfter(currentDate)) {
            String label = YearMonth.of(tempDate.getYear(), tempDate.getMonthValue()).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
            monthlyLateMapping.putIfAbsent(label, 0L);
            tempDate = tempDate.plusMonths(1);
        }

        List<Map<String, Object>> monthlyLateDistribution = monthlyLateMapping.entrySet().stream()
                .sorted(Comparator.comparing(entry ->
                        YearMonth.parse(entry.getKey(), DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
                ))
                .map(entry -> {
                    Map<String, Object> obj = new LinkedHashMap<>();
                    obj.put("month", entry.getKey());
                    obj.put("count", entry.getValue());
                    return obj;
                })
                .toList();

        // prepare summary
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalEmployeesLate", employeeStats.size());
        summary.put("totalLateCount", totalLateCount);
        summary.put("totalLateMinutes", totalLateMinutes);
        summary.put("totalLateHours", BigDecimal.valueOf(totalLateMinutes / 60.0)
                .setScale(1, RoundingMode.HALF_UP));
        summary.put("top5LateEmployees", top5);
        summary.put("monthlyLateDistribution", monthlyLateDistribution);

        return summary;
    }

    @Override
    public Map<String, Object> getLeaveVsOvertimeReport(Long storeId, int period) {
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id: " + storeId + " is not found!"));

        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = currentDate.minusMonths(period - 1).withDayOfMonth(1);

        List<Object[]> resultLeaveApplication = leaveApplicationRepository.getLeaveOvertimeSummary(
                selectedStore.getId(), startDate, currentDate);

        int totalLeaveDaysInPeriod = 0;
        int totalLeaveRequestInPeriod = 0;
        Map<String, Map<String, Integer>> leaveTotals = new HashMap<>();
        Map<String, Map<String, Map<String, Integer>>> leaveTotalsByMonth = new LinkedHashMap<>();

        // 🔹 Hitung total cuti & per bulan
        for (Object[] row : resultLeaveApplication) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            LeaveTypeEnum leaveTypeEnum = (LeaveTypeEnum) row[2];
            int totalLeaveRequest = row[3] != null ? ((Number) row[3]).intValue() : 0;
            int totalLeaveDays = row[4] != null ? ((Number) row[4]).intValue() : 0;

            String leaveType = leaveTypeEnum.name();
            totalLeaveDaysInPeriod += totalLeaveDays;
            totalLeaveRequestInPeriod += totalLeaveRequest;

            // Total per jenis
            leaveTotals.computeIfAbsent(leaveType, key -> new HashMap<>());
            leaveTotals.get(leaveType).merge("totalRequests", totalLeaveRequest, Integer::sum);
            leaveTotals.get(leaveType).merge("leaveDays", totalLeaveDays, Integer::sum);

            // Total per bulan
            String yearMonth = YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));

            leaveTotalsByMonth.computeIfAbsent(yearMonth, k -> new HashMap<>());
            leaveTotalsByMonth.get(yearMonth).computeIfAbsent(leaveType, k -> new HashMap<>());

            Map<String, Integer> leaveData = leaveTotalsByMonth.get(yearMonth).get(leaveType);
            leaveData.merge("totalRequests", totalLeaveRequest, Integer::sum);
            leaveData.merge("leaveDays", totalLeaveDays, Integer::sum);
        }

        // 🔹 Pastikan semua bulan & tipe cuti ada (meski 0)
        LocalDate tempDate = startDate;
        while (!tempDate.isAfter(currentDate)) {
            String key = YearMonth.of(tempDate.getYear(), tempDate.getMonthValue())
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));

            leaveTotalsByMonth.computeIfAbsent(key, k -> new HashMap<>());
            for (LeaveTypeEnum type : LeaveTypeEnum.values()) {
                Map<String, Map<String, Integer>> monthData = leaveTotalsByMonth.get(key);
                monthData.computeIfAbsent(type.name(), k -> new HashMap<>());
                Map<String, Integer> typeData = monthData.get(type.name());
                typeData.putIfAbsent("totalRequests", 0);
                typeData.putIfAbsent("leaveDays", 0);
            }
            tempDate = tempDate.plusMonths(1);
        }

        List<Map<String, Object>> leaveDistributionByMonth = leaveTotalsByMonth.entrySet().stream()
                .sorted(Comparator.comparing(entry -> {
                    String key = entry.getKey(); // contoh: "March 2025"
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
                    return YearMonth.parse(key, formatter);
                }))
                .map(entry -> {
                    String month = entry.getKey();
                    Map<String, Map<String, Integer>> monthData = entry.getValue();

                    Map<String, Object> monthObject = new LinkedHashMap<>();
                    monthObject.put("month", month);

                    monthData.forEach((type, data) -> {
                        int leaveDays = data.getOrDefault("leaveDays", 0);
                        monthObject.put(type, leaveDays);
                    });

                    return monthObject;
                })
                .toList();

        // 🔹 Buat ringkasan
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalLeaveDaysInPeriod", totalLeaveDaysInPeriod);
        summary.put("totalLeaveRequestInPeriod", totalLeaveRequestInPeriod);
        summary.put("leaveDistributionByMonth", leaveDistributionByMonth);

        List<Map<String, Object>> pieDistribution = new ArrayList<>();

        for (LeaveTypeEnum type : LeaveTypeEnum.values()) {
            String key = type.name();
            Map<String, Integer> stats = leaveTotals.getOrDefault(key, Map.of("totalRequests", 0, "leaveDays", 0));

            double percentage = 0.0;
            if (totalLeaveDaysInPeriod > 0) {
                percentage = (stats.get("leaveDays") * 100.0) / totalLeaveDaysInPeriod;
            }

            // Pie Chart Distribution
            Map<String, Object> pieItem = new LinkedHashMap<>();
            String label = switch (key) {
                case "SICK" -> "sick";
                case "PERSONAL" -> "personal";
                case "OTHER" -> "other";
                default -> key;
            };

            pieItem.put("type", label);
            pieItem.put("leaveDays", stats.get("leaveDays"));
            pieItem.put("totalRequests", stats.get("totalRequests"));
            pieItem.put("percentage", Math.round(percentage * 100.0) / 100.0);

            pieDistribution.add(pieItem);
        }
        summary.put("pieDistribution", pieDistribution);

        // 🔹 Bagian lembur (overtime)
        List<Object[]> resultOvertimeApplication = overtimeApplicationRepository.getOvertimeCountSummary(
                selectedStore.getId(), startDate, currentDate);

        Map<String, Integer> overtimeTotalsByMonth = new LinkedHashMap<>();
        int totalOvertimeDaysInPeriod = 0;

        for (Object[] row : resultOvertimeApplication) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            int totalOvertimeDays = row[2] != null ? ((Number) row[2]).intValue() : 0;

            totalOvertimeDaysInPeriod += totalOvertimeDays;

            String key = YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
            overtimeTotalsByMonth.merge(key, totalOvertimeDays, Integer::sum);
        }

        // Pastikan setiap bulan ada
        tempDate = startDate;
        while (!tempDate.isAfter(currentDate)) {
            String key = YearMonth.of(tempDate.getYear(), tempDate.getMonthValue())
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
            overtimeTotalsByMonth.putIfAbsent(key, 0);
            tempDate = tempDate.plusMonths(1);
        }

        List<Map<String, Object>> overtimeDaysDistributionByMonth = overtimeTotalsByMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("month", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        summary.put("totalOvertimeDaysInPeriod", totalOvertimeDaysInPeriod);
        summary.put("overtimeDaysDistributionByMonth", overtimeDaysDistributionByMonth);

        return summary;
    }
}
