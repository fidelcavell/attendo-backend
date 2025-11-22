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
import org.springframework.web.multipart.MultipartFile;

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
                .orElseThrow(() -> new ResourceNotFoundException("Store is not found!"));
        return modelMapper.map(store, StoreDTO.class);
    }

    @Transactional
    @Override
    public void addStore(String ownerUsername, StoreDTO storeDTO) {
        User selectedUser = userRepository.findByUsernameAndIsActiveTrue(ownerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + ownerUsername + " is not found!"));

        try {
            Store store = modelMapper.map(storeDTO, Store.class);
            store.setOwner(selectedUser);
            storeRepository.save(store);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to save Toko!");
        }
    }

    @Transactional
    @Override
    public void updateStore(Long storeId, StoreDTO storeDTO) {
        Store store = storeRepository.findByIdAndIsActive(storeId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Store is not found!"));
        System.out.println(storeDTO);
        try {
            store.setName(storeDTO.getName());
            store.setAddress(storeDTO.getAddress());
            store.setLat(storeDTO.getLat());
            store.setLng(storeDTO.getLng());
            store.setRadius(storeDTO.getRadius());
            store.setBreakDuration(storeDTO.getBreakDuration());
            store.setUpdatedDate(LocalDateTime.now());
            store.setMaxBreakCount(storeDTO.getMaxBreakCount());
            store.setCurrentBreakCount(storeDTO.getCurrentBreakCount());
            storeRepository.save(store);

        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to update Store!");
        }
    }

    @Transactional
    @Override
    public void storeActivation(Long storeId) {
        Store selectedStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store is not found!"));
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
            throw new InternalServerErrorException("Failed to delete Store!");
        }
    }

    @Override
    public List<StoreDTO> getAllOwnedStore(String username) {
        User owner = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username: " + username + " is not found!"));

        List<Store> ownedStore = storeRepository.findAllByOwnerUsername(owner.getUsername());
        return ownedStore.stream().map(store -> modelMapper.map(store, StoreDTO.class)).toList();
    }
}
