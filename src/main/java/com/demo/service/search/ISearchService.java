package com.demo.service.search;

import com.demo.service.ServiceMultiResult;
import com.demo.web.form.RentSearch;

public interface ISearchService {
    void index(Long houseId);

    void remove(Long houseId);

    /**
     * 查询房源接口
     *
     * @param rentSearch
     * @return
     */
    ServiceMultiResult<Long> query(RentSearch rentSearch);
}
