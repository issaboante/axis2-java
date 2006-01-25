/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis2.handlers.addressing;

import junit.framework.TestCase;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisConfiguration;

import javax.xml.namespace.QName;
import java.io.File;

public class AddressingModuleTest extends TestCase {

    /**
     * @param testName
     */
    public AddressingModuleTest(String testName) {
        super(testName);
    }

    public void testExtractAddressingInformationFromHeaders() throws AxisFault {
        AxisConfiguration er = ConfigurationContextFactory.createConfigurationContextFromFileSystem("target", null)
                .getAxisConfiguration();
        File file = new File("target/addressing-SNAPSHOT-0.95.mar");
        assertTrue(file.exists());
        AxisModule axisModule = er.getModule(new QName("addressing"));
        assertNotNull(axisModule);
    }

}
