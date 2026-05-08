package com.xiaoshan.springbootdemo.controller;

import com.xiaoshan.springbootdemo.entity.Address;
import com.xiaoshan.springbootdemo.entity.dto.AddressDTO;
import com.xiaoshan.springbootdemo.service.AddressService;
import com.xiaoshan.springbootdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AddressController {

    private final UserService userService;
    private final AddressService addressService;

    /**
     * 新增收货地址
     * POST /api/v1/addresses
     */
    @PostMapping("/addresses")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> addAddress(
            Authentication authentication,
            @Valid @RequestBody AddressDTO addressDTO
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            // DTO 转 Entity
            Address address = convertToEntity(addressDTO);
            address.setUserId(userId);

            addressService.addAddress(address);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "添加成功"
            ));
        } catch (Exception e) {
            log.error("添加地址失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户所有地址
     * GET /api/v1/addresses
     */
    @GetMapping("/addresses")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getAddresses(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            List<Address> addresses = addressService.getUserAddresses(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", addresses
            ));
        } catch (Exception e) {
            log.error("获取地址列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取默认地址
     * GET /api/v1/addresses/default
     */
    @GetMapping("/addresses/default")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getDefaultAddress(Authentication authentication) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Address address = addressService.getDefaultAddress(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", address
            ));
        } catch (Exception e) {
            log.error("获取默认地址失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取地址详情
     * GET /api/v1/addresses/{addressId}
     */
    @GetMapping("/addresses/{addressId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> getAddressDetail(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            Address address = addressService.getAddressByUserIdAndAddressId(userId, addressId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", address
            ));
        } catch (Exception e) {
            log.error("获取地址详情失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新收货地址
     * PUT /api/v1/addresses/{addressId}
     */
    @PutMapping("/addresses/{addressId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressDTO addressDTO
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);

            // DTO 转 Entity
            Address address = convertToEntity(addressDTO);
            address.setId(addressId);
            address.setUserId(userId);

            addressService.updateAddress(address);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "更新成功"
            ));
        } catch (Exception e) {
            log.error("更新地址失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 设置默认地址
     * PUT /api/v1/addresses/{addressId}/default
     */
    @PutMapping("/addresses/{addressId}/default")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> setDefaultAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        try {
            // 获取认证用户ID
            Long userId = userService.getCurrentUserId(authentication);
            addressService.setDefaultAddress(userId, addressId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "设置成功"
            ));
        } catch (Exception e) {
            log.error("设置默认地址失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除收货地址
     * DELETE /api/v1/addresses/{addressId}
     */
    @DeleteMapping("/addresses/{addressId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_SELLER','ROLE_ADMIN')")
    public ResponseEntity<?> deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        try {
            Long userId = userService.getCurrentUserId(authentication);
            addressService.deleteAddress(userId, addressId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "删除成功"
            ));
        } catch (Exception e) {
            log.error("删除地址失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ========== 私有方法 ==========

    /**
     * DTO 转 Entity
     */
    private Address convertToEntity(AddressDTO dto) {
        Address address = new Address();
        address.setRecipientName(dto.getRecipientName());
        address.setRecipientPhone(dto.getRecipientPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetailAddress(dto.getDetailAddress());
        address.setLabel(dto.getLabel());
        address.setIsDefault(dto.getIsDefault());
        return address;
    }
}