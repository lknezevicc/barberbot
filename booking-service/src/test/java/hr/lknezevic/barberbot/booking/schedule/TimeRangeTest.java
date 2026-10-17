package hr.lknezevic.barberbot.booking.schedule;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeRangeTest {

    private final TimeRange range = new TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0));

    @Test
    void contains_timeInsideRange_returnsTrue() {
        assertThat(range.contains(LocalTime.of(12, 0))).isTrue();
    }

    @Test
    void contains_timeAtStartBoundary_returnsTrue() {
        assertThat(range.contains(LocalTime.of(9, 0))).isTrue();
    }

    @Test
    void contains_timeAtEndBoundary_returnsFalse() {
        assertThat(range.contains(LocalTime.of(17, 0))).isFalse();
    }

    @Test
    void contains_timeBeforeRange_returnsFalse() {
        assertThat(range.contains(LocalTime.of(8, 59))).isFalse();
    }

    @Test
    void contains_timeAfterRange_returnsFalse() {
        assertThat(range.contains(LocalTime.of(17, 1))).isFalse();
    }

    @Test
    void fitsEntirely_slotFitsWithinRange_returnsTrue() {
        assertThat(range.fitsEntirely(LocalTime.of(10, 0), LocalTime.of(11, 0))).isTrue();
    }

    @Test
    void fitsEntirely_slotStartsBeforeRange_returnsFalse() {
        assertThat(range.fitsEntirely(LocalTime.of(8, 30), LocalTime.of(10, 0))).isFalse();
    }

    @Test
    void fitsEntirely_slotEndsAfterRange_returnsFalse() {
        assertThat(range.fitsEntirely(LocalTime.of(16, 0), LocalTime.of(17, 30))).isFalse();
    }

    @Test
    void fitsEntirely_exactBoundariesMatch_returnsTrue() {
        assertThat(range.fitsEntirely(LocalTime.of(9, 0), LocalTime.of(17, 0))).isTrue();
    }
}
