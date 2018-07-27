package com.demo.repository;

import com.demo.entity.HouseTag;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HouseTagRepository extends CrudRepository<HouseTag, Long> {

    List<HouseTag> findAllByHouseId(Long id);

    HouseTag findByNameAndHouseId(String name, Long houseId);
}
