package com.ecommerce.user.service;

import com.ecommerce.core.model.PageResult;
import com.ecommerce.user.model.dto.MerchantApplicationDTO;
import com.ecommerce.user.model.dto.ReviewApplicationDTO;
import com.ecommerce.user.model.vo.MerchantApplicationVO;

public interface MerchantApplicationService {

    void submitApplication(MerchantApplicationDTO dto, Long userId);

    MerchantApplicationVO getApplicationByUserId(Long userId);

    MerchantApplicationVO getApplicationById(Long id);

    PageResult<MerchantApplicationVO> page(Integer pageNum, Integer pageSize, Integer status);

    void review(Long id, ReviewApplicationDTO dto, Long reviewerId);
}