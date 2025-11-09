package com.dev.attendo.service;

import com.dev.attendo.dtos.toko.StoreDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StoreService {

    StoreDTO getStore(Long storeId);

    void addStore(String ownerUsername, StoreDTO storeDTO);

    void updateStore(Long id, StoreDTO storeDTO);

    void storeActivation(Long storeId);

    List<StoreDTO> getAllOwnedStore(String username);
}
