package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.mapper.BaseCategoryViewMapper;
import com.atguigu.search.Product;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * VIEW 服务实现类
 * </p>
 *
 * @author yx
 * @since 2021-11-01
 */
@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {

    @Override
    public List<JSONObject> getIndexCategoryInfo() {
        List<BaseCategoryView> baseCategoryViews = baseMapper.selectList(null);
        List<JSONObject> jsonObjects = new ArrayList<>();
        Map<Long, List<BaseCategoryView>> category1List = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        int index = 1;
        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1List.entrySet()) {

            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("index",index++);
            jsonObject1.put("categoryId",category1Entry.getKey());
            jsonObject1.put("categoryName",category1Entry.getValue().get(0).getCategory1Name());
            List<BaseCategoryView> category1EntryValue = category1Entry.getValue();
            Map<Long, List<BaseCategoryView>> category2List = category1EntryValue.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            ArrayList<JSONObject> category1Child = new ArrayList<>();

            for (Map.Entry<Long, List<BaseCategoryView>> category2Entry : category2List.entrySet()) {

                JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("categoryId",category2Entry.getKey());
                jsonObject2.put("categoryName",category2Entry.getValue().get(0).getCategory2Name());
                List<BaseCategoryView> category2EntryValue = category2Entry.getValue();
                Map<Long, List<BaseCategoryView>> category3List = category2EntryValue.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                List<JSONObject> category2Child = new ArrayList<>();
                for (Map.Entry<Long, List<BaseCategoryView>> category3Entry : category3List.entrySet()) {
                    JSONObject jsonObject3 = new JSONObject();
                    jsonObject3.put("categoryId",category3Entry.getKey());
                    jsonObject3.put("categoryName",category3Entry.getValue().get(0).getCategory3Name());
                    category2Child.add(jsonObject3);
                }
                jsonObject2.put("categoryChild",category2Child);
                category1Child.add(jsonObject2);
            }
            jsonObject1.put("categoryChild",category1Child);
            jsonObjects.add(jsonObject1);
        }

        return jsonObjects;
    }
}
