/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.syson.tree.selectiondialog;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.syson.sysml.Feature;

/**
 * A virtual selection dialog element representing an ordered chain of features.
 *
 * @param features
 *            the features in traversal order
 * @author Jerome Gout
 */
public record FeatureChain(List<Feature> features) {

    public FeatureChain {
        features = List.copyOf(features);
        if (features.isEmpty()) {
            throw new IllegalArgumentException("A feature chain must contain at least one feature");
        }
    }

    public String getLabel() {
        return this.features.stream().map(Feature::getName).collect(Collectors.joining("."));
    }
}
