package com.demo.service.house;

import com.demo.base.LoginUserUtil;
import com.demo.entity.*;
import com.demo.repository.*;
import com.demo.service.ServiceResult;
import com.demo.web.dto.HouseDTO;
import com.demo.web.dto.HouseDetailDTO;
import com.demo.web.dto.HousePictureDTO;
import com.demo.web.form.HouseForm;
import com.demo.web.form.PhotoForm;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class HouseServiceImpl implements IHouseService {

    @Autowired
    public ModelMapper modelMapper;

    @Autowired
    public SubwayRepository subwayRepository;

    @Autowired
    public SubwayStationRepository subwayStationRepository;

    @Autowired
    public HouseRepository houseRepository;

    @Autowired
    public HouseDetailRepository houseDetailRepository;

    @Autowired
    private HousePictureRepository housePictureRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;

    @Override
    public ServiceResult<HouseDTO> save(HouseForm houseForm) {
        HouseDetail detail = new HouseDetail();
        ServiceResult<HouseDTO> subwayValidtionResult = wrapperDetailInfo(detail, houseForm);
        if (subwayValidtionResult != null) {
            return subwayValidtionResult;
        }
        House house = new House();
        modelMapper.map(houseForm, house);

        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        house.setAdminId(LoginUserUtil.getLonginUserId());
        house = houseRepository.save(house);

        detail.setHouseId(house.getId());
        detail = houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePictures(houseForm, house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.save(pictures);

        HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
        HouseDetailDTO houseDetailDTO = modelMapper.map(detail, HouseDetailDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(modelMapper.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);
        houseDTO.setCover(this.cdnPrefix + houseDTO.getCover());

        List<String> tags = houseForm.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTag(house.getId(), tag));
            }
            houseTagRepository.save(houseTags);
            houseDTO.setTags(tags);
        }
        return new ServiceResult<HouseDTO>(true, null, houseDTO);
    }

    private List<HousePicture> generatePictures(HouseForm form, Long houseId) {
        List<HousePicture> pictures = new ArrayList<>();
        if (form.getPhotos() == null || form.getPhotos().isEmpty()) {
            return pictures;
        }
        for (PhotoForm photoForm : form.getPhotos()) {
            HousePicture picture = new HousePicture();
            picture.setHouseId(houseId);
            picture.setCdnPrefix(cdnPrefix);
            picture.setPath(photoForm.getPath());
            picture.setWidth(photoForm.getWidth());
            picture.setHeight(photoForm.getHeight());
            pictures.add(picture);
        }
        return pictures;
    }


    private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {
        Subway subway = subwayRepository.findOne(houseForm.getSubwayLineId());
        if (subway == null) {
            return new ServiceResult<>(false, "Not valid subway line!");
        }
        SubwayStation subwayStation = subwayStationRepository.findOne(houseForm.getSubwayStationId());
        if (subwayStation == null || subway.getId() != subwayStation.getSubwayId()) {
            return new ServiceResult<>(false, "Not valid subway station!");
        }
        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());
        return null;
    }

}
