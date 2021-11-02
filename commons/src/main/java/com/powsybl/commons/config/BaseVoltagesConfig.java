/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.commons.config;

import com.powsybl.commons.PowsyblException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Massimo Ferraro <massimo.ferraro@techrain.eu>
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class BaseVoltagesConfig {

    private static final String CONFIG_FILE = "base-voltages.yml";

    private List<BaseVoltageConfig> baseVoltages = new ArrayList<>();
    private String defaultProfile;

    public List<BaseVoltageConfig> getBaseVoltages() {
        return baseVoltages;
    }

    public void setBaseVoltages(List<BaseVoltageConfig> baseVoltages) {
        this.baseVoltages = baseVoltages == null ? Collections.emptyList() : baseVoltages;
    }

    public String getDefaultProfile() {
        return defaultProfile;
    }

    public void setDefaultProfile(String defaultProfile) {
        this.defaultProfile = Objects.requireNonNull(defaultProfile);
    }

    public static BaseVoltagesConfig fromPlatformConfig() {
        return fromPlatformConfig(PlatformConfig.defaultConfig());
    }

    public static BaseVoltagesConfig fromPlatformConfig(PlatformConfig platformConfig) {
        Path path = platformConfig.getConfigDir().resolve(CONFIG_FILE);
        if (!Files.exists(path)) {
            throw new PowsyblException("No base voltages configuration found");
        }
        return fromPath(path);
    }

    public static BaseVoltagesConfig fromInputStream(InputStream configInputStream) {
        Objects.requireNonNull(configInputStream);
        Yaml yaml = new Yaml(new BaseVoltagesConfigConstructor());
        return yaml.load(configInputStream);
    }

    public static BaseVoltagesConfig fromPath(Path configFile) {
        Objects.requireNonNull(configFile);
        try (InputStream configInputStream = Files.newInputStream(configFile)) {
            return fromInputStream(configInputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<String> getProfiles() {
        return getBaseVoltages()
                .stream()
                .map(BaseVoltageConfig::getProfile)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getBaseVoltageNames(String profile) {
        Objects.requireNonNull(profile);
        return getBaseVoltages()
                .stream()
                .filter(baseVoltage -> baseVoltage.getProfile().equals(profile))
                .map(BaseVoltageConfig::getName)
                .collect(Collectors.toList());
    }

    public Optional<String> getBaseVoltageName(double baseVoltage, String profile) {
        Objects.requireNonNull(profile);
        return getBaseVoltages()
                .stream()
                .filter(v -> v.getProfile().equals(profile)
                        && v.getMinValue() <= baseVoltage
                        && v.getMaxValue() > baseVoltage)
                .map(BaseVoltageConfig::getName)
                .findFirst();
    }

    private static class BaseVoltagesConfigConstructor extends Constructor {
        private static final List<String> BASE_VOLTAGES_CONFIG_REQUIRED_FIELDS = Arrays.asList("baseVoltages", "defaultProfile");
        private static final List<String> BASE_VOLTAGE_CONFIG_REQUIRED_FIELDS = Arrays.asList("name", "minValue", "maxValue", "profile");

        BaseVoltagesConfigConstructor() {
            super(BaseVoltagesConfig.class);
        }

        @Override
        protected Object constructObject(Node node) {
            if (node.getTag().equals(rootTag)) {
                checkRequiredFields(node, new LinkedList<>(BASE_VOLTAGES_CONFIG_REQUIRED_FIELDS), BaseVoltagesConfig.class);
            } else if (node.getType().equals(BaseVoltageConfig.class)) {
                checkRequiredFields(node, new LinkedList<>(BASE_VOLTAGE_CONFIG_REQUIRED_FIELDS), node.getType());
            }
            return super.constructObject(node);
        }

        private void checkRequiredFields(Node node, List<String> requiredFields, Class<?> aClass) {
            if (node instanceof MappingNode) {
                for (NodeTuple nodeTuple : ((MappingNode) node).getValue()) {
                    Node keyNode = nodeTuple.getKeyNode();
                    if (keyNode instanceof ScalarNode) {
                        requiredFields.remove(((ScalarNode) keyNode).getValue());
                    }
                }
            }
            if (!requiredFields.isEmpty()) {
                throw new YAMLException(aClass + " is missing " + String.join(", ", requiredFields));
            }
        }
    }
}