package com.dev.attendo.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String roleName;
    private boolean isActive;

    private Long idProfile;
    private Long idSchedule;
    private Long idStore;
}
