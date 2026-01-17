package hr.lknezevic.barberbot.worker;

import java.util.function.Supplier;

public interface Worker {
    void process(Supplier<Boolean> runningSupplier);
}
