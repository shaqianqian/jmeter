// $Header$
/*
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

/*
 * Created on May 4, 2003
 */
package org.apache.jmeter.engine.util;

import java.util.Map;

import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.testelement.property.FunctionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;

/**
 * @author ano ano
 * 
 * @version $Revision$
 */
public class ReplaceStringWithFunctions extends AbstractTransformer {
	public ReplaceStringWithFunctions(CompoundVariable masterFunction, Map variables) {
		super();
		setMasterFunction(masterFunction);
		setVariables(variables);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ValueTransformer#transformValue(JMeterProperty)
	 */
	public JMeterProperty transformValue(JMeterProperty prop) throws InvalidVariableException {
		JMeterProperty newValue = prop;
		getMasterFunction().clear();
		getMasterFunction().setParameters(prop.getStringValue());
		if (getMasterFunction().hasFunction()) {
			newValue = new FunctionProperty(prop.getName(), getMasterFunction().getFunction());
		}
		return newValue;
	}

}
