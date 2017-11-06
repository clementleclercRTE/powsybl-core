/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.ext.base;

import com.google.auto.service.AutoService;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.ProjectFileExtension;
import com.powsybl.afs.storage.AppFileSystemStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.iidm.import_.ImportersLoader;
import com.powsybl.iidm.import_.ImportersServiceLoader;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(ProjectFileExtension.class)
public class ImportedCaseExtension implements ProjectFileExtension {

    private final ImportersLoader importersLoader;

    public ImportedCaseExtension() {
        this(new ImportersServiceLoader());
    }

    public ImportedCaseExtension(ImportersLoader importersLoader) {
        this.importersLoader = Objects.requireNonNull(importersLoader);
    }

    @Override
    public Class<ImportedCase> getProjectFileClass() {
        return ImportedCase.class;
    }

    @Override
    public String getProjectFilePseudoClass() {
        return ImportedCase.PSEUDO_CLASS;
    }

    @Override
    public Class<ImportedCaseBuilder> getProjectFileBuilderClass() {
        return ImportedCaseBuilder.class;
    }

    @Override
    public ImportedCase createProjectFile(NodeInfo info, AppFileSystemStorage storage, NodeInfo projectInfo, AppFileSystem fileSystem) {
        return new ImportedCase(info, storage, projectInfo, fileSystem, importersLoader);
    }

    @Override
    public ImportedCaseBuilder createProjectFileBuilder(NodeInfo folderInfo, AppFileSystemStorage storage, NodeInfo projectInfo, AppFileSystem fileSystem) {
        return new ImportedCaseBuilder(folderInfo, storage, projectInfo, fileSystem, importersLoader);
    }
}
