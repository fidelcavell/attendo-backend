package com.dev.attendo.model;

import com.dev.attendo.utils.enums.ApprovalStatusEnum;
import com.dev.attendo.utils.enums.LeaveTypeEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private ApprovalStatusEnum status;

    @Enumerated(EnumType.STRING)
    private LeaveTypeEnum type;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;

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

    @OneToMany(mappedBy = "leaveApplication", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private List<Attendance> attendanceList;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_store", referencedColumnName = "id")
    private Store store;
}
