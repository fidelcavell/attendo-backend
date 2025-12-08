package com.dev.attendo.service.impl;

import com.dev.attendo.dtos.toko.StoreDTO;
import com.dev.attendo.exception.InternalServerErrorException;
import com.dev.attendo.exception.ResourceNotFoundException;
import com.dev.attendo.model.Store;
import com.dev.attendo.model.User;
import com.dev.attendo.repository.StoreRepository;
import com.dev.attendo.repository.UserRepository;
import com.dev.attendo.service.StoreService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoreServiceImpl implements StoreService {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    public StoreDTO getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Data toko tidak ditemukan!"));
        return modelMapper.map(store, StoreDTO.class);
    }

    @Transactional
    @Override
    public void addStore(String ownerUsername, StoreDTO storeDTO) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + ownerUsername + "tidak ditemukan!"));

        try {
            Store store = modelMapper.map(storeDTO, Store.class);
            store.setOwner(selectedUser);
            storeRepository.save(store);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menambahkan data toko!");
        }
    }

    @Transactional
    @Override
    public void updateStore(Long storeId, StoreDTO storeDTO) {
        Store selectedStore = storeRepository.findByIdAndIsActive(storeId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Data toko tidak ditemukan!"));
        System.out.println(storeDTO);
        try {
            selectedStore.setName(storeDTO.getName());
            selectedStore.setAddress(storeDTO.getAddress());
            selectedStore.setLat(storeDTO.getLat());
            selectedStore.setLng(storeDTO.getLng());
            selectedStore.setRadius(storeDTO.getRadius());
            selectedStore.setBreakDuration(storeDTO.getBreakDuration());
            selectedStore.setUpdatedDate(LocalDateTime.now());
            selectedStore.setMaxBreakCount(storeDTO.getMaxBreakCount());
            selectedStore.setCurrentBreakCount(storeDTO.getCurrentBreakCount());
            storeRepository.save(selectedStore);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal mengubah data toko!");
        }
    }

    @Transactional
    @Override
    public void storeActivation(Long storeId) {
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Data toko tidak ditemukan!"));
        List<User> associatedUser = userRepository.findAllByStoreIdAndIsActive(selectedStore.getId(), true);

        try {
            if (!associatedUser.isEmpty()) {
                for (User employee : associatedUser) {
                    employee.setStore(null);
                    employee.getProfile().setSchedule(null);
                }
                userRepository.saveAll(associatedUser);
            }
            selectedStore.setActive(!selectedStore.isActive());
            storeRepository.save(selectedStore);

        } catch (Exception e) {
            throw new InternalServerErrorException("Gagal menghapus data toko!");
        }
    }

    @Override
    public List<StoreDTO> getAllOwnedStore(String username) {
        User owner = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User dengan username: " + username + " tidak ditemukan!"));

        List<Store> ownedStore = storeRepository.findAllByOwnerUsername(owner.getUsername());
        return ownedStore.stream().map(store -> modelMapper.map(store, StoreDTO.class)).toList();
    }
}
