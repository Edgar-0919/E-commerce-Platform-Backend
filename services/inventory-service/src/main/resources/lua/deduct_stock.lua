-- 确认扣减：支付成功后调用，清除锁定记录
-- KEYS[1]: lock key (stock:lock:{orderId}:{skuId})
-- ARGV[1]: quantity to deduct
-- 注意：stock key 在锁定时已扣减，此处只清理 lock key，DB 持久化由 Java 层完成

local locked = tonumber(redis.call('get', KEYS[1]))
if locked == nil or locked < tonumber(ARGV[1]) then
    return -1
end
redis.call('decrby', KEYS[1], ARGV[1])
return 1
