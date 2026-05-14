package com.xiaoshan.springbootdemo.service;

import com.xiaoshan.springbootdemo.entity.CartItem;
import com.xiaoshan.springbootdemo.entity.dto.CartAddDTO;
import com.xiaoshan.springbootdemo.entity.dto.CartUpdateDTO;
import com.xiaoshan.springbootdemo.entity.vo.CheckoutItemVO;
import com.xiaoshan.springbootdemo.mapper.CartItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xiaoshan.springbootdemo.entity.vo.CartItemVO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class CartService {

    private final CartItemMapper cartItemMapper;

    /**
     * 添加商品到购物车
     * - 如果商品已在购物车中，则增加数量
     * - 如果不在，则新增记录
     *
     * @param userId 用户ID
     * @param dto 添加购物车参数
     */
    @Transactional
    public void addCartItem(Long userId, CartAddDTO dto) {
        // 1. 查询购物车中是否已有该商品
        Optional<CartItem> existingItem = cartItemMapper.findByUserIdAndProductId(userId, dto.getProductId());

        if (existingItem.isPresent()) {
            // 2. 已存在：增加数量
            CartItem cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + dto.getQuantity();
            cartItemMapper.updateQuantity(cartItem.getId(), newQuantity);
            log.info("购物车商品数量增加: userId={}, productId={}, newQuantity={}", userId, dto.getProductId(), newQuantity);
        } else {
            // 3. 不存在：新增记录
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(dto.getProductId());
            cartItem.setQuantity(dto.getQuantity());
            cartItemMapper.insert(cartItem);
            log.info("添加商品到购物车: userId={}, productId={}, quantity={}", userId, dto.getProductId(), dto.getQuantity());
        }
    }

    /**
     * 删除购物车项
     */
    @Transactional
    public void deleteCartItem(Long userId, Long cartItemId) {
        // 查询购物车项是否存在且属于当前用户
        Optional<CartItem> cartItemOpt = cartItemMapper.findByIdAndUserId(cartItemId, userId);

        if (cartItemOpt.isEmpty()) {
            throw new RuntimeException("购物车项不存在");
        }

        cartItemMapper.deleteById(cartItemId);
        log.info("删除购物车项: id={}", cartItemId);
    }
    /**
     * 批量删除购物车项
     */
    @Transactional
    public void batchDeleteCartItems(Long userId, List<Long> cartItemIds) {
        // 验证所有购物车项是否都属于当前用户
        for (Long id : cartItemIds) {
            Optional<CartItem> cartItemOpt = cartItemMapper.findByIdAndUserId(id, userId);
            if (cartItemOpt.isEmpty()) {
                throw new RuntimeException("购物车项不存在或无权操作: " + id);
            }
        }

        cartItemMapper.deleteByIds(cartItemIds);
        log.info("批量删除购物车项: ids={}", cartItemIds);
    }

    /**
     * 修改购物车商品数量
     */
    @Transactional
    public void updateCartItemQuantity(Long userId, CartUpdateDTO dto) {
        // 1. 查询购物车项是否存在
        CartItem cartItemOpt = cartItemMapper.findByIdAndUserId(dto.getCartItemId(), userId)
                .orElseThrow(() -> new RuntimeException("购物车项不存在"));

        // 2. 验证该购物车项是否属于当前用户
        if (!cartItemOpt.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }

        // 3. 数量为0则删除，否则更新
        if (dto.getQuantity() <= 0) {
            cartItemMapper.deleteById(dto.getCartItemId());
            log.info("删除购物车项: id={}", dto.getCartItemId());
        } else {
            cartItemMapper.updateQuantity(dto.getCartItemId(), dto.getQuantity());
            log.info("更新购物车数量: id={}, quantity={}", dto.getCartItemId(), dto.getQuantity());
        }
    }

    /**
     * 获取用户购物车列表（含商品信息）
     */
    public List<CartItemVO> getCartList(Long userId) {
        return cartItemMapper.findCartItemsWithProduct(userId);
    }


    /**
     * 获取用户购物车商品总数量
     *
     * @param userId 用户ID
     * @return 购物车商品总数量
     */
    public int getCartCount(Long userId) {
        Integer count = cartItemMapper.countQuantityByUserId(userId);
        return count != null ? count : 0;
    }

    // ============== 结算查询 ==============

    /**
     * 通过购物车选项查询关联商品信息
     * @param userId 用户ID
     * @param cartItemIds 购物项Ids
     * */
    public List<CheckoutItemVO> getCheckoutItems(Long userId, List<Long> cartItemIds) {
        // 将 List 转换为逗号分隔的字符串： [1,2,3] → "1,2,3"
        String idsStr = cartItemIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // 联表查询：购物车表 + 商品表 + 图片表
        return cartItemMapper.getCheckoutItems(userId, idsStr);
    }


}
