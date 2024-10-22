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

import java.util.HashMap;
import java.util.Map;


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
    }

    private void convertToUcte(Network network) {
        for (Substation substation : network.getSubstations()) {
            String countryCode = substation.getCountry().map(Country::getName).orElse("XX");
            for (VoltageLevel vl : substation.getVoltageLevels()) {
                //bus
                for (Bus bus : vl.getBusBreakerView().getBuses()) {
                    generateUcteNodeId(countryCode, bus);
                    System.out.println(ucteNodeIds);
                }
            }
        }

        //Lignes
        for (Line line : network.getLines()) {
            String busId1 = line.getTerminal1().getBusBreakerView().getBus().getId();
            String busId2 = line.getTerminal2().getBusBreakerView().getBus().getId();
            generateUcteElementId(busId1, busId2);
        }

    }

    private void generateUcteNodeId(String country, Bus bus) {
        char countryCode = getCountryCode(country);
        char voltageCode = getVoltageLevelCode(bus.getVoltageLevel().getNominalV());
        String busId = bus.getId();
        String nodeCode = String.format("%-5s", busId).replace(' ', '_');
        String name = countryCode + nodeCode + voltageCode + "_";
        ucteNodeIds.computeIfAbsent(bus.getId(),
                n -> UcteNodeCode.parseUcteNodeCode(name)
                        .orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + n)));
    }

    private void generateUcteElementId(String busId1,String busId2) {
        if(ucteNodeIds.containsKey(busId1) && ucteNodeIds.containsKey(busId2)) {
            UcteNodeCode nodeCode1 = ucteNodeIds.get(busId1);
            UcteNodeCode nodeCode2 = ucteNodeIds.get(busId2);
            UcteElementId id = new UcteElementId(nodeCode1,nodeCode2,'1');
            ucteElementIds.computeIfAbsent(id.toString(), k -> UcteElementId.parseUcteElementId(id.toString()).orElseThrow(() -> new UcteException("Invalid UCTE node identifier: " + k)));
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

}
