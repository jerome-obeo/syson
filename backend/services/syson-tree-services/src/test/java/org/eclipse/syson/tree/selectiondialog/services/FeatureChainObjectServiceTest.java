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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.components.core.api.IDefaultLabelService;
import org.eclipse.sirius.components.core.api.IDefaultObjectSearchService;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.labels.StyledString;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.tree.selectiondialog.FeatureChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests virtual feature chain identity, presentation and resolution.
 *
 * @author Jerome Gout
 */
public class FeatureChainObjectServiceTest {

    private final IEditingContext editingContext = new IEditingContext.NoOp();

    private final TestObjectSearchService searchService = new TestObjectSearchService(this.editingContext);

    private final TestLabelService labelService = new TestLabelService();

    private final FeatureChainObjectService featureChainObjectService = new FeatureChainObjectService(this.searchService, this.labelService);

    @Test
    public void chainDefensivelyCopiesItsFeaturesAndRejectsInvalidContents() {
        var feature = SysmlFactory.eINSTANCE.createPartUsage();
        var features = new ArrayList<Feature>(List.of(feature));
        var chain = new FeatureChain(features);
        features.clear();

        assertThat(chain.features()).containsExactly(feature);
        assertThatThrownBy(() -> chain.features().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new FeatureChain(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FeatureChain(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new FeatureChain(Arrays.asList(feature, null))).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void labelsFollowChainOrderAndIconsComeFromTheLastFeature() {
        var part = SysmlFactory.eINSTANCE.createPartUsage();
        part.setDeclaredName("part");
        var port = SysmlFactory.eINSTANCE.createPortUsage();
        port.setDeclaredName("port");
        var chain = new FeatureChain(List.of(part, port));
        var icons = List.of("/icons/custom-port.svg");
        this.labelService.imagePaths.put(port, icons);

        assertThat(chain.getLabel()).isEqualTo("part.port");
        assertThat(this.featureChainObjectService.getStyledLabel(chain).toString()).isEqualTo("part.port");
        assertThat(this.featureChainObjectService.getImagePaths(chain)).isEqualTo(icons);
        assertThat(this.featureChainObjectService.canHandle(chain)).isTrue();
        assertThat(this.featureChainObjectService.getKind(chain)).isEqualTo("FeatureChain");
        assertThat(this.labelService.requestedObjects).containsExactly(port);
    }

    @Test
    public void unsupportedObjectsHaveNoIdentityLabelOrIcon() {
        var feature = SysmlFactory.eINSTANCE.createPartUsage();

        assertThat(this.featureChainObjectService.canHandle(feature)).isFalse();
        assertThat(this.featureChainObjectService.canHandle(null)).isFalse();
        assertThat(this.featureChainObjectService.getId(feature)).isNull();
        assertThat(this.featureChainObjectService.getStyledLabel(feature).toString()).isEmpty();
        assertThat(this.featureChainObjectService.getImagePaths(feature)).isEmpty();
        assertThat(this.labelService.requestedObjects).isEmpty();
    }

    @Test
    public void idsDistinguishSameNamedFeaturesAndPreserveChainOrderOnResolution() {
        var resource = new ResourceImpl(URI.createURI("test:/model"));
        var first = SysmlFactory.eINSTANCE.createPartUsage();
        var second = SysmlFactory.eINSTANCE.createPartUsage();
        first.setDeclaredName("sameName");
        second.setDeclaredName("sameName");
        resource.getContents().addAll(List.of(first, second));
        this.searchService.objects.put(EcoreUtil.getURI(first).toString(), first);
        this.searchService.objects.put(EcoreUtil.getURI(second).toString(), second);
        var chain = new FeatureChain(List.of(first, second));
        var id = this.featureChainObjectService.getId(chain);

        assertThat(this.featureChainObjectService.canHandle(this.editingContext, id)).isTrue();
        assertThat(this.featureChainObjectService.getId(new FeatureChain(List.of(first, second)))).isEqualTo(id);
        assertThat(this.featureChainObjectService.getId(new FeatureChain(List.of(second, first)))).isNotEqualTo(id);
        assertThat(this.featureChainObjectService.getId(new FeatureChain(List.of(first)))).isNotEqualTo(this.featureChainObjectService.getId(new FeatureChain(List.of(second))));
        assertThat(this.featureChainObjectService.getObject(this.editingContext, id)).contains(chain);
        assertThat(resource.getContents()).containsExactly(first, second);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "other-id", "feature-chain:", "feature-chain:!" })
    public void invalidIdsCannotBeResolved(String id) {
        assertThat(this.featureChainObjectService.getObject(this.editingContext, id)).isEmpty();
    }

    @Test
    public void unrelatedIdsDoNotInvokeTheDefaultSearchService() {
        assertThat(this.featureChainObjectService.canHandle(this.editingContext, null)).isFalse();
        assertThat(this.featureChainObjectService.canHandle(this.editingContext, "other-id")).isFalse();
        assertThat(this.featureChainObjectService.getObject(this.editingContext, "other-id")).isEmpty();
        assertThat(this.searchService.searchedIds).isEmpty();
    }

    @Test
    public void malformedSegmentAfterAResolvedPrefixDoesNotReturnAPartialChain() {
        var feature = SysmlFactory.eINSTANCE.createPartUsage();
        this.searchService.objects.put("foo", feature);

        assertThat(this.featureChainObjectService.getObject(this.editingContext, "feature-chain:Zm9v.!")).isEmpty();
        assertThat(this.searchService.searchedIds).containsExactly("foo");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void unresolvedOrNonFeatureSegmentsStopResolutionWithoutReturningAPartialChain(boolean wrongType) {
        var resource = new ResourceImpl(URI.createURI("test:/model"));
        var first = SysmlFactory.eINSTANCE.createPartUsage();
        var second = SysmlFactory.eINSTANCE.createPartUsage();
        var third = SysmlFactory.eINSTANCE.createPartUsage();
        resource.getContents().addAll(List.of(first, second, third));
        this.searchService.objects.put(EcoreUtil.getURI(first).toString(), first);
        if (wrongType) {
            this.searchService.objects.put(EcoreUtil.getURI(second).toString(), SysmlFactory.eINSTANCE.createPackage());
        }
        var id = this.featureChainObjectService.getId(new FeatureChain(List.of(first, second, third)));

        assertThat(this.featureChainObjectService.getObject(this.editingContext, id)).isEmpty();
        assertThat(this.searchService.searchedIds).containsExactly(EcoreUtil.getURI(first).toString(), EcoreUtil.getURI(second).toString());
    }

    /**
     * Test label service providing configured icons and recording the objects requested.
     *
     * @author Jerome Gout
     */
    private static final class TestLabelService implements IDefaultLabelService {

        private final Map<Object, List<String>> imagePaths = new HashMap<>();

        private final List<Object> requestedObjects = new ArrayList<>();

        @Override
        public StyledString getStyledLabel(Object object) {
            this.requestedObjects.add(object);
            return StyledString.of("");
        }

        @Override
        public List<String> getImagePaths(Object object) {
            this.requestedObjects.add(object);
            return this.imagePaths.getOrDefault(object, List.of());
        }
    }

    /**
     * Configurable NoOp search service checking the editing context and recording the requested identifiers.
     *
     * @author Jerome Gout
     */
    private static final class TestObjectSearchService extends IDefaultObjectSearchService.NoOp {

        private final IEditingContext expectedEditingContext;

        private final Map<String, Object> objects = new HashMap<>();

        private final List<String> searchedIds = new ArrayList<>();

        private TestObjectSearchService(IEditingContext expectedEditingContext) {
            this.expectedEditingContext = expectedEditingContext;
        }

        @Override
        public Optional<Object> getObject(IEditingContext editingContext, String objectId) {
            assertThat(editingContext).isSameAs(this.expectedEditingContext);
            this.searchedIds.add(objectId);
            return Optional.ofNullable(this.objects.get(objectId));
        }
    }
}
