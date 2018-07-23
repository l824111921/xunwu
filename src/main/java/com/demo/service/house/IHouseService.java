package com.demo.service.house;

import com.demo.service.ServiceResult;
import com.demo.web.dto.HouseDTO;
import com.demo.web.form.HouseForm;

public interface IHouseService {

    ServiceResult<HouseDTO> save(HouseForm houseForm);
}
