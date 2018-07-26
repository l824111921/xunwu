package com.demo.service.house;

import com.demo.service.ServiceMultiResult;
import com.demo.service.ServiceResult;
import com.demo.web.dto.HouseDTO;
import com.demo.web.form.DatatableSearch;
import com.demo.web.form.HouseForm;

public interface IHouseService {

    ServiceResult<HouseDTO> save(HouseForm houseForm);

    ServiceResult update(HouseForm houseForm);

    ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch search);

    /**
     * 查询完整房源信息
     *
     * @param id
     * @return
     */
    ServiceResult<HouseDTO> findCompleteOne(Long id);
}
