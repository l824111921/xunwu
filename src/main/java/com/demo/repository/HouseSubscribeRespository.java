package com.demo.repository;

import com.demo.entity.HouseSubscribe;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface HouseSubscribeRespository extends PagingAndSortingRepository<HouseSubscribe, Long> {

    HouseSubscribe findByHouseIdAndUserId(Long houseId, Long loginUserId);

}
