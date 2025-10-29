package mry.ccdemos.helloattestation.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ExecutionTrail {
    private final Instant startAt = Instant.now();
    private final List<String> steps = new ArrayList<>();

    public void add(String step) {
        steps.add(step);
    }

    public Instant getStartAt() {
        return startAt;
    }

    public List<String> getSteps() {
        return steps;
    }
}
