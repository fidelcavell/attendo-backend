package com.dev.attendo.controller;

import com.dev.attendo.dtos.toko.StoreDTO;
import com.dev.attendo.security.response.MessageResponse;
import com.dev.attendo.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store")
@PreAuthorize("hasRole('OWNER')")
public class StoreController {

    @Autowired
    StoreService storeService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{storeId}")
    public ResponseEntity<?> getStoreById(@PathVariable Long storeId) {
        StoreDTO storeDTO = storeService.getStore(storeId);
        return ResponseEntity.ok(storeDTO);
    }

    @PostMapping("/{ownerUsername}")
    public ResponseEntity<?> addStore(@PathVariable String ownerUsername, @RequestBody StoreDTO storeDTO) {
        storeService.addStore(ownerUsername, storeDTO);
        return ResponseEntity.ok(new MessageResponse(true, "New Store has been added!"));
    }

    @PutMapping("/{storeId}")
    public ResponseEntity<?> updateStore(@PathVariable Long storeId, @RequestBody StoreDTO storeDTO) {
        storeService.updateStore(storeId, storeDTO);
        return ResponseEntity.ok(new MessageResponse(true, "Store has been updated!"));
    }

    @PutMapping("/store-activation/{storeId}")
    public ResponseEntity<?> storeActivation(@PathVariable Long storeId) {
        storeService.storeActivation(storeId);
        return ResponseEntity.ok(new MessageResponse(true, "Store's activation status has been updated!"));
    }

    @GetMapping("/owned/{username}")
    public ResponseEntity<?> getAllOwnedStore(@PathVariable String username) {
        return ResponseEntity.ok(storeService.getAllOwnedStore(username));
    }
}
