-- 锁定库存：下单时调用，原子性地检查库存并扣减可用库存
-- KEYS[1]: stock key (stock:sku:{skuId})
-- ARGV[1]: quantity to lock
-- 返回值: 1=成功, 0=库存不足, -1=库存未初始化

local stock = tonumber(redis.call('get', KEYS[1]))
if stock == nil then
    return -1  -- stock key not found (not initialized)
end
if stock >= tonumber(ARGV[1]) then
    redis.call('decrby', KEYS[1], ARGV[1])
    return 1  -- lock success
else
    return 0  -- insufficient stock
end
