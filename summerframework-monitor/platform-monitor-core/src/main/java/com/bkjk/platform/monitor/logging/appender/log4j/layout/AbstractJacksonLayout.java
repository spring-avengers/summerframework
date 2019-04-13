package com.bkjk.platform.monitor.logging.appender.log4j.layout;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectWriter;

public class AbstractJacksonLayout extends AbstractStringLayout {

    protected static final String DEFAULT_EOL = "\r\n";
    protected static final String COMPACT_EOL = Strings.EMPTY;

    private static LogEvent convertMutableToLog4jEvent(final LogEvent event) {
        return event instanceof MutableLogEvent ? ((MutableLogEvent)event).createMemento() : event;
    }

    protected final String eol;
    protected final ObjectWriter objectWriter;
    protected final boolean compact;

    protected final boolean complete;

    protected AbstractJacksonLayout(final Configuration config, final ObjectWriter objectWriter, final Charset charset,
        final boolean compact, final boolean complete, final boolean eventEol, final Serializer headerSerializer,
        final Serializer footerSerializer) {
        super(config, charset, headerSerializer, footerSerializer);
        this.objectWriter = objectWriter;
        this.compact = compact;
        this.complete = complete;
        this.eol = compact && !eventEol ? COMPACT_EOL : DEFAULT_EOL;
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilderWriter writer = new StringBuilderWriter();
        try {
            toSerializable(event, writer);
            return writer.toString();
        } catch (final IOException e) {
            LOGGER.error(e);
            return Strings.EMPTY;
        }
    }

    public void toSerializable(final LogEvent event, final Writer writer)
        throws JsonGenerationException, JsonMappingException, IOException {
        objectWriter.writeValue(writer, convertMutableToLog4jEvent(event));
        writer.write(eol);
        markEvent();
    }

}
