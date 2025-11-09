package com.dev.attendo.dtos.toko;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreDTO {
    private Long id;
    private String name;
    private String address;
    private double lat;
    private double lng;
    private double radius;
    private int breakDuration;
    private int maxBreakCount;
    private int currentBreakCount = 0;
    private int lateClockInPenaltyAmount = 0;
    private int lateBreakOutPenaltyAmount = 0;
    private double multiplierOvertime = 1.0;
    private boolean isActive = true;
}
