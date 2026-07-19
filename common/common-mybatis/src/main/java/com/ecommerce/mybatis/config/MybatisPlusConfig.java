package com.ecommerce.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ecommerce.core.constant.GlobalConstants;
import com.ecommerce.core.model.UserContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus配置类 —— 统一注册 MP 插件。
 * <p>
 * 插件链顺序（按官方推荐）：
 *   1. 多租户 TenantLineInnerInterceptor
 *   2. 乐观锁 OptimisticLockerInnerInterceptor
 *   3. 分页 PaginationInnerInterceptor
 * <p>
 * 商户隔离策略：
 *   - B端接口（/api/admin/**）自动注入 merchant_id 条件
 *   - ROLE_ADMIN 传 X-Merchant-Id=ALL 时跳过租户过滤
 *   - C端接口不注入（用户无需关心商户归属）
 * <p>
 * 注意：实体字段必须加 @Version 注解才能触发乐观锁逻辑；
 * 若缺失拦截器，updateById 将抛出 BindingException: MP_OPTLOCK_VERSION_ORIGINAL not found。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 多租户：自动为 SQL 注入 merchant_id 条件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new CustomTenantHandler()));
        // 2. 乐观锁：支持 version 字段自动对比与自增（注入 MP_OPTLOCK_VERSION_ORIGINAL 旧版本参数）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 3. 分页：MySQL 方言
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自定义租户处理器：从 UserContext 获取当前商户ID
     * <p>
     * 忽略条件：
     *   - merchantId 为 null（ROLE_ADMIN 传 ALL 或 C端用户）
     *   - 当前请求路径非 /api/admin/**（C端接口不隔离）
     */
    static class CustomTenantHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

        @Override
        public Expression getTenantId() {
            Long merchantId = UserContext.currentMerchantId();
            if (merchantId == null) {
                return null;
            }
            return new LongValue(merchantId);
        }

        @Override
        public String getTenantIdColumn() {
            return "merchant_id";
        }

        @Override
        public boolean ignoreTable(String tableName) {
            // merchantId 为 null（C端用户）时，所有表都不隔离
            if (UserContext.currentMerchantId() == null) {
                return true;
            }
            String upperTableName = tableName.toUpperCase();
            // 优惠券表不做商户隔离，由平台统一发放
            if ("T_COUPON_TEMPLATE".equals(upperTableName)
                    || "T_USER_COUPON".equals(upperTableName)) {
                return true;
            }
            // merchantId 不为 null（B端商户）时，只对以下表做商户隔离
            // 返回 true = 不隔离，返回 false = 需要隔离
            return !("T_PRODUCT".equals(upperTableName)
                    || "T_SKU".equals(upperTableName)
                    || "T_STOCK".equals(upperTableName)
                    || "T_STOCK_LOG".equals(upperTableName)
                    || "T_STOCK_PRE_LOCK".equals(upperTableName)
                    || "T_ORDER".equals(upperTableName)
                    || "T_ORDER_ITEM".equals(upperTableName));
        }
    }
}
