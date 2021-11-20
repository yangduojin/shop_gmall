package com.atguigu.controller;


import com.atguigu.entity.BaseBrand;
import com.atguigu.mapper.BaseBrandMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseBrandService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@RestController
@RequestMapping("/product/brand")
public class BaseBrandController {

    @Value("${fastdfs.prefix}")
    String prefix;

    @Autowired
    BaseBrandService baseBrandService;

    @Autowired
    BaseBrandMapper BaseBrandMapper;



    @GetMapping("/queryBrandByPage/{pageNum}/{pageSize}")
    public RetVal queryBrandByPage(@PathVariable Long pageNum,
                               @PathVariable Long pageSize){
        Page<BaseBrand> baseBrandPage = new Page<>(pageNum,pageSize);
        IPage<BaseBrand> page = baseBrandService.page(baseBrandPage, null);
        return RetVal.ok(page);
    }

    @PostMapping
    public RetVal saveBrand(@RequestBody BaseBrand baseBrand){
        baseBrandService.save(baseBrand);
        return RetVal.ok();
    }

    @PutMapping
    public RetVal updateBrand(@RequestBody BaseBrand baseBrand){
        baseBrandService.updateById(baseBrand);
        return RetVal.ok();
    }

    @GetMapping("{id}")
    public RetVal getBrand(@PathVariable Long id){
        BaseBrand baseBrand = baseBrandService.getById(id);
        return RetVal.ok(baseBrand);
    }

    // feign 调用返回BaseBrand
    @GetMapping("getBrandById/{id}")
    public BaseBrand getBrandById(@PathVariable Long id){
        BaseBrand baseBrand = baseBrandService.getById(id);
        return baseBrand;
    }

    @DeleteMapping("{id}")
    public RetVal delBrand(@PathVariable Long id){
        baseBrandService.removeById(id);
        return RetVal.ok();
    }

    @PostMapping("/fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception {
        String filePath = this.getClass().getResource("/tracker.conf").getFile();
        String originalFilename = file.getOriginalFilename();
        String filenameExtension = StringUtils.getFilenameExtension(originalFilename);
        ClientGlobal.init(filePath);
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = trackerClient.getConnection();
        StorageClient1 storageClient1 = new StorageClient1(connection, null);
        String s = storageClient1.upload_file1(file.getBytes(), filenameExtension, null);
        System.out.println(prefix + s);
        return RetVal.ok(prefix + s);
    }

    @GetMapping("/getAllBrand")
    public RetVal getAllBrand(){
        List<BaseBrand> baseBrands = BaseBrandMapper.selectList(null);
        return RetVal.ok(baseBrands);
    }


}

