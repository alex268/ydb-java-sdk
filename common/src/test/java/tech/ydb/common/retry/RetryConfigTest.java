package tech.ydb.common.retry;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class RetryConfigTest {
    private void assertDuration(long from, long to, long ms) {
        Assert.assertTrue("time " + ms + " must be great than " + from, from <= ms);
        Assert.assertTrue("time " + ms + " must be lower than " + to, to >= ms);
    }

    @Test
    public void nullStatusesTest() {
        RetryConfig config = RetryConfig.retryForever();

        Assert.assertNull(config.isThrowableRetryable(null));
        Assert.assertNull(config.isStatusRetryable(null));
    }

    @Test
    public void throwableRetriesTest() {
        RetryConfig config = RetryConfig.retryUntilElapsed(1000);

        Assert.assertNull(config.isThrowableRetryable(new RuntimeException("test message")));
        Assert.assertNull(config.isThrowableRetryable(new Exception("1", new RuntimeException("2"))));

        RetryPolicy immediatelly = config.isStatusRetryable(StatusCode.BAD_SESSION);
        RetryPolicy fast = config.isStatusRetryable(StatusCode.NOT_FOUND);

        Assert.assertEquals(immediatelly, config.isThrowableRetryable(
                new UnexpectedResultException("base", Status.of(StatusCode.BAD_SESSION)))
        );
        Assert.assertEquals(immediatelly, config.isThrowableRetryable(new Exception("base",
                new UnexpectedResultException("cause", Status.of(StatusCode.SESSION_BUSY)))
        ));
        Assert.assertEquals(fast, config.isThrowableRetryable(new Exception("base",
                new UnexpectedResultException("cause", Status.of(StatusCode.NOT_FOUND)))
        ));
    }

    @Test
    public void noRetryPolicyTest() {
        RetryConfig config = RetryConfig.noRetries();
        // unretrayable
        for (StatusCode code: StatusCode.values()) {
            Assert.assertNull(config.isStatusRetryable(code));
        }
    }

    @Test
    public void nonIdempotentRetryPolicyTest() {
        RetryConfig config = RetryConfig.retryForever();

        // unretrayable
        Assert.assertNull(config.isStatusRetryable(StatusCode.SCHEME_ERROR));
        Assert.assertNull(config.isStatusRetryable(StatusCode.ALREADY_EXISTS));
        Assert.assertNull(config.isStatusRetryable(StatusCode.UNAUTHORIZED));
        Assert.assertNull(config.isStatusRetryable(StatusCode.UNAVAILABLE));
        Assert.assertNull(config.isStatusRetryable(StatusCode.TRANSPORT_UNAVAILABLE));
        Assert.assertNull(config.isStatusRetryable(StatusCode.CLIENT_CANCELLED));
        Assert.assertNull(config.isStatusRetryable(StatusCode.CLIENT_INTERNAL_ERROR));
        Assert.assertNull(config.isStatusRetryable(StatusCode.NOT_FOUND));

        RetryPolicy immediatelly = config.isStatusRetryable(StatusCode.BAD_SESSION);
        Assert.assertNotNull(immediatelly);
        Assert.assertEquals(immediatelly, config.isStatusRetryable(StatusCode.SESSION_BUSY));

        RetryPolicy fast = config.isStatusRetryable(StatusCode.ABORTED);
        Assert.assertNotNull(fast);
        Assert.assertEquals(fast, config.isStatusRetryable(StatusCode.UNDETERMINED));

        RetryPolicy slow = config.isStatusRetryable(StatusCode.OVERLOADED);
        Assert.assertNotNull(slow);
        Assert.assertEquals(slow, config.isStatusRetryable(StatusCode.CLIENT_RESOURCE_EXHAUSTED));
    }

    @Test
    public void idempotentRetryPolicyTest() {
        RetryConfig config = RetryConfig.idempotentRetryForever();

        // unretrayable
        Assert.assertNull(config.isStatusRetryable(StatusCode.SCHEME_ERROR));
        Assert.assertNull(config.isStatusRetryable(StatusCode.ALREADY_EXISTS));
        Assert.assertNull(config.isStatusRetryable(StatusCode.UNAUTHORIZED));
        Assert.assertNull(config.isStatusRetryable(StatusCode.NOT_FOUND));

        RetryPolicy immediatelly = config.isStatusRetryable(StatusCode.BAD_SESSION);
        Assert.assertNotNull(immediatelly);
        Assert.assertEquals(immediatelly, config.isStatusRetryable(StatusCode.SESSION_BUSY));

        RetryPolicy fast = config.isStatusRetryable(StatusCode.ABORTED);
        Assert.assertNotNull(fast);
        Assert.assertEquals(fast, config.isStatusRetryable(StatusCode.UNDETERMINED));
        Assert.assertEquals(fast, config.isStatusRetryable(StatusCode.UNAVAILABLE));
        Assert.assertEquals(fast, config.isStatusRetryable(StatusCode.TRANSPORT_UNAVAILABLE));
        Assert.assertEquals(fast, config.isStatusRetryable(StatusCode.CLIENT_CANCELLED));
        Assert.assertEquals(fast, config.isStatusRetryable(StatusCode.CLIENT_INTERNAL_ERROR));

        RetryPolicy slow = config.isStatusRetryable(StatusCode.OVERLOADED);
        Assert.assertNotNull(slow);
        Assert.assertEquals(slow, config.isStatusRetryable(StatusCode.CLIENT_RESOURCE_EXHAUSTED));
    }

    @Test
    public void notFoundRetryPolicyTest() {
        RetryConfig config = RetryConfig.newConfig().retryNotFound(true).retryForever();

        RetryPolicy fast = config.isStatusRetryable(StatusCode.ABORTED);
        Assert.assertEquals(fast, config.isStatusRetryable(StatusCode.NOT_FOUND));
    }

    @Test
    public void foreverRetryTest() {
        RetryConfig config = RetryConfig.newConfig().withSlowBackoff(100, 5).withFastBackoff(10, 10).retryForever();

        RetryPolicy immediatelly = config.isStatusRetryable(StatusCode.BAD_SESSION);
        Assert.assertEquals(0, immediatelly.nextRetryMs(0, 0));
        Assert.assertEquals(0, immediatelly.nextRetryMs(0, Integer.MAX_VALUE));
        Assert.assertEquals(0, immediatelly.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(0, immediatelly.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));

        RetryPolicy fast = config.isStatusRetryable(StatusCode.ABORTED);
        assertDuration(10, 20, fast.nextRetryMs(0, 0));
        assertDuration(10, 20, fast.nextRetryMs(0, Integer.MAX_VALUE));
        assertDuration(10240, 20480, fast.nextRetryMs(Integer.MAX_VALUE, 0));
        assertDuration(10240, 20480, fast.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));

        RetryPolicy slow = config.isStatusRetryable(StatusCode.OVERLOADED);
        assertDuration(100, 200, slow.nextRetryMs(0, 0));
        assertDuration(100, 200, slow.nextRetryMs(0, Integer.MAX_VALUE));
        assertDuration(3200, 6400, slow.nextRetryMs(Integer.MAX_VALUE, 0));
        assertDuration(3200, 6400, slow.nextRetryMs(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void untilElapsedRetryTest() {
        RetryConfig config = RetryConfig.idempotentRetryUntilElapsed(5000);

        RetryPolicy immediatelly = config.isStatusRetryable(StatusCode.BAD_SESSION);
        Assert.assertEquals(0, immediatelly.nextRetryMs(0, 0));
        Assert.assertEquals(0, immediatelly.nextRetryMs(0, 5000));
        Assert.assertEquals(0, immediatelly.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(0, immediatelly.nextRetryMs(Integer.MAX_VALUE, 5000));
        Assert.assertEquals(-1, immediatelly.nextRetryMs(0, 5001));
        Assert.assertEquals(-1, immediatelly.nextRetryMs(Integer.MAX_VALUE, 5001));

        RetryPolicy fast = config.isStatusRetryable(StatusCode.ABORTED);
        assertDuration(5, 10, fast.nextRetryMs(0, 0));
        Assert.assertEquals(3, fast.nextRetryMs(0, 4997));
        Assert.assertEquals(5000, fast.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(1, fast.nextRetryMs(Integer.MAX_VALUE, 4999));
        Assert.assertEquals(-1, fast.nextRetryMs(Integer.MAX_VALUE, 5000));

        RetryPolicy slow = config.isStatusRetryable(StatusCode.OVERLOADED);
        assertDuration(500, 1000, slow.nextRetryMs(0, 0));
        Assert.assertEquals(3, slow.nextRetryMs(0, 4997));
        Assert.assertEquals(5000, slow.nextRetryMs(Integer.MAX_VALUE, 0));
        Assert.assertEquals(1, slow.nextRetryMs(Integer.MAX_VALUE, 4999));
        Assert.assertEquals(-1, slow.nextRetryMs(Integer.MAX_VALUE, 5000));
    }

    @Test
    public void nTimesRetryTest() {
        RetryConfig config = RetryConfig.newConfig().retryNTimes(8);

        RetryPolicy immediatelly = config.isStatusRetryable(StatusCode.BAD_SESSION);
        Assert.assertEquals(0, immediatelly.nextRetryMs(0, 0));
        Assert.assertEquals(0, immediatelly.nextRetryMs(0, Integer.MAX_VALUE));
        Assert.assertEquals(0, immediatelly.nextRetryMs(7, 0));
        Assert.assertEquals(0, immediatelly.nextRetryMs(7, Integer.MAX_VALUE));
        Assert.assertEquals(-1, immediatelly.nextRetryMs(8, 0));
        Assert.assertEquals(-1, immediatelly.nextRetryMs(8, Integer.MAX_VALUE));

        RetryPolicy fast = config.isStatusRetryable(StatusCode.ABORTED);
        assertDuration(5, 10, fast.nextRetryMs(0, 0));
        assertDuration(5, 10, fast.nextRetryMs(0, Integer.MAX_VALUE));
        assertDuration(5 * 128, 5 * 256, fast.nextRetryMs(7, 0));
        assertDuration(5 * 128, 5 * 256, fast.nextRetryMs(7, Integer.MAX_VALUE));
        Assert.assertEquals(-1, fast.nextRetryMs(8, 0));
        Assert.assertEquals(-1, fast.nextRetryMs(8, Integer.MAX_VALUE));

        RetryPolicy slow = config.isStatusRetryable(StatusCode.OVERLOADED);
        assertDuration(500, 1000, slow.nextRetryMs(0, 0));
        assertDuration(500, 1000, slow.nextRetryMs(0, Integer.MAX_VALUE));
        assertDuration(500 * 64, 500 * 128, slow.nextRetryMs(7, 0));
        assertDuration(500 * 64, 500 * 128, slow.nextRetryMs(7, Integer.MAX_VALUE));
        Assert.assertEquals(-1, slow.nextRetryMs(8, 0));
        Assert.assertEquals(-1, slow.nextRetryMs(8, Integer.MAX_VALUE));
    }
}