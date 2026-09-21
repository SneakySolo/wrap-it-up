package com.wrapitup.wrap.cache;

import com.wrapitup.common.dto.WrapResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WrapCacheServiceTest {

    @Mock
    private RedisTemplate<String, WrapResponse> redisTemplate;

    @Mock
    private ValueOperations<String, WrapResponse> valueOps;

    private WrapCacheService service;
    private static final long DEFAULT_TTL_MINUTES = 15;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new WrapCacheService(redisTemplate, DEFAULT_TTL_MINUTES);
    }

    @Test
    void testSave_StoresWrapWithDefaultTTL() {
        String accountId = "user-123";
        WrapResponse wrap = createSampleWrap();

        service.save(accountId, wrap);

        verify(valueOps).set(
                "wrap:user-123",
                wrap,
                DEFAULT_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    @Test
    void testGet_ReturnsWrapWhenCached() {
        String accountId = "user-123";
        WrapResponse cached = createSampleWrap();
        when(valueOps.get("wrap:user-123")).thenReturn(cached);

        WrapResponse result = service.get(accountId);

        assertNotNull(result);
        assertEquals(cached, result);
        verify(valueOps).get("wrap:user-123");
    }

    @Test
    void testGet_ReturnsNullWhenNotCached() {
        String accountId = "user-123";
        when(valueOps.get("wrap:user-123")).thenReturn(null);

        WrapResponse result = service.get(accountId);

        assertNull(result);
        verify(valueOps).get("wrap:user-123");
    }

    @Test
    void testDelete_RemovesFromCache() {
        String accountId = "user-123";
        when(redisTemplate.delete("wrap:user-123")).thenReturn(true);

        service.delete(accountId);

        verify(redisTemplate).delete("wrap:user-123");
    }

    @Test
    void testExists_ReturnsTrueWhenCached() {
        String accountId = "user-123";
        when(redisTemplate.hasKey("wrap:user-123")).thenReturn(true);

        boolean exists = service.exists(accountId);

        assertTrue(exists);
        verify(redisTemplate).hasKey("wrap:user-123");
    }

    @Test
    void testExists_ReturnsFalseWhenNotCached() {
        String accountId = "user-123";
        when(redisTemplate.hasKey("wrap:user-123")).thenReturn(false);

        boolean exists = service.exists(accountId);

        assertFalse(exists);
        verify(redisTemplate).hasKey("wrap:user-123");
    }

    @Test
    void testSave_HandlesMultipleUsers() {
        WrapResponse wrap1 = createSampleWrap();
        WrapResponse wrap2 = createSampleWrap();

        service.save("user-123", wrap1);
        service.save("user-456", wrap2);

        verify(valueOps).set("wrap:user-123", wrap1, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
        verify(valueOps).set("wrap:user-456", wrap2, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Test
    void testSave_HandlesCachingException_DoesNotThrow() {
        String accountId = "user-123";
        WrapResponse wrap = createSampleWrap();
        doThrow(new RuntimeException("Redis unavailable"))
                .when(valueOps).set(anyString(), any(), anyLong(), any(TimeUnit.class));

        // Should not throw
        assertDoesNotThrow(() -> service.save(accountId, wrap));
    }

    @Test
    void testGet_HandlesRetrievalException_ReturnsNull() {
        String accountId = "user-123";
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis unavailable"));

        WrapResponse result = service.get(accountId);

        assertNull(result);
    }

    @Test
    void testCacheKeyFormat_IncludesPrefix() {
        String accountId = "spotify-user-abc123";
        WrapResponse wrap = createSampleWrap();

        service.save(accountId, wrap);

        verify(valueOps).set(
                "wrap:spotify-user-abc123",
                wrap,
                DEFAULT_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private WrapResponse createSampleWrap() {
        return WrapResponse.builder()
                .recentListeningMinutes(184)
                .topGenre("Hip-Hop")
                .artistLoyalty(82)
                .genreDiversity(74)
                .familiarityRatio(0.72)
                .discoveryRatio(0.28)
                .build();
    }
}