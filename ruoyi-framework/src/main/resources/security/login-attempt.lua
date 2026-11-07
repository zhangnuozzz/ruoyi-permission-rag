local ttl = redis.call('TTL', KEYS[2])
if ttl > 0 then return ttl end
if ARGV[1] == 'check' then return 0 end
if ARGV[1] == 'success' then
    redis.call('DEL', KEYS[1])
    return 0
end
local attempts = redis.call('INCR', KEYS[1])
if attempts == 1 then redis.call('EXPIRE', KEYS[1], ARGV[3]) end
if attempts >= tonumber(ARGV[2]) then
    redis.call('SET', KEYS[2], '1', 'EX', ARGV[4])
    redis.call('DEL', KEYS[1])
    return tonumber(ARGV[4])
end
return 0
