package com.xiaoshan.springbootdemo.entity.vo;

import com.xiaoshan.springbootdemo.entity.Review;
import com.xiaoshan.springbootdemo.entity.ReviewImage;
import com.xiaoshan.springbootdemo.entity.UserProfile;
import lombok.Data;

import java.util.List;

@Data
public class UserReviewVO {

    private Review review;

    private List<ReviewImage> reviewImages;

    private UserProfile userProfile;

    public UserReviewVO(Review review, List<ReviewImage> reviewImages, UserProfile userProfile) {
        this.review = review;
        this.reviewImages = reviewImages;
        this.userProfile = userProfile;
    }
}
