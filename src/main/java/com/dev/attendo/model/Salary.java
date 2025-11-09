package com.dev.attendo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private int amount;

    @NotNull
    private LocalDate effectiveDate;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_user", referencedColumnName = "id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonBackReference
    @JoinColumn(name = "id_store", referencedColumnName = "id")
    private Store store;
}
