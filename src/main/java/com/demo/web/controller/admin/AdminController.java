package com.demo.web.controller.admin;

import com.demo.base.ApiResponse;
import com.demo.service.house.IQiNiuService;
import com.demo.web.dto.QiNiuPutRet;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class AdminController {

    @Autowired
    private IQiNiuService qiNiuService;

    @Autowired
    private Gson gson;

    @GetMapping("/admin/center")
    public String adminCenterPage() {
        return "admin/center";
    }

    @GetMapping("/admin/welcome")
    public String welcomePage() {
        return "admin/welcome";
    }

    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin/login";
    }

    /**
     * 房源列表页
     *
     * @return
     */
    @GetMapping("admin/house/list")
    public String houseListPage() {
        return "admin/house-list";
    }

    /**
     * 新增房源功能页
     *
     * @return
     */
    @GetMapping("admin/add/house")
    public String addHousePage() {
        return "admin/house-add";
    }

    /**
     * 上传图片接口
     *
     * @param file
     * @return
     */
    @PostMapping(value = "admin/upload/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ApiResponse uploadPhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }
        try {
            InputStream inputStream = file.getInputStream();
            Response response = qiNiuService.uploadFile(inputStream);
            if (response.isOK()) {
                QiNiuPutRet ret = gson.fromJson(response.bodyString(), QiNiuPutRet.class);
                return ApiResponse.ofSuccess(ret);
            } else {
                return ApiResponse.ofMessage(response.statusCode, response.getInfo());
            }
        } catch (QiniuException e) {
            Response response = e.response;
            try {
                return ApiResponse.ofMessage(response.statusCode, response.bodyString());
            } catch (QiniuException e1) {
                e1.printStackTrace();
                return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ApiResponse.ofSuccess(null);
    }

}
