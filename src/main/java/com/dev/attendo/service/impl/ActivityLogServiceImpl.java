package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.audit.ActivityLogDTO;
import com.dev.attendo.dtos.audit.ActivityLogPagination;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.model.ActivityLog;
import com.dev.attendo.model.User;
import com.dev.attendo.repository.ActivityLogRepository;
import com.dev.attendo.service.ActivityLogService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    @Autowired
    ActivityLogRepository activityLogRepository;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public ActivityLogPagination getAllActivityLog(Long storeId, String keyword, String actionMethod, LocalDate startDate, LocalDate endDate, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<ActivityLog> pageAuditLogs = activityLogRepository.getAllActivityLog(storeId, '%' + keyword + '%', actionMethod, startDate, endDate, pageDetails);

        List<ActivityLog> activityLogList = pageAuditLogs.getContent();
        List<ActivityLogDTO> activityLogDTOList = activityLogList.stream().map(activityLog -> {
            ActivityLogDTO mappingResult = modelMapper.map(activityLog, ActivityLogDTO.class);
            mappingResult.setCreatedBy(activityLog.getUser().getUsername());
            mappingResult.setCreatedOn(activityLog.getCreatedDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
            return mappingResult;
        }).toList();

        ActivityLogPagination activityLogPagination = new ActivityLogPagination();
        activityLogPagination.setContent(activityLogDTOList);
        activityLogPagination.setPageNumber(pageAuditLogs.getNumber());
        activityLogPagination.setPageSize(pageAuditLogs.getSize());
        activityLogPagination.setTotalElements(pageAuditLogs.getTotalElements());
        activityLogPagination.setTotalPages(pageAuditLogs.getTotalPages());
        activityLogPagination.setLastPage(pageAuditLogs.isLast());
        return activityLogPagination;
    }

    @Transactional
    @Override
    public void addActivityLog(User user, String actionMethod, String actionName, String entity, String description) {
       try {
           ActivityLog newActivityLog = new ActivityLog();
           newActivityLog.setActionMethod(actionMethod);
           newActivityLog.setActionName(actionName);
           newActivityLog.setEntity(entity);
           newActivityLog.setDescription(description);
           newActivityLog.setUser(user);
           activityLogRepository.save(newActivityLog);

       } catch (Exception e) {
           throw new InternalServerErrorException("Failed to add new Audit Log!");
       }
    }
}
