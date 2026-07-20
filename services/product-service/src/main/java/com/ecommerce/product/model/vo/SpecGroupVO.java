package com.ecommerce.product.model.vo;

import lombok.Data;
import java.util.List;

@Data
public class SpecGroupVO {

    private Long id;
    private String name;
    private Long categoryId;
    private List<SpecParamVO> params;
}