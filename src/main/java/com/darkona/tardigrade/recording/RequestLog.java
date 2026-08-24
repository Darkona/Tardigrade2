package com.darkona.tardigrade.recording;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Every request the server answered, newest last. This is what turns Tardigrade from
 * something you watch into something you can assert on.
 *
 * <p>Bounded on purpose: a server left running for days must not grow without limit.
 * When the bound is reached the oldest request is dropped.
 */
public class RequestLog {

    public static final int DEFAULT_CAPACITY = 1000;

    private final ConcurrentLinkedDeque<RecordedRequest> recorded = new ConcurrentLinkedDeque<>();
    private final int capacity;

    public RequestLog() {
        this(DEFAULT_CAPACITY);
    }

    public RequestLog(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1, was " + capacity);
        }
        this.capacity = capacity;
    }

    public void record(RecordedRequest request) {
        recorded.addLast(request);
        while (recorded.size() > capacity) {
            recorded.pollFirst();
        }
    }

    /** Everything recorded so far, oldest first. */
    public List<RecordedRequest> all() {
        return List.copyOf(recorded);
    }

    public List<RecordedRequest> forPath(String path) {
        List<RecordedRequest> matches = new ArrayList<>();
        for (RecordedRequest request : recorded) {
            if (request.path().equals(path)) {
                matches.add(request);
            }
        }
        return List.copyOf(matches);
    }

    public Optional<RecordedRequest> last() {
        return Optional.ofNullable(recorded.peekLast());
    }

    /** Last request that hit the given path, which is the usual thing to assert on. */
    public Optional<RecordedRequest> last(String path) {
        List<RecordedRequest> matches = forPath(path);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getLast());
    }

    public int count() {
        return recorded.size();
    }

    public int count(String path) {
        return forPath(path).size();
    }

    public boolean isEmpty() {
        return recorded.isEmpty();
    }

    /** Leaves the log empty, so one test does not see what another one sent. */
    public void clear() {
        recorded.clear();
    }
}
