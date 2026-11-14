-- GCRA admission check, the same arithmetic as io.quarkus.http.ratelimit.runtime.internal.gcra.Gcra.tryConsume.
--
-- KEYS[1] the key; its value is the theoretical arrival time (TAT) in epoch microseconds
-- ARGV[1] the emission interval T in microseconds
-- ARGV[2] the burst
-- ARGV[3] the cost: 0 is a probe that writes nothing
--
-- Returns {allowed (0 or 1), remaining, retry_after_us}. A rejection writes nothing, unless it capped the debt, and
-- reports the permits available before the call as remaining.
--
-- Everything is in microseconds: Lua numbers are doubles, exact up to 2^53, which holds epoch microseconds but not
-- epoch nanoseconds. Every division has non-negative operands, so math.floor equals the Java truncation.
local t = tonumber(ARGV[1])
local burst = tonumber(ARGV[2])
local cost = tonumber(ARGV[3])
if cost > burst then
    return redis.error_reply('cost ' .. ARGV[3] .. ' exceeds the burst ' .. ARGV[2])
end
local tau = t * burst

local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000000 + tonumber(time[2])

-- debt = max(tat, now) - now; a missing key or a past tat is no state
local tat = tonumber(redis.call('GET', KEYS[1]))
local debt = 0
if tat ~= nil and tat > now then
    debt = tat - now
end
-- capped at tau: a larger debt was written under a larger policy, or by a clock that jumped forward, and would lock
-- the key out for the leftover debt, up to 30 days
local capped = debt > tau
if capped then
    debt = tau
end

local candidate = debt + math.max(cost, 1) * t
local allow_at = candidate - tau
if allow_at > 0 or cost == 0 then
    local available = math.floor((tau - debt) / t)
    if allow_at > 0 then
        if capped and cost > 0 then
            -- stored, so that the cap holds after this call too
            redis.call('SET', KEYS[1], string.format('%.0f', now + debt), 'PX',
                string.format('%.0f', math.max(1, math.ceil(debt / 1000))))
        end
        return {0, available, allow_at}
    end
    return {1, available, 0}
end

-- the key lives until its tat passes; %.0f writes plain integer digits whatever number conversion this Redis uses
redis.call('SET', KEYS[1], string.format('%.0f', now + candidate), 'PX',
    string.format('%.0f', math.max(1, math.ceil(candidate / 1000))))
return {1, math.floor(-allow_at / t), 0}
