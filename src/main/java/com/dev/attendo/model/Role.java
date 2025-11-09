package com.dev.attendo.model;

import com.dev.attendo.utils.enums.RoleEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @Enumerated(EnumType.STRING)
    private RoleEnum name;

    @ToString.Exclude
    @JsonBackReference
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    private Set<User> users = new HashSet<>();

    public Role(RoleEnum name) {
        this.name = name;
    }
}
