package com.dev.attendo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalTime startTime;

    private LocalTime endTime;

    private int lateTolerance;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_store", referencedColumnName = "id")
    private Store store;

    // Relation: Schedule and Profile
    @OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY)
    private List<Profile> profileList;
}
