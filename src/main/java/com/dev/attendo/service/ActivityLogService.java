package com.dev.attendo.service;

import com.dev.attendo.dtos.audit.ActivityLogPagination;
import com.dev.attendo.model.User;

import java.time.LocalDate;

public interface ActivityLogService {

    ActivityLogPagination getAllActivityLog(Long storeId, String keyword, String actionMethod, LocalDate startDate, LocalDate endDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    void addActivityLog(User user, String actionMethod, String actionName, String entity, String description);
}
