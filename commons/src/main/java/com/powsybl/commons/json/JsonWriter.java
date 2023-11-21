/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.commons.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.commons.io.TreeDataWriter;
import com.powsybl.commons.json.JsonUtil.Context;
import com.powsybl.commons.json.JsonUtil.ContextType;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class JsonWriter implements TreeDataWriter {

    public static final String VERSION = "version";
    private static final String EXTENSION_VERSIONS = "extensionVersions";
    private final JsonGenerator jsonGenerator;
    private final String rootVersion;
    private Map<String, String> extensionVersions;

    private final Deque<Context> contextQueue = new ArrayDeque<>();

    public JsonWriter(OutputStream os, boolean indent, String rootVersion) throws IOException {
        this.jsonGenerator = JsonUtil.createJsonFactory().createGenerator(os);
        if (indent) {
            jsonGenerator.useDefaultPrettyPrinter();
        }
        this.rootVersion = Objects.requireNonNull(rootVersion);
    }

    @Override
    public void setVersions(Map<String, String> extensionVersions) {
        this.extensionVersions = Objects.requireNonNull(extensionVersions);
    }

    @Override
    public void writeStartNodes(String name) {
        contextQueue.push(new Context(ContextType.ARRAY, name));
    }

    @Override
    public void writeEndNodes() {
        try {
            Context context = Objects.requireNonNull(contextQueue.pop());
            if (context.getType() != ContextType.ARRAY) {
                throw new IllegalStateException();
            }
            if (context.getObjectCount() > 0) {
                jsonGenerator.writeEndArray();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeStartNode(String namespace, String name) {
        try {
            Context context = contextQueue.peekFirst();
            if (context != null) {
                if (context.getType() == ContextType.ARRAY) {
                    if (context.getObjectCount() == 0) {
                        jsonGenerator.writeFieldName(context.getFieldName());
                        jsonGenerator.writeStartArray();
                    }
                    context.incrementObjectCount();
                } else if (context.getType() == ContextType.OBJECT) {
                    jsonGenerator.writeFieldName(name);
                }
                jsonGenerator.writeStartObject();
            } else {
                jsonGenerator.writeStartObject();
                writeStringAttribute(VERSION, rootVersion);
                writeStartNodes(EXTENSION_VERSIONS);
                extensionVersions.forEach((extensionName, version) -> {
                    writeStartNode("", "");
                    writeStringAttribute("extensionName", extensionName);
                    writeStringAttribute(VERSION, version);
                    writeEndNode();
                });
                writeEndNodes();

            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        contextQueue.push(new Context(ContextType.OBJECT, name));
    }

    @Override
    public void writeEndNode() {
        try {
            jsonGenerator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        contextQueue.pop();
    }

    @Override
    public void writeNamespace(String prefix, String namespace) {
        // nothing to do
    }

    @Override
    public void writeNodeContent(String value) {
        writeStringAttribute("content", value);
    }

    @Override
    public void writeStringAttribute(String name, String value) {
        try {
            if (value != null) {
                jsonGenerator.writeStringField(name, value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeFloatAttribute(String name, float value) {
        try {
            if (!Float.isNaN(value)) {
                jsonGenerator.writeNumberField(name, value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeDoubleAttribute(String name, double value) {
        try {
            if (!Double.isNaN(value)) {
                jsonGenerator.writeNumberField(name, value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeDoubleAttribute(String name, double value, double absentValue) {
        if (value != absentValue) {
            writeDoubleAttribute(name, value);
        }
    }

    @Override
    public void writeIntAttribute(String name, int value) {
        try {
            jsonGenerator.writeNumberField(name, value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeIntAttribute(String name, int value, int absentValue) {
        if (value != absentValue) {
            writeIntAttribute(name, value);
        }
    }

    @Override
    public void writeIntArrayAttribute(String name, Collection<Integer> values) {
        try {
            jsonGenerator.writeFieldName(name);
            jsonGenerator.writeStartArray();
            for (int value : values) {
                jsonGenerator.writeNumber(value);
            }
            jsonGenerator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <E extends Enum<E>> void writeEnumAttribute(String name, E value) {
        if (value != null) {
            writeStringAttribute(name, value.name());
        }
    }

    @Override
    public void writeBooleanAttribute(String name, boolean value) {
        try {
            jsonGenerator.writeBooleanField(name, value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeBooleanAttribute(String name, boolean value, boolean absentValue) {
        if (value != absentValue) {
            writeBooleanAttribute(name, value);
        }
    }

    @Override
    public void close() {
        try {
            jsonGenerator.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
