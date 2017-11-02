/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.scripting.groovy;

import com.powsybl.afs.FooFileExtension;
import com.powsybl.afs.ProjectFileExtension;
import com.powsybl.afs.mapdb.storage.MapDbAppFileSystemStorage;
import com.powsybl.afs.storage.AppFileSystemStorage;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class AfsExtensionGroovyScriptTest extends AbstractGroovyScriptTest {

    @Override
    protected AppFileSystemStorage createStorage() {
        return MapDbAppFileSystemStorage.createHeap("mem");
    }

    @Override
    protected List<ProjectFileExtension> getProjectFileExtensions() {
        return Collections.singletonList(new FooFileExtension());
    }

    @Override
    protected Reader getCodeReader() {
        return new StringReader(String.join(System.lineSeparator(),
                "project = afs.getNode('mem:/').createProject('test')",
                "root = project.getRootFolder()",
                "",
                "foo = root.fooBuilder()",
                "    .withName('foo')",
                "    .build()",
                "println foo.getName()",
                "",
                "foo = root.getFoo(\"foo\")", // try to reload
                "println foo.getName()",
                "",
                "foo2 = root.buildFoo {", // groovy idiomatic builder
                "    name 'foo2'",
                "}",
                "print foo2.getName()"
        ));
    }

    @Override
    protected String getExpectedOutput() {
        return String.join(System.lineSeparator(), "foo", "foo", "foo2");
    }
}
