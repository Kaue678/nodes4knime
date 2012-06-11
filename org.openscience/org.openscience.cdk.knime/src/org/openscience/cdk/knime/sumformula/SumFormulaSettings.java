/*
 * Created on 20.01.2012 10:58:41 by Stephan Beisken
 * ------------------------------------------------------------------------
 * 
 * Copyright (C) 2012 Stephan Beisken <beisken@ebi.ac.uk>
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License, Version 3, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * 
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs. Hence, KNIME and ECLIPSE are both independent
 * programs and are not derived from each other. Should, however, the interpretation of the GNU GPL Version 3
 * ("License") under any applicable laws result in KNIME and ECLIPSE being a combined program, KNIME GMBH herewith
 * grants you the additional permission to use and propagate KNIME together with ECLIPSE with only the license terms in
 * place for ECLIPSE applying to ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the license terms of
 * ECLIPSE themselves allow for the respective use and propagation of ECLIPSE together with KNIME.
 * 
 * Additional permission relating to nodes for KNIME that extend the Node Extension (and in particular that are based on
 * subclasses of NodeModel, NodeDialog, and NodeView) and that only interoperate with KNIME through standard APIs
 * ("Nodes"): Nodes are deemed to be separate and independent programs and to not be covered works. Notwithstanding
 * anything to the contrary in the License, the License does not apply to Nodes, you are not required to license Nodes
 * under the License, and you are granted a license to prepare and propagate Nodes, in each case even if such Nodes are
 * propagated with or for interoperation with KNIME. The owner of a Node may freely choose the license terms applicable
 * to such Node, including when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------- *
 */
package org.openscience.cdk.knime.sumformula;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings for the "SumFormulaNode" Node.
 * 
 * @author Stephan Beisken
 * 
 */
public class SumFormulaSettings {

	private String massColumn;
	private boolean excludeByValidSum;

	/**
	 * Gets the name of the mass containing double column.
	 * 
	 * @return the massColumn
	 */
	public String getMassColumn() {

		return massColumn;
	}

	/**
	 * Sets the name of the mass containing double column.
	 * 
	 * @param massColumn the massColumn to set
	 */
	public void setMassColumn(String massColumn) {

		this.massColumn = massColumn;
	}

	/**
	 * Returns if unlikely filtered molecular formulas should be exclucded.
	 * 
	 * @return the excludeByValidSum
	 */
	public boolean isExcludeByValidSum() {

		return excludeByValidSum;
	}

	/**
	 * Sets if unlikely filtered molecular formulas should be exclucded.
	 * 
	 * @param excludeByValidSum the excludeByValidSum to set
	 */
	public void setExcludeByValidSum(boolean excludeByValidSum) {

		this.excludeByValidSum = excludeByValidSum;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("massColumn", massColumn);
		settings.addBoolean("exclude", excludeByValidSum);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		massColumn = settings.getString("massColumn");
		excludeByValidSum = settings.getBoolean("exclude");
	}
}