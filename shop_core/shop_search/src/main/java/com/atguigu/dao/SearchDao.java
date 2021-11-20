package com.atguigu.dao;

import com.atguigu.search.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchDao extends ElasticsearchRepository<Product,Long> {
}
