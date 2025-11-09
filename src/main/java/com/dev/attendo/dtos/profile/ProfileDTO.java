package com.dev.attendo.dtos.profile;

import com.dev.attendo.utils.enums.GenderEnum;
import com.dev.attendo.utils.enums.MarriedStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private LocalDate birthDate;
    private GenderEnum gender;

    // Additional:
    private Long idUser = null;
    private String username = null;
    private String email = null;
    private String roleName = null;
    private Long idSchedule = null;
}
