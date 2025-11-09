package com.dev.attendo.dtos.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDTO {
    private int id;
    private String actionMethod;
    private String actionName;
    private String entity;
    private String description;

    // Additional
    private String createdBy;
    private String createdOn;
}
