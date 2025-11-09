package com.dev.attendo.model;

import com.dev.attendo.utils.enums.ApprovalStatusEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@Entity
public class OvertimeApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ApprovalStatusEnum status;

    private LocalDate overtimeDate;

    private String description;

    private int overtimePay;

    private LocalTime startTime;

    private  LocalTime endTime;

    private int lateTolerance;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_approver", referencedColumnName = "id")
    private User approver;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_user", referencedColumnName = "id")
    private User user;

    @OneToOne(mappedBy = "overtimeApplication", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private Attendance attendance;
}
