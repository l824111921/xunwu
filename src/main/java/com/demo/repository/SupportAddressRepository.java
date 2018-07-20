package com.demo.repository;

import com.demo.entity.SupportAddress;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SupportAddressRepository extends CrudRepository<SupportAddress, Long> {

    List<SupportAddress> findAllByLevel(String level);

    List<SupportAddress> findAllByLevelAndBelongTo(String level, String belongTo);
}
