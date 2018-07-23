package com.demo.repository;

import com.demo.entity.HousePicture;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HousePictureRepository extends CrudRepository<HousePicture, Long> {
    List<HousePicture> findAllByHouseId(Long id);
}
