package com.bkjk.platform.monitor.metric.micrometer.binder.jvm;

import static java.util.Collections.emptyList;

import java.io.File;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

public class DiskSpaceMetrics implements MeterBinder {
    private final Iterable<Tag> tags;
    private final File path;
    private final String absolutePath;

    public DiskSpaceMetrics(File path) {
        this(path, emptyList());
    }

    public DiskSpaceMetrics(File path, Iterable<Tag> tags) {
        this.path = path;
        this.absolutePath = path.getAbsolutePath();
        this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Iterable<Tag> tagsWithPath = Tags.concat(tags, "path", absolutePath);
        Gauge.builder("disk.free", path, File::getUsableSpace).tags(tagsWithPath).description("Usable space for path")
            .baseUnit("bytes").register(registry);
        Gauge.builder("disk.total", path, File::getTotalSpace).tags(tagsWithPath).description("Total space for path")
            .baseUnit("bytes").register(registry);
    }
}
