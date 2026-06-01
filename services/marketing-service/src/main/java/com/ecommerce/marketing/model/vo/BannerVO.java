package com.ecommerce.marketing.model.vo;

import lombok.Data;

@Data
public class BannerVO {
    private Long id;
    private String image;
    private String title;
    private String description;
    private String linkUrl;
}