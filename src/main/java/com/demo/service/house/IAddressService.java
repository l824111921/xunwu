package com.demo.service.house;

import com.demo.entity.SupportAddress;
import com.demo.service.ServiceMultiResult;
import com.demo.web.dto.SubwayDTO;
import com.demo.web.dto.SubwayStationDTO;
import com.demo.web.dto.SupportAddressDTO;

import java.util.List;
import java.util.Map;

public interface IAddressService {

    /**
     * 获取所有支持的城市列表
     *
     * @return
     */
    ServiceMultiResult<SupportAddressDTO> findAllCities();

    /**
     * 根据英文简写获取具体区域的信息
     *
     * @param cityEnName
     * @param regionEnName
     * @return
     */
    Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName);

    /**
     * 根据城市英文简写获取该城市所有支持的区域信息
     *
     * @param cityName
     * @return
     */
    ServiceMultiResult findAllRegionsByCityName(String cityName);

    /**
     * 获取该城市所有的地铁线路
     *
     * @param cityEnName
     * @return
     */
    List<SubwayDTO> findAllSubwayByCity(String cityEnName);

    List<SubwayStationDTO> findAllStationBySubway(Long subwayId);

}
