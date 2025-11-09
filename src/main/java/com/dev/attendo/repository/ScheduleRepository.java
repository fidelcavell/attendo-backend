package com.dev.attendo.repository;

import com.dev.attendo.model.Store;
import com.dev.attendo.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByStoreId(Long storeId);

    boolean existsByNameAndStoreAndStartTimeAndEndTime(String name, Store store, LocalTime startTime, LocalTime endTime);
}
