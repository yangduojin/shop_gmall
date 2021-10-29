package com.atguigu.controller;


import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.result.RetVal;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 属性表 前端控制器
 * </p>
 *
 * @author yx
 * @since 2021-10-27
 */
@RestController
@RequestMapping("/product")
public class PlatformPropertyKeyController {

    @Autowired
    private PlatformPropertyKeyService platformPropertyKeyService;
    @Autowired
    private PlatformPropertyValueService platformPropertyValueService;

    @GetMapping("getPlatformPropertyByCategoryId/{category1Id}/{category2Id}/{category3Id}")
    public RetVal getPlatformPropertyByCategoryId(
            @PathVariable Long category1Id,
            @PathVariable Long category2Id,
            @PathVariable Long category3Id){
       List<PlatformPropertyKey> platformPropertyList = platformPropertyKeyService.getPlatformPropertyByCategoryId(category1Id,category2Id,category3Id);

        return RetVal.ok(platformPropertyList);
    }

    @GetMapping("getPropertyValueByPropertyKeyId/{propertyKeyId}")
    public RetVal getPropertyValueByPropertyKeyId(@PathVariable Long propertyKeyId){
        List<PlatformPropertyValue> platformPropertyValueList = platformPropertyValueService.getPlatformPropertyByPropertyKeyId(propertyKeyId);
        return RetVal.ok(platformPropertyValueList);
    }

    @PostMapping("savePlatformProperty")
    public RetVal savePlatformProperty(@RequestBody PlatformPropertyKey platformPropertyKey){
        platformPropertyKeyService.savePlatformProperty(platformPropertyKey);

        return RetVal.ok();
    }

}

