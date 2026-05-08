package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Category;
import com.xiaoshan.springbootdemo.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 获取所有启用的分类
     */
    public List<Category> findAllActive() {
        return categoryMapper.findAllActive();
    }
}
