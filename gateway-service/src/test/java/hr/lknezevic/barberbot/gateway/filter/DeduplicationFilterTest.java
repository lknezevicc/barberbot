package hr.lknezevic.barberbot.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeduplicationFilterTest {

    private final DeduplicationFilter filter = new DeduplicationFilter();

    @Test
    void firstCall_returnsNotDuplicate() {
        assertThat(filter.isDuplicate(100L)).isFalse();
    }

    @Test
    void secondCall_sameId_returnsDuplicate() {
        filter.isDuplicate(200L);
        assertThat(filter.isDuplicate(200L)).isTrue();
    }

    @Test
    void differentIds_notDuplicate() {
        filter.isDuplicate(300L);
        assertThat(filter.isDuplicate(301L)).isFalse();
    }

    @Test
    void evictExpired_removesOldEntries() throws Exception {
        filter.isDuplicate(400L);
        assertThat(filter.isDuplicate(400L)).isTrue();

        var seenField = DeduplicationFilter.class.getDeclaredField("seenUpdateIds");
        seenField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var map = (java.util.concurrent.ConcurrentHashMap<Long, java.time.Instant>) seenField.get(filter);
        map.put(400L, java.time.Instant.now().minusSeconds(120));

        filter.evictExpired();

        assertThat(filter.isDuplicate(400L)).isFalse();
    }

    @Test
    void multipleIds_trackedIndependently() {
        filter.isDuplicate(500L);
        filter.isDuplicate(501L);
        filter.isDuplicate(502L);

        assertThat(filter.isDuplicate(500L)).isTrue();
        assertThat(filter.isDuplicate(501L)).isTrue();
        assertThat(filter.isDuplicate(503L)).isFalse();
    }
}
