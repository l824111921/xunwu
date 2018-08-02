package com.demo.service.house;

import com.demo.service.ServiceMultiResult;
import com.demo.service.ServiceResult;
import com.demo.web.dto.HouseDTO;
import com.demo.web.form.DatatableSearch;
import com.demo.web.form.HouseForm;
import com.demo.web.form.RentSearch;

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

    /**
     * 移除图片
     *
     * @param id
     * @return
     */
    ServiceResult removePhoto(Long id);

    /**
     * 更新封面
     *
     * @param coverId
     * @param targetId
     * @return
     */
    ServiceResult updateCover(Long coverId, Long targetId);

    /**
     * 新增标签
     *
     * @param houseId
     * @param tag
     * @return
     */
    ServiceResult addTag(Long houseId, String tag);

    /**
     * 移除标签
     *
     * @param houseId
     * @param tag
     * @return
     */
    ServiceResult removeTag(Long houseId, String tag);

    ServiceResult updateStatus(Long id, int value);

    ServiceMultiResult<HouseDTO> query(RentSearch rentSearch);
}
