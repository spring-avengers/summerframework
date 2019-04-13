package com.bkjk.platform.monitor.logging.appender.log4j.layout;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "CustomJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class CustomJsonLayout extends AbstractJacksonLayout {

    public static final String DEFAULT_FOOTER = "]";

    public static final String DEFAULT_HEADER = "[";

    public static final String CONTENT_TYPE = "application/json";

    public static CustomJsonLayout createDefaultLayout() {
        return new CustomJsonLayout(new DefaultConfiguration(), false, false, false, false, false, false,
            DEFAULT_HEADER, DEFAULT_FOOTER, StandardCharsets.UTF_8, true, false, false);
    }

    @PluginFactory
    public static CustomJsonLayout createLayout(@PluginConfiguration final Configuration config,
        @PluginAttribute(value = "locationInfo", defaultBoolean = false) final boolean locationInfo,
        @PluginAttribute(value = "properties", defaultBoolean = false) final boolean properties,
        @PluginAttribute(value = "propertiesAsList", defaultBoolean = false) final boolean propertiesAsList,
        @PluginAttribute(value = "complete", defaultBoolean = false) final boolean complete,
        @PluginAttribute(value = "compact", defaultBoolean = false) final boolean compact,
        @PluginAttribute(value = "eventEol", defaultBoolean = false) final boolean eventEol,
        @PluginAttribute(value = "header", defaultString = DEFAULT_HEADER) final String headerPattern,
        @PluginAttribute(value = "footer", defaultString = DEFAULT_FOOTER) final String footerPattern,
        @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charset,
        @PluginAttribute(value = "includeStacktrace", defaultBoolean = true) final boolean includeStacktrace,
        @PluginAttribute(value = "stacktraceAsString", defaultBoolean = false) final boolean stacktraceAsString,
        @PluginAttribute(value = "objectMessageAsJsonObject",
            defaultBoolean = false) final boolean objectMessageAsJsonObject) {
        final boolean encodeThreadContextAsList = properties && propertiesAsList;
        return new CustomJsonLayout(config, locationInfo, properties, encodeThreadContextAsList, complete, compact,
            eventEol, headerPattern, footerPattern, charset, includeStacktrace, stacktraceAsString,
            objectMessageAsJsonObject);
    }

    public CustomJsonLayout(final Configuration config, final boolean locationInfo, final boolean properties,
        final boolean encodeThreadContextAsList, final boolean complete, final boolean compact, final boolean eventEol,
        final String headerPattern, final String footerPattern, final Charset charset, final boolean includeStacktrace,
        boolean stacktraceAsString, boolean objectMessageAsJsonObject) {
        super(config,
            new JacksonFactory.JSON(encodeThreadContextAsList, includeStacktrace, stacktraceAsString,
                objectMessageAsJsonObject).newWriter(locationInfo, properties, compact),
            charset, compact, complete, eventEol,
            PatternLayout.createSerializer(config, null, headerPattern, DEFAULT_HEADER, null, false, false),
            PatternLayout.createSerializer(config, null, footerPattern, DEFAULT_FOOTER, null, false, false));
    }

    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("version", "2.0");
        return result;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    @Override
    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(this.eol);
        final String str = serializeToString(getFooterSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    @Override
    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        final String str = serializeToString(getHeaderSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    @Override
    public void toSerializable(final LogEvent event, final Writer writer) throws IOException {
        if (complete && eventCount > 0) {
            writer.append(", ");
        }
        super.toSerializable(event, writer);
    }

}
