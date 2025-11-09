package com.dev.attendo.model;

import com.dev.attendo.utils.enums.AttendanceStatusEnum;
import com.dev.attendo.utils.enums.AttendanceTypeEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AttendanceTypeEnum type;

    @Enumerated(EnumType.STRING)
    private AttendanceStatusEnum status;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] photoIn;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] photoOut;

    private LocalDateTime clockIn;

    private LocalDateTime clockOut;

    private LocalDateTime breakIn;

    private LocalDateTime breakOut;

    private String description;

    private int deductionAmount;

    private int lateInMinutes;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_user", referencedColumnName = "id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_store", referencedColumnName = "id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_leave", referencedColumnName = "id")
    private LeaveApplication leaveApplication;

    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_overtime", referencedColumnName = "id")
    private OvertimeApplication overtimeApplication;
}
