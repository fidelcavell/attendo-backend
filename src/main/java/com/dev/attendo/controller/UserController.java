package com.dev.attendo.controller;

import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('OWNER')")
public class UserController {

    @Autowired
    UserService userService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping("/{username}")
    public ResponseEntity<?> addEmployeeToStore(
            @PathVariable String username,
            @RequestParam(name = "store") Long storeId,
            @RequestParam String currentUser,
            @RequestParam int salaryAmount
    ) {
        userService.addEmployeeToStore(storeId, username, currentUser, salaryAmount);
        return ResponseEntity.ok(new MessageResponse(true, "New Employee has been added!"));
    }

    @PutMapping("/{username}")
    public ResponseEntity<?> removeEmployeeFromStore(@PathVariable String username) {
        userService.removeEmployeeFromStore(username);
        return ResponseEntity.ok(new MessageResponse(true, "Employee has been removed!"));
    }

    @PutMapping("/assign-role/{username}")
    public ResponseEntity<?> updateEmployeeRole(@PathVariable String username) {
        userService.updateEmployeeRole(username);
        return ResponseEntity.ok(new MessageResponse(true, "Employee's role has been updated!"));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/delete-account/{username}")
    public ResponseEntity<?> deleteAccount(@PathVariable String username) {
        userService.deleteAccount(username);
        return ResponseEntity.ok(new MessageResponse(true, "Account has been deleted!"));
    }
}
