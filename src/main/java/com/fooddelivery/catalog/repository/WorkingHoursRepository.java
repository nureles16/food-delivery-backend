package com.fooddelivery.catalog.repository;


import com.fooddelivery.catalog.entity.WorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkingHoursRepository extends JpaRepository<WorkingHours, UUID> {
    void saveAll(List<WorkingHours> workingHoursList);

    void deleteByRestaurantId(UUID id);
}
