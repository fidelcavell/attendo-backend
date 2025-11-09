package com.dev.attendo.service;

import java.util.Map;

public interface ReportService {

    Map<String, Object> getExpensesReport(Long storeId, int period);

    Map<String, Object> getFrequentlyLateReport(Long storeId, int period);

    Map<String, Object> getLeaveVsOvertimeReport(Long storeId, int period);
}
