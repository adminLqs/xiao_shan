package com.xiaoshan.springbootdemo.entity.dto;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MerchantApplyDTO {

    // 联系人信息
    @NotBlank(message = "联系人姓名不能为空")
    @Size(min = 2, max = 20, message = "联系人姓名长度应在2-20个字符之间")
    private String contactName;                         // 联系人姓名

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号码格式")
    private String contactPhone;                        // 联系电话

    @NotBlank(message = "联系邮箱不能为空")
    @Email(message = "请输入正确的邮箱格式")
    private String contactEmail;                        // 联系邮箱

    // 店铺信息
    @NotBlank(message = "店铺名称不能为空")
    @Size(min = 2, max = 50, message = "店铺名称长度应在2-50个字符之间")
    private String storeName;                           // 店铺名称

    @Size(max = 500, message = "店铺描述长度不能超过500个字符")
    private String storeDetail;                         // 店铺描述（可选）

    @NotNull(message = "经营类型不能为空")
    private String businessType;                        // 经营类型

    @NotNull(message = "主营类目不能为空")
    private String mainCategory;                        // 主营类目

    // 资质文件
    @NotNull(message = "营业执照不能为空")
    private MultipartFile businessLicense;              // 营业执照文件

    @NotNull(message = "身份证正面照片不能为空")
    private MultipartFile idCardFront;                  // 身份证正面照片

    @NotNull(message = "身份证反面照片不能为空")
    private MultipartFile idCardBack;                   // 身份证反面照片
}