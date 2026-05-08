package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressMapper addressMapper;

    // 增（Create）
    // ============================================================

    /**
     * 新增收货地址
     *
     * 业务逻辑：
     * 1. 如果新增的地址是默认地址，先清除该用户所有现有的默认地址标记
     * 2. 插入新地址到数据库
     *
     * @param address 地址实体（必须包含 userId 和地址信息）
     */
    @Transactional
    public void addAddress(Address address) {
        // 如果新增的是默认地址，先清除该用户所有现有的默认地址标记
        // 确保一个用户只能有一个默认地址
        if (address.getIsDefault()) {
            addressMapper.clearDefaultByUserId(address.getUserId());
            log.debug("清除用户默认地址标记: userId={}", address.getUserId());
        }

        // 插入新地址
        addressMapper.insert(address);
        log.info("添加地址成功: userId={}, addressId={}, isDefault={}",
                address.getUserId(), address.getId(), address.getIsDefault());
    }

    // 删（Delete）
    // ============================================================

    /**
     * 删除收货地址
     *
     * 业务逻辑：
     * 1. 验证地址是否存在且属于当前用户（防止越权删除）
     * 2. 执行删除操作
     *
     * 注意：删除默认地址后，用户将没有默认地址，需要用户手动设置新的默认地址
     *
     * @param userId    当前登录用户ID（用于权限校验）
     * @param addressId 要删除的地址ID
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        // 验证地址是否存在且属于当前用户（防止越权操作）
        addressMapper.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("地址不存在或无权操作"));

        // 执行删除
        addressMapper.deleteByIdAndUserId(addressId, userId);
        log.info("删除地址成功: userId={}, addressId={}", userId, addressId);
    }

    // 改（Update）
    // ============================================================

    /**
     * 更新收货地址（全字段更新）
     *
     * 业务逻辑：
     * 1. 验证地址是否存在且属于当前用户
     * 2. 如果用户要将此地址设为默认地址，先清除该用户所有现有的默认地址标记
     * 3. 执行更新操作
     *
     * 适用场景：用户在编辑地址时同时修改了收货人信息、地址内容或默认状态
     *
     * @param address 地址实体（必须包含 id、userId 和要更新的字段）
     */
    @Transactional
    public void updateAddress(Address address) {
        // 1. 验证地址是否存在且属于当前用户（防止越权操作）
        Address existingAddress = addressMapper.findByIdAndUserId(address.getId(), address.getUserId())
                .orElseThrow(() -> new RuntimeException("地址不存在或无权操作"));

        // 2. 处理默认地址逻辑
        // 条件：用户要将此地址设为默认地址 && 原地址不是默认地址
        // 作用：确保一个用户只能有一个默认地址
        if (address.getIsDefault() && !existingAddress.getIsDefault()) {
            addressMapper.clearDefaultByUserId(address.getUserId());
            log.debug("清除用户默认地址标记: userId={}", address.getUserId());
        }

        // 3. 执行更新
        addressMapper.update(address);
        log.info("更新地址成功: userId={}, addressId={}, isDefault={}",
                address.getUserId(), address.getId(), address.getIsDefault());
    }

    /**
     * 设置默认地址（专用接口）
     *
     * 业务逻辑：
     * 1. 验证地址是否存在且属于当前用户
     * 2. 清除该用户所有现有的默认地址标记
     * 3. 将目标地址设置为默认地址
     *
     * 适用场景：用户在地址列表中直接点击"设为默认"按钮
     * 优势：无需传递完整地址对象，只需地址ID，性能更好
     *
     * @param userId    当前登录用户ID（用于权限校验）
     * @param addressId 要设为默认的地址ID
     */
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        // 1. 验证地址是否存在且属于当前用户（防止越权操作）
        addressMapper.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("地址不存在或无权操作"));

        // 2. 清除该用户所有现有的默认地址标记
        addressMapper.clearDefaultByUserId(userId);
        log.debug("清除用户默认地址标记: userId={}", userId);

        // 3. 将目标地址设置为默认地址
        addressMapper.setDefault(addressId, userId);
        log.info("设置默认地址成功: userId={}, addressId={}", userId, addressId);
    }

    // 查（Select）
    // ============================================================

    /**
     * 根据地址ID查询地址信息
     *
     * 业务逻辑：
     * 1. 仅根据地址ID查询，不校验用户归属
     * 2. 适用于内部调用场景（如订单详情查询地址）
     *
     * 注意：此方法不进行权限校验，仅供内部服务调用使用
     *       如需权限校验，请使用 getAddressDetail(userId, addressId)
     *
     * @param addressId 地址ID
     * @return 地址信息
     * @throws RuntimeException 地址不存在时抛出
     */
    public Address getAddressById(Long addressId) {
        Address address = addressMapper.findById(addressId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        log.debug("查询地址信息: addressId={}, recipientName={}",
                addressId, address.getRecipientName());
        return address;
    }

    /**
     * 获取用户所有地址
     *
     * 业务逻辑：
     * 1. 根据用户ID查询所有地址
     * 2. 按默认地址优先、创建时间倒序排序
     *
     * 排序规则：
     * - is_default DESC：默认地址排在最前面
     * - created_at DESC：新添加的地址排在前面
     *
     * @param userId 用户ID
     * @return 地址列表（可能为空列表）
     */
    public List<Address> getUserAddresses(Long userId) {
        List<Address> addresses = addressMapper.findByUserId(userId);
        log.debug("查询用户地址列表: userId={}, 共{}条", userId, addresses.size());
        return addresses;
    }

    /**
     * 获取默认地址
     *
     * 业务逻辑：
     * 1. 查询用户标记为默认的地址
     * 2. 一个用户只有一个默认地址，使用 LIMIT 1 确保只返回一条
     *
     * 注意：可能返回 null（用户没有设置默认地址）
     *
     * @param userId 用户ID
     * @return 默认地址（可能为 null）
     */
    public Address getDefaultAddress(Long userId) {
        Address defaultAddress = addressMapper.findDefaultByUserId(userId).orElse(null);
        log.debug("查询默认地址: userId={}, 结果={}", userId, defaultAddress != null ? "存在" : "不存在");
        return defaultAddress;
    }

    /**
     * 获取用户地址详情
     *
     * 业务逻辑：
     * 1. 根据地址ID和用户ID查询
     * 2. 同时验证地址归属，防止越权查看
     *
     * @param userId 当前登录用户ID（用于权限校验）
     * @param addressId 地址ID
     * @return 地址详情
     * @throws RuntimeException 地址不存在或无权访问时抛出
     */
    public Address getAddressByUserIdAndAddressId(Long userId, Long addressId) {
        Address address = addressMapper.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("地址不存在或无权访问"));
        log.debug("查询地址详情: userId={}, addressId={}", userId, addressId);
        return address;
    }
}