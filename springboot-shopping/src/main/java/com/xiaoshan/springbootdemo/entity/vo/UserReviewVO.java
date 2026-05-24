package com.xiaoshan.springbootdemo.entity.vo;

import com.xiaoshan.springbootdemo.entity.Review;
import com.xiaoshan.springbootdemo.entity.ReviewImage;
import com.xiaoshan.springbootdemo.entity.ReviewVideo;
import com.xiaoshan.springbootdemo.entity.UserProfile;
import lombok.Data;

import java.util.List;

@Data
public class UserReviewVO {

    private Review review;

    private List<ReviewImage> reviewImages;

    private List<ReviewVideo> reviewVideos;

    private UserProfile userProfile;

    // 商品名称（用于店铺评价列表显示）
    private String productName;

    // 商品图片（用于店铺评价列表显示）
    private String productImage;

    // 规格名称（用于店铺评价列表显示）
    private String skuName;

    // 商品ID（用于跳转）
    private Long productId;

    public UserReviewVO(Review review, List<ReviewImage> reviewImages, List<ReviewVideo> reviewVideos, UserProfile userProfile) {
        this.review = review;
        this.reviewImages = reviewImages;
        this.reviewVideos = reviewVideos;
        this.userProfile = userProfile;
        // 从 review 中提取商品信息
        if (review != null) {
            this.productId = review.getProductId();
            this.productName = review.getProductName();
            this.productImage = review.getProductImage();
            // 优先使用 skuName（JOIN查询），否则使用 skuSpec（快照）
            this.skuName = review.getSkuName() != null ? review.getSkuName() : review.getSkuSpec();
        }
    }

    // 向后兼容的构造函数
    public UserReviewVO(Review review, List<ReviewImage> reviewImages, UserProfile userProfile) {
        this(review, reviewImages, null, userProfile);
    }
}
