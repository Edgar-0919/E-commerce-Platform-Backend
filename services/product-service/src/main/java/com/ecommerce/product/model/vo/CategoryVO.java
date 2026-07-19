package com.ecommerce.product.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.List;

@Data
public class CategoryVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String name;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parentId;
    private Integer level;
    private Integer sort;
    private String icon;
    private List<CategoryVO> children;
}
