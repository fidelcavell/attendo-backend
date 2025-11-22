package com.dev.attendo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @JsonIgnore
    private String password;

    private boolean isActive;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    // Relation: User and Role
    @ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE })
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_role", referencedColumnName = "id")
    private Role role;

    // Relation: User and Profile
    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.REMOVE })
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_profile", referencedColumnName = "id")
    private Profile profile;

    // Relation: User and Store (As Admin and Employee)
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_store", referencedColumnName = "id")
    private Store store;

    // Relation: User and Attendance
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.REMOVE })
    private List<Attendance> attendances;

    // Relation: User and PasswordResetToken
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.REMOVE })
    private List<Token> resetTokens;

    // Relation: User and AuditLog
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private List<ActivityLog> activityLogList;

    // Relation: User and Leave (As approver)
    @OneToMany(mappedBy = "approver", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private List<LeaveApplication> approverLeaveApplicationList;

    // Relation: User and Leave (As employee)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private List<LeaveApplication> issuerLeaveApplicationList;

    // Relation: User and Leave (As approver)
    @OneToMany(mappedBy = "approver", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private List<OvertimeApplication> approverOvertimeApplicationList;

    // Relation: User and Overtime (As employee)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private List<OvertimeApplication> issuerOvertimeApplicationList;

    // Relation: User and Toko (As Owner) -> B2B Purpose
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private List<Store> ownedStoreList;

    // Relation: User and Salary
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Salary> salaryList;

    // Relation: User and Loan
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Loan> loanList;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return id != null && id.equals(((User) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public User(String username, String email, String password, Role role, boolean isActive) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
