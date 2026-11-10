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
package org.eclipse.syson.tree.selectiondialog.services;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.components.core.api.IDefaultLabelService;
import org.eclipse.sirius.components.core.api.IDefaultObjectSearchService;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IIdentityServiceDelegate;
import org.eclipse.sirius.components.core.api.ILabelServiceDelegate;
import org.eclipse.sirius.components.core.api.IObjectSearchServiceDelegate;
import org.eclipse.sirius.components.core.api.labels.StyledString;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.tree.selectiondialog.FeatureChain;
import org.springframework.stereotype.Service;

/**
 * Identifies, labels and resolves virtual feature chains without storing them in the model.
 *
 * @author Jerome Gout
 */
@Service
public class FeatureChainObjectService implements IIdentityServiceDelegate, ILabelServiceDelegate, IObjectSearchServiceDelegate {

    private static final String ID_PREFIX = "feature-chain:";

    private final IDefaultObjectSearchService objectSearchService;

    private final IDefaultLabelService labelService;

    public FeatureChainObjectService(IDefaultObjectSearchService objectSearchService, IDefaultLabelService labelService) {
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
        this.labelService = Objects.requireNonNull(labelService);
    }

    @Override
    public boolean canHandle(Object object) {
        return object instanceof FeatureChain;
    }

    @Override
    public String getId(Object object) {
        String id = null;
        if (object instanceof FeatureChain chain) {
            id = ID_PREFIX + chain.features().stream()
                    .map(feature -> EcoreUtil.getURI(feature).toString())
                    .map(uri -> Base64.getUrlEncoder().withoutPadding().encodeToString(uri.getBytes(StandardCharsets.UTF_8)))
                    .collect(Collectors.joining("."));
        }
        return id;
    }

    @Override
    public String getKind(Object object) {
        return FeatureChain.class.getSimpleName();
    }

    @Override
    public StyledString getStyledLabel(Object object) {
        String label = "";
        if (object instanceof FeatureChain chain) {
            label = chain.getLabel();
        }
        return StyledString.of(label);
    }

    @Override
    public List<String> getImagePaths(Object object) {
        if (object instanceof FeatureChain(List<Feature> features)) {
            return this.labelService.getImagePaths(features.getLast());
        }
        return List.of();
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, String objectId) {
        return objectId != null && objectId.startsWith(ID_PREFIX);
    }

    @Override
    public Optional<Object> getObject(IEditingContext editingContext, String objectId) {
        Optional<Object> result = Optional.empty();
        if (this.canHandle(editingContext, objectId)) {
            try {
                var segments = objectId.substring(ID_PREFIX.length()).split("\\.", -1);
                var features = Arrays.stream(segments)
                        .map(segment -> new String(Base64.getUrlDecoder().decode(segment), StandardCharsets.UTF_8))
                        .map(uri -> this.objectSearchService.getObject(editingContext, uri).orElse(null))
                        .takeWhile(Feature.class::isInstance)
                        .map(Feature.class::cast)
                        .toList();
                if (features.size() == segments.length) {
                    // all segments have been converted to features: the chain is complete
                    result = Optional.of(new FeatureChain(features));
                }
            } catch (IllegalArgumentException exception) {
                // A malformed identifier cannot be decoded.
            }
        }
        return result;
    }
}
