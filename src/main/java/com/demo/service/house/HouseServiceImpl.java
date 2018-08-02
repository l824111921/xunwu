package com.demo.service.house;

import com.demo.base.HouseSort;
import com.demo.base.HouseStatus;
import com.demo.base.LoginUserUtil;
import com.demo.entity.*;
import com.demo.repository.*;
import com.demo.service.ServiceMultiResult;
import com.demo.service.ServiceResult;
import com.demo.service.search.ISearchService;
import com.demo.web.dto.HouseDTO;
import com.demo.web.dto.HouseDetailDTO;
import com.demo.web.dto.HousePictureDTO;
import com.demo.web.form.DatatableSearch;
import com.demo.web.form.HouseForm;
import com.demo.web.form.PhotoForm;
import com.demo.web.form.RentSearch;
import com.google.common.collect.Maps;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.*;

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

    @Autowired
    private HouseSubscribeRespository subscribeRespository;

    @Autowired
    private IQiNiuService qiNiuService;

    @Autowired
    private ISearchService searchService;

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

    @Override
    @Transactional
    public ServiceResult update(HouseForm houseForm) {
        House house = houseRepository.findOne(houseForm.getId());
        if (house == null) {
            return ServiceResult.notFound();
        }
        HouseDetail detail = houseDetailRepository.findByHouseId(house.getId());
        if (detail == null) {
            return ServiceResult.notFound();
        }
        ServiceResult wrapperResult = wrapperDetailInfo(detail, houseForm);
        if (wrapperResult != null) {
            return wrapperResult;
        }

        houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePictures(houseForm, houseForm.getId());
        housePictureRepository.save(pictures);

        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }
        modelMapper.map(houseForm, house);
        house.setLastUpdateTime(new Date());
        houseRepository.save(house);

        if (house.getStatus() == HouseStatus.PASSES.getValue()) {
            searchService.index(house.getId());
        }
        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch search) {
        List<HouseDTO> houseDTOS = new ArrayList<>();

        Sort sort = new Sort(Sort.Direction.fromString(search.getDirection()), search.getOrderBy());
        int page = search.getStart() / search.getLength();
        Pageable pageable = new PageRequest(page, search.getLength(), sort);

        Specification<House> specification = (root, query, cb) -> {

            Predicate predicate = cb.equal(root.get("adminId"), LoginUserUtil.getLonginUserId());
            predicate = cb.and(predicate, cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

            if (search.getCity() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), search.getCity()));
            }

            if (search.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), search.getStatus()));
            }

            if (search.getCreateTimeMin() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("createTime"), search.getCreateTimeMin()));
            }

            if (search.getCreateTimeMax() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("createTime"), search.getCreateTimeMax()));
            }

            if (search.getTitle() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("title"), "%" + search.getTitle() + "%"));
            }

            return predicate;
        };

        Page<House> houses = houseRepository.findAll(specification, pageable);
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix + house.getCover());
            houseDTOS.add(houseDTO);
        });
        return new ServiceMultiResult<>(houses.getTotalElements(), houseDTOS);
    }

    @Override
    public ServiceResult<HouseDTO> findCompleteOne(Long id) {
        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }
        HouseDetail detail = houseDetailRepository.findByHouseId(id);
        List<HousePicture> pictures = housePictureRepository.findAllByHouseId(id);

        HouseDetailDTO detailDTO = modelMapper.map(detail, HouseDetailDTO.class);
        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        for (HousePicture picture : pictures) {
            HousePictureDTO pictureDTO = modelMapper.map(picture, HousePictureDTO.class);
            pictureDTOS.add(pictureDTO);
        }

        List<HouseTag> tags = houseTagRepository.findAllByHouseId(id);
        List<String> tagList = new ArrayList<>();
        for (HouseTag tag : tags) {
            tagList.add(tag.getName());
        }

        HouseDTO result = modelMapper.map(house, HouseDTO.class);
        result.setHouseDetail(detailDTO);
        result.setPictures(pictureDTOS);
        result.setTags(tagList);

        if (LoginUserUtil.getLonginUserId() > 0) {
            HouseSubscribe subscribe = subscribeRespository.findByHouseIdAndUserId(house.getId(), LoginUserUtil.getLonginUserId());
            if (subscribe != null) {
                result.setSubscribeStatus(subscribe.getStatus());
            }
        }
        return ServiceResult.of(result);
    }

    @Override
    public ServiceResult removePhoto(Long id) {
        HousePicture picture = housePictureRepository.findOne(id);
        if (picture == null) {
            return ServiceResult.notFound();
        }

        try {
            Response response = qiNiuService.delete(picture.getPath());
            if (response.isOK()) {
                housePictureRepository.delete(id);
                return ServiceResult.success();
            } else {
                return new ServiceResult(false, response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ServiceResult(false, e.getMessage());
        }
    }

    @Override
    @Transactional
    public ServiceResult updateCover(Long coverId, Long targetId) {
        HousePicture picture = housePictureRepository.findOne(coverId);
        if (picture == null) {
            return ServiceResult.notFound();
        }
        houseRepository.updateCover(targetId, picture.getPath());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult addTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }
        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag != null) {
            return new ServiceResult(false, "标签已存在");
        }
        houseTagRepository.save(new HouseTag(houseId, tag));
        return ServiceResult.success();
    }

    @Override
    public ServiceResult removeTag(Long houseId, String tag) {
        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }
        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag == null) {
            return new ServiceResult(false, "标签已存在");
        }
        houseTagRepository.delete(houseTag.getId());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult updateStatus(Long id, int value) {
        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }

        if (house.getStatus() == value) {
            return new ServiceResult(false, "状态没有发生变化");
        }
        if (house.getStatus() == HouseStatus.RENTED.getValue()) {
            return new ServiceResult(false, "已出租的房源不允许修改状态");
        }
        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ServiceResult(false, "已删除的资源不允许操作");
        }
        houseRepository.updateStatus(id, value);
        if (value == HouseStatus.PASSES.getValue()) {
            searchService.index(id);
        } else {
            searchService.remove(id);
        }
        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {
        if (rentSearch.getKeywords() != null && !rentSearch.getKeywords().isEmpty()) {
            ServiceMultiResult<Long> serviceMultiResult = searchService.query(rentSearch);
            if (serviceMultiResult.getTotal() == 0) {
                return new ServiceMultiResult<>(0, new ArrayList<>());
            }
            return new ServiceMultiResult<>(serviceMultiResult.getTotal(), wrapperHouseResult(serviceMultiResult.getResult()));
        }
        return simpleQuery(rentSearch);
    }

    private ServiceMultiResult<HouseDTO> simpleQuery(RentSearch rentSearch) {
        Sort sort = HouseSort.generateSort(rentSearch.getOrderBy(), rentSearch.getOrderDirection());
        int page = rentSearch.getStart() / rentSearch.getSize();

        Pageable pageable = new PageRequest(page, rentSearch.getSize(), sort);
        Specification<House> specification = (root, criteriaQuery, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.equal(root.get("status"), HouseStatus.PASSES.getValue());
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("cityEnName"), rentSearch.getCityEnName()));

            if (HouseSort.DISTANCE_TO_SUBWAY_KEY.equals(rentSearch.getOrderBy())) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.ge(root.get(HouseSort.DISTANCE_TO_SUBWAY_KEY), -1));
            }
            return predicate;
        };
        Page<House> houses = houseRepository.findAll(specification, pageable);
        List<HouseDTO> houseDTOS = new ArrayList<>();
        List<Long> houseIds = new ArrayList<>();
        Map<Long, HouseDTO> idToHoseMap = Maps.newHashMap();
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix + house.getCover());
            houseDTOS.add(houseDTO);
            houseIds.add(house.getId());
            idToHoseMap.put(house.getId(), houseDTO);
        });
        wrapperHouseList(houseIds, idToHoseMap);
        return new ServiceMultiResult<>(houses.getTotalElements(), houseDTOS);
    }

    private List<HouseDTO> wrapperHouseResult(List<Long> houseIds) {
        List<HouseDTO> result = new ArrayList<>();
        Map<Long, HouseDTO> idToHouseMap = new HashMap<>();
        Iterable<House> houses = houseRepository.findAll(houseIds);
        houses.forEach(house -> {
            HouseDTO houseDTO = modelMapper.map(house, HouseDTO.class);
            houseDTO.setCover(this.cdnPrefix + house.getCover());
            idToHouseMap.put(house.getId(), houseDTO);
        });
        wrapperHouseList(houseIds, idToHouseMap);
        for (Long houseId : houseIds) {
            result.add(idToHouseMap.get(houseId));
        }
        return result;
    }

    private void wrapperHouseList(List<Long> houseIds, Map<Long, HouseDTO> idToHouseMap) {
        List<HouseDetail> details = houseDetailRepository.findAllByHouseIdIn(houseIds);
        details.forEach(houseDetail -> {
            HouseDTO houseDTO = idToHouseMap.get(houseDetail.getHouseId());
            HouseDetailDTO detailDTO = modelMapper.map(houseDetail, HouseDetailDTO.class);
            houseDTO.setHouseDetail(detailDTO);
        });
        List<HouseTag> houseTags = houseTagRepository.findAllByHouseIdIn(houseIds);
        houseTags.forEach(houseTag -> {
            HouseDTO houseDTO = idToHouseMap.get(houseTag.getHouseId());
            houseDTO.getTags().add(houseTag.getName());
        });
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
