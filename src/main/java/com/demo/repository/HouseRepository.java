package com.demo.repository;

import com.demo.entity.House;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface HouseRepository extends CrudRepository<House, Long>,JpaSpecificationExecutor<House> {



}
