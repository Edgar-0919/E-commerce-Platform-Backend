-- 释放库存：订单取消/超时时调用，退回锁定库存到可用库存
-- KEYS[1]: stock key
-- KEYS[2]: lock key (stock:lock:{orderId}:{skuId}) — 记录该订单锁定的数量
-- ARGV[1]: quantity to release
-- 注意：必须同时操作 lock key 和 stock key，保证数据一致性

local locked = tonumber(redis.call('get', KEYS[2]))
if locked == nil or locked < tonumber(ARGV[1]) then
    return -1  -- nothing to release
end
redis.call('decrby', KEYS[2], ARGV[1])
redis.call('incrby', KEYS[1], ARGV[1])
return 1
