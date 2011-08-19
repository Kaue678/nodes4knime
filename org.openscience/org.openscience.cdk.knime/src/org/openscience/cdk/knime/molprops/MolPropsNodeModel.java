/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 */
package org.openscience.cdk.knime.molprops;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.type.CDKValue;


/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class MolPropsNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(MolPropsNodeModel.class);
    
    /** NodeSettings file containing names. */
    private static final String MOLPROPS_IDENTIFIER_FILE = 
        MolPropsNodeModel.class.getPackage().getName().replace('.', '/')
            + "/molprops.set";
    
    private static final Map<DataColumnSpec, String> MOLPROPS_IDENTIFIER_MAP;

    /** NodeSettings key to store all property desriptions. */
    protected static final String CFGKEY_PROPS = "PropertyDescriptions";

    /** NodeSettings key to store smiles column header. */
    protected static final String CFGKEY_SMILES = "smilesIndex";

    private final ArrayList<String> m_propDescriptions;

    private String m_cdkColumn;
    
    static {
        // available properties for this node
        LinkedHashSet<String> descResultSet = new LinkedHashSet<String>();
        try {
            ClassLoader loader = MolPropsNodeModel.class.getClassLoader();
            InputStream stream = 
                loader.getResourceAsStream(MOLPROPS_IDENTIFIER_FILE);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(stream));
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#")) {
                    descResultSet.add(line);
                }
            }
        } catch (Exception ioe) {
            LOGGER.debug("Failed load descriptor result file "
                    + MOLPROPS_IDENTIFIER_FILE, ioe);
        }
        LinkedHashMap<DataColumnSpec, String> specStringMap = 
            new LinkedHashMap<DataColumnSpec, String>();
        for (String className : descResultSet) {
            DataColumnSpec s = MolPropsLibrary.getColumnSpec(className);
            if (s == null) {
                LOGGER.debug("Descriptor \"" + className + "\" not available");
            } else {
                specStringMap.put(s, className);
            }
        }
        MOLPROPS_IDENTIFIER_MAP = Collections.unmodifiableMap(specStringMap);
    }

    /** Constructor: One input, one output. */
    public MolPropsNodeModel() {
        super(1, 1);
        m_propDescriptions = new ArrayList<String>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFGKEY_SMILES, m_cdkColumn);
        String[] props = m_propDescriptions.toArray(new String[0]);
        settings.addStringArray(CFGKEY_PROPS, props);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(CFGKEY_SMILES);
        settings.getStringArray(CFGKEY_PROPS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_cdkColumn = settings.getString(CFGKEY_SMILES);
        String[] props = settings.getStringArray(CFGKEY_PROPS);
        m_propDescriptions.clear();
        m_propDescriptions.addAll(Arrays.asList(props));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        ColumnRearranger arranger = 
            createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable t = exec.createColumnRearrangeTable(
                inData[0], arranger, exec);
        return new BufferedDataTable[]{t};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        if (!inSpec.containsCompatibleType(CDKValue.class)) {
            throw new InvalidSettingsException("No smiles cell in input table");
        }
        if (m_cdkColumn == null) {
            // if only one smiles cell, we can safely use it
            int smilesCellCount = 0;
            String smilesColName = null;
            for (int i = 0; i < inSpec.getNumColumns(); i++) {
                DataType cC = inSpec.getColumnSpec(i).getType();
                if (cC.isCompatible(CDKValue.class)) {
                    smilesCellCount++;
                    smilesColName = inSpec.getColumnSpec(i).getName();
                }
            }
            assert (smilesCellCount >= 1);
            if (smilesCellCount == 1) { // only one is found - use it.
                LOGGER.info("No smiles cell was set: I fix it to \""
                        + smilesColName + "\".");
                m_cdkColumn = smilesColName;
            } else { // ambiguous smiles cell columns
                throw new InvalidSettingsException("No smiles cell defined");
            }
        }
        int smilesCol = inSpec.findColumnIndex(m_cdkColumn);
        if (smilesCol < 0) {
            throw new InvalidSettingsException("No smiles column \""
                    + m_cdkColumn + "\" in table.");
        }
        DataTableSpec[] outSpecs = new DataTableSpec[1];
        outSpecs[0] = createColumnRearranger(inSpec).createSpec();
        return outSpecs;
    }
    
    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) 
        throws InvalidSettingsException {
        HashSet<String> hash = new HashSet<String>(m_propDescriptions); 
        String[] propsClassNames = new String[m_propDescriptions.size()];
        int index = 0;
        for (Map.Entry<DataColumnSpec, String> entry 
            : MOLPROPS_IDENTIFIER_MAP.entrySet()) {
            if (hash.remove(entry.getKey().getName())) {
                propsClassNames[index++] = entry.getValue();
            }
        }
        if (!hash.isEmpty()) {
            throw new InvalidSettingsException("Some properties are unknown: "
                    + Arrays.toString(hash.toArray()));
        }
        assert index == propsClassNames.length;
        // MolPropsGenerator needs the column specs of the new columns,
        // we need to generate that
        DataColumnSpec[] propsSpec = new DataColumnSpec[propsClassNames.length];
        for (int i = 0; i < propsClassNames.length; i++) {
            String s = propsClassNames[i];
            DataColumnSpec colSpec = MolPropsLibrary.getColumnSpec(s);
            if (spec.containsName(colSpec.getName())) {
                int uniquifier = 1;
                String name;
                do {
                    name = colSpec.getName() + " #" + uniquifier;
                    uniquifier++;
                } while (spec.containsName(name));
                DataColumnSpecCreator c = new DataColumnSpecCreator(colSpec);
                c.setName(name);
                colSpec = c.createSpec();
            }
            propsSpec[i] = colSpec;
        }
        int smilesIn = spec.findColumnIndex(m_cdkColumn);
        MolPropsGenerator generator = 
            new MolPropsGenerator(smilesIn, propsClassNames, propsSpec);
        ColumnRearranger arrange = new ColumnRearranger(spec);
        arrange.append(generator);
        return arrange;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
    
    /** Get list of available descriptors for this node.
     * @return This list. */
    static DataColumnSpec[] getAvailableDescriptorList() {
        return MOLPROPS_IDENTIFIER_MAP.keySet().toArray(new DataColumnSpec[0]);
    }
}