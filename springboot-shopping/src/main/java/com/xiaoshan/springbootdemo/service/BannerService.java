package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Banner;
import com.xiaoshan.springbootdemo.mapper.BannerMapper;
import com.xiaoshan.springbootdemo.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerMapper bannerMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public List<Banner> findAll() {
        return bannerMapper.findAll();
    }

    public List<Banner> findActiveByPosition(String position) {
        return bannerMapper.findActiveByPosition(position);
    }

    public Banner findById(Long id) {
        return bannerMapper.findById(id);
    }

    @Transactional
    public Banner create(Banner banner) {
        if (banner.getTitle() == null || banner.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (banner.getImageUrl() == null || banner.getImageUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("图片地址不能为空");
        }

        banner.setId(snowflakeIdGenerator.nextId());
        if (banner.getSortOrder() == null) {
            Integer maxSort = bannerMapper.getMaxSortOrderByPosition(banner.getPosition() != null ? banner.getPosition() : "HOME");
            banner.setSortOrder(maxSort == null ? 0 : maxSort + 1);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getPosition() == null) {
            banner.setPosition("HOME");
        }
        banner.setCreatedAt(LocalDateTime.now());
        banner.setUpdatedAt(LocalDateTime.now());

        bannerMapper.insert(banner);
        log.info("创建Banner: id={}, title={}", banner.getId(), banner.getTitle());
        return banner;
    }

    @Transactional
    public Banner update(Long id, Banner banner) {
        Banner existing = bannerMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Banner不存在");
        }

        if (banner.getTitle() != null && !banner.getTitle().trim().isEmpty()) {
            existing.setTitle(banner.getTitle().trim());
        }
        if (banner.getImageUrl() != null && !banner.getImageUrl().trim().isEmpty()) {
            existing.setImageUrl(banner.getImageUrl().trim());
        }
        if (banner.getLinkUrl() != null) {
            existing.setLinkUrl(banner.getLinkUrl());
        }
        if (banner.getSortOrder() != null) {
            existing.setSortOrder(banner.getSortOrder());
        }
        if (banner.getStatus() != null) {
            existing.setStatus(banner.getStatus());
        }
        if (banner.getStartTime() != null) {
            existing.setStartTime(banner.getStartTime());
        }
        if (banner.getEndTime() != null) {
            existing.setEndTime(banner.getEndTime());
        }
        if (banner.getPosition() != null) {
            existing.setPosition(banner.getPosition());
        }
        existing.setUpdatedAt(LocalDateTime.now());

        bannerMapper.updateById(existing);
        log.info("更新Banner: id={}, title={}", id, existing.getTitle());
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        Banner banner = bannerMapper.findById(id);
        if (banner == null) {
            throw new IllegalArgumentException("Banner不存在");
        }

        bannerMapper.deleteById(id);
        log.info("删除Banner: id={}", id);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        Banner banner = bannerMapper.findById(id);
        if (banner == null) {
            throw new IllegalArgumentException("Banner不存在");
        }

        bannerMapper.updateStatus(id, status);
        log.info("更新Banner状态: id={}, status={}", id, status);
    }

    @Transactional
    public void updateSortOrder(Long id, Integer sortOrder) {
        Banner banner = bannerMapper.findById(id);
        if (banner == null) {
            throw new IllegalArgumentException("Banner不存在");
        }

        bannerMapper.updateSortOrder(id, sortOrder);
        log.info("更新Banner排序: id={}, sortOrder={}", id, sortOrder);
    }
}