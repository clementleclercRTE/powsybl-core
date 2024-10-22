/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.ucte.converter;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.*;
import com.powsybl.ucte.network.UcteCountryCode;
import com.powsybl.ucte.network.UcteElementId;
import com.powsybl.ucte.network.UcteNodeCode;
import com.powsybl.ucte.network.UcteVoltageLevelCode;

import java.util.*;
import java.util.stream.StreamSupport;


/**
 * A {@link NamingStrategy} implementation that ensures the conformity of IDs with the UCTE-DEF format
 *
 * @author Mathieu Bague {@literal <mathieu.bague@rte-france.com>}
 */
@AutoService(NamingStrategy.class)
public class DefaultNamingStrategy implements NamingStrategy {

    private final Map<String, UcteNodeCode> ucteNodeIds = new HashMap<>();
    private final Map<String, UcteElementId> ucteElementIds = new HashMap<>();

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public void init(Network network) {
        convertToUcte(network);
        validateConversion(network);
    }

    private void convertToUcte(Network network) {
        //  bus
        network.getSubstationStream()
                .forEach(substation -> {
                    String countryCode = substation.getCountry()
                            .map(Country::getName)
                            .orElse("XX");

                    substation.getVoltageLevelStream()
                            .flatMap(vl -> StreamSupport.stream(vl.getBusBreakerView().getBuses().spliterator(), false))
                            .forEach(bus -> {
                                generateUcteNodeId(countryCode, bus);
                                System.out.println("BUS : " + bus.getId() + "--> " + ucteNodeIds.get(bus.getId()));
                            });
                });

        // lines
        network.getLineStream()
                .forEach(line -> {
                    generateUcteElementId(line);
                    System.out.println("LINE : " + line.getId() + "--> " + ucteElementIds.get(line.getId()));
                });

        // transformers
        network.getTwoWindingsTransformerStream()
                .forEach(transformer -> {
                    generateUcteElementId(transformer);
                    System.out.println("TwoWindingsTrans : " + transformer.getId() + "--> " + ucteElementIds.get(transformer.getId()));
                });

    }

    private void generateUcteNodeId(String country, Bus bus) {
        String busId = bus.getId();
        if (ucteNodeIds.containsKey(busId)) {
            return;
        }

        StringBuilder nameBuilder = new StringBuilder(8);
        nameBuilder.append(getCountryCode(country));

        String fomatedId = busId.length() >= 5
                ? busId.substring(0, 5)
                : String.format("%-5s", busId).replace(' ', '_');
        nameBuilder.append(fomatedId);
        nameBuilder.append(getVoltageLevelCode(bus.getVoltageLevel().getNominalV())).append('_');
        String name = nameBuilder.toString();

        UcteNodeCode nodeCode = UcteNodeCode.parseUcteNodeCode(name)
                .orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + name));
        ucteNodeIds.put(busId, nodeCode);
    }

    private void generateUcteElementId(TwoWindingsTransformer transformer) {
        String busId1 = transformer.getTerminal1().getBusBreakerView().getBus().getId();
        String busId2 = transformer.getTerminal2().getBusBreakerView().getBus().getId();

        if (!ucteNodeIds.containsKey(busId1) || !ucteNodeIds.containsKey(busId2)) {
            throw new UcteException(String.format(
                    transformer.getId(), busId1, busId2));
        }
        String originalId = new StringBuilder(busId1.length() + busId2.length() + 1)
                .append(busId1)
                .append('_')
                .append(busId2)
                .toString();

        if (ucteElementIds.containsKey(originalId)) {
            return;
        }
        UcteElementId elementId = new UcteElementId(
                ucteNodeIds.get(busId1),
                ucteNodeIds.get(busId2),
                '1'
        );

        ucteElementIds.computeIfAbsent(originalId, k -> UcteElementId.parseUcteElementId(elementId.toString()).orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + k)));

    }

    private void generateUcteElementId(Line line) {
        String busId1 = line.getTerminal1().getBusBreakerView().getBus().getId();
        String busId2 = line.getTerminal2().getBusBreakerView().getBus().getId();
        if (ucteNodeIds.containsKey(busId1) && ucteNodeIds.containsKey(busId2)) {
            UcteNodeCode nodeCode1 = ucteNodeIds.get(busId1);
            UcteNodeCode nodeCode2 = ucteNodeIds.get(busId2);
            char orderCode = line.getId().charAt(line.getId().length() - 1);
            UcteElementId id = new UcteElementId(nodeCode1, nodeCode2, orderCode);
            String originalId = busId1+"_"+busId2+"_"+orderCode;
            ucteElementIds.computeIfAbsent(originalId, k -> UcteElementId.parseUcteElementId(id.toString()).orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + k)));
        }
    }

    private static char getCountryCode(String code) {
         for(UcteCountryCode countryCode : UcteCountryCode.values()) {
             if(code.equalsIgnoreCase(countryCode.getPrettyName())) {
                 return countryCode.getUcteCode();
             }
         }
        return 'X';
    }

    public static char getVoltageLevelCode(double voltage) {
        for (UcteVoltageLevelCode code : UcteVoltageLevelCode.values()) {
            if (code.getVoltageLevel() == (int) voltage) {
                return (char) ('0' + code.ordinal());
            }
        }
        throw new IllegalArgumentException("No voltage level code found for " + voltage + " kV");
    }



    @Override
    public UcteNodeCode getUcteNodeCode(String id) {
        return ucteNodeIds.computeIfAbsent(id, k -> UcteNodeCode.parseUcteNodeCode(k).orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + k)));
    }

    @Override
    public UcteNodeCode getUcteNodeCode(Bus bus) {
        return getUcteNodeCode(bus.getId());
    }

    @Override
    public UcteNodeCode getUcteNodeCode(DanglingLine danglingLine) {
        return getUcteNodeCode(danglingLine.getPairingKey());
    }

    @Override
    public UcteElementId getUcteElementId(String id) {
        return ucteElementIds.computeIfAbsent(id, k -> UcteElementId.parseUcteElementId(k).orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + k)));
    }

    @Override
    public UcteElementId getUcteElementId(Switch sw) {
        return getUcteElementId(sw.getId());
    }

    @Override
    public UcteElementId getUcteElementId(Branch branch) {
        return getUcteElementId(branch.getId());
    }

    @Override
    public UcteElementId getUcteElementId(DanglingLine danglingLine) {
        return getUcteElementId(danglingLine.getId());
    }


    private void validateConversion(Network network) {
        System.out.println("\n=== UCTE Conversion Validation ===");
        boolean isValid = true;
        StringBuilder errors = new StringBuilder();

        // 1. Valider les bus
        Set<String> unconvertedBuses = new HashSet<>();
        for (VoltageLevel vl : network.getVoltageLevels()) {
            for (Bus bus : vl.getBusBreakerView().getBuses()) {
                String busId = bus.getId();
                if (!ucteNodeIds.containsKey(busId)) {
                    unconvertedBuses.add(busId);
                    isValid = false;
                }
            }
        }
        if (!unconvertedBuses.isEmpty()) {
            errors.append("Unconverted buses: ").append(unconvertedBuses).append("\n");
        }

        // 2. Valider les lignes
        Set<String> unconvertedLines = new HashSet<>();
        for (Line line : network.getLines()) {
            String lineId = line.getId();
            String bus1 = line.getTerminal1().getBusBreakerView().getBus().getId();
            String bus2 = line.getTerminal2().getBusBreakerView().getBus().getId();
            String originalId = bus1 + "_" + bus2 + "_" + line.getId().charAt(line.getId().length() - 1);

            if (!ucteElementIds.containsKey(originalId)) {
                unconvertedLines.add(lineId);
                isValid = false;
            }
        }
        if (!unconvertedLines.isEmpty()) {
            errors.append("Unconverted lines: ").append(unconvertedLines).append("\n");
        }

        // 3. Valider les transformateurs
        Set<String> unconvertedTransformers = new HashSet<>();
        for (TwoWindingsTransformer transformer : network.getTwoWindingsTransformers()) {
            String transformerId = transformer.getId();
            String bus1 = transformer.getTerminal1().getBusBreakerView().getBus().getId();
            String bus2 = transformer.getTerminal2().getBusBreakerView().getBus().getId();
            String originalId = bus1 + "_" + bus2;

            if (!ucteElementIds.containsKey(originalId)) {
                unconvertedTransformers.add(transformerId);
                isValid = false;
            }
        }
        if (!unconvertedTransformers.isEmpty()) {
            errors.append("Unconverted transformers: ").append(unconvertedTransformers).append("\n");
        }

        // 4. Afficher les statistiques
        System.out.println("Conversion status: " + (isValid ? "SUCCESS" : "FAILURE"));
        System.out.println("\nStatistics:");
        System.out.printf("- Buses: converted %d/%d%n",
                ucteNodeIds.size(),
                network.getVoltageLevelStream()
                        .mapToLong(vl -> StreamSupport.stream(vl.getBusBreakerView().getBuses().spliterator(), false).count())
                        .sum());
        System.out.printf("- Lines: converted %d/%d%n",
                network.getLineCount() - unconvertedLines.size(),
                network.getLineCount());
        System.out.printf("- Transformers: converted %d/%d%n",
                network.getTwoWindingsTransformerCount() - unconvertedTransformers.size(),
                network.getTwoWindingsTransformerCount());

        // 5. Afficher les erreurs si présentes
        if (!isValid) {
            System.out.println("\nConversion errors:");
            System.out.println(errors);
        }

        // 6. Valider le format des IDs convertis
        System.out.println("\nValidating UCTE format:");
        // Vérifier le format des noeuds
        for (Map.Entry<String, UcteNodeCode> entry : ucteNodeIds.entrySet()) {
            String ucteId = entry.getValue().toString();
            if (!ucteId.matches("[A-Z][A-Z0-9_]{5}[0-9]_")) {
                System.out.printf("Invalid node format: %s -> %s%n", entry.getKey(), ucteId);

            }
        }
        // Vérifier le format des éléments
        for (Map.Entry<String, UcteElementId> entry : ucteElementIds.entrySet()) {
            String ucteId = entry.getValue().toString();
            if (!ucteId.matches("[A-Z][A-Z0-9_]{5}[0-9]_ [A-Z][A-Z0-9_]{5}[0-9]_ [0-9]")) {
                System.out.printf("Invalid element format: %s -> %s%n", entry.getKey(), ucteId);

            }
        }
    }

}
