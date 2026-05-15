package com.ecommerce.core.exception;

import com.ecommerce.core.constant.ResultCodeEnum;

/**
 * 库存异常类
 * 用于封装库存相关的异常，如库存不足、库存锁定失败等
 */
public class InventoryException extends BusinessException {

    public InventoryException(ResultCodeEnum resultCode) {
        super(resultCode);
    }

    public InventoryException(String message) {
        super(ResultCodeEnum.STOCK_INSUFFICIENT.getCode(), message);
    }
}
