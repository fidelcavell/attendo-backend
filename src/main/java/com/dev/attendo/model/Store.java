package com.dev.attendo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 6)
    private String name;

    @NotBlank
    private String address;

    @Column(name = "latitude")
    private double lat;

    @Column(name = "longitude")
    private double lng;

    private double radius;

    private int breakDuration;

    private int maxBreakCount;

    private int currentBreakCount;

    private int lateClockInPenaltyAmount;

    private int lateBreakOutPenaltyAmount;

    private double multiplierOvertime;

    private boolean isActive;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    // Relation: User and Toko (As Admin and Employee)
    @OneToMany(mappedBy = "store", fetch = FetchType.EAGER)
    private Set<User> users = new HashSet<>();

    // Relation: Toko and WorkSchedule
    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<Schedule> scheduleList;

    // Relation: Toko and Attendance
    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<Attendance> attendanceList;

    // Relation: Toko and Attendance
    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<LeaveApplication> leaveApplicationList;

    // Relation: User and Toko (As Owner) -> B2B Purpose
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_owner", referencedColumnName = "id")
    private User owner;

    // Relation: User and Salary
    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<Salary> salaryList;

    // Relation: User and Loan
    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<Loan> loanList;


    // For Initial Data purpose: LATER WILL BE DELETED!
    public Store(String name, String address, double lat, double lng, double radius, int breakDuration, int maxBreakCount, int currentBreakCount, int lateClockInPenaltyAmount, int lateBreakOutPenaltyAmount, double multiplierOvertime) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.radius = radius;
        this.breakDuration = breakDuration;
        this.maxBreakCount = maxBreakCount;
        this.currentBreakCount = currentBreakCount;
        this.lateClockInPenaltyAmount = lateClockInPenaltyAmount;
        this.lateBreakOutPenaltyAmount = lateBreakOutPenaltyAmount;
        this.multiplierOvertime = multiplierOvertime;
    }
}
