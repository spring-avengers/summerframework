local key     = KEYS[1]
local content = KEYS[2]
local ttl     = ARGV[1]
local isexist = redis.call("exists",key)
if(isexist == 1) then
    redis.call('pexpire', key, ttl)    
end
return 1