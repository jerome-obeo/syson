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
package org.eclipse.syson.tree.services.aql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.syson.application.services.SysONResourceService;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.Type;
import org.eclipse.syson.sysml.metamodel.services.MetamodelMutationElementService;
import org.eclipse.syson.tree.explorer.services.LibrarySearchServiceNoOp;
import org.eclipse.syson.tree.explorer.services.SysONExplorerFilterService;
import org.eclipse.syson.tree.explorer.services.api.ISysONExplorerService;
import org.eclipse.syson.tree.selectiondialog.FeatureChain;
import org.eclipse.syson.tree.services.TreeQuerySelectionDialogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the feature chain collection exposed to selection dialogs by the AQL service.
 *
 * @author Jerome Gout
 */
public class TreeQueryAQLServiceTest {

    private final TreeQueryAQLService service = new TreeQueryAQLService(new ISysONExplorerService.NoOp(),
            new TreeQuerySelectionDialogService(new ISysONExplorerService.NoOp(),
                    new SysONExplorerFilterService(new SysONResourceService(new LibrarySearchServiceNoOp()))));

    @Test
    public void collectsInheritedChainsAndKeepsInheritedDescendantsOfLocalPrefixes() {
        var definition = SysmlFactory.eINSTANCE.createPartDefinition();
        var context = SysmlFactory.eINSTANCE.createPartUsage();
        this.addType(context, definition);
        var part = SysmlFactory.eINSTANCE.createPartUsage();
        var attribute = SysmlFactory.eINSTANCE.createAttributeUsage();
        this.addFeature(definition, part);
        this.addFeature(part, attribute);
        this.addFeature(context, SysmlFactory.eINSTANCE.createAttributeUsage());

        assertThat(this.service.getFeatureChainSelectionDialogElements(context))
                .containsExactly(new FeatureChain(List.of(part)), new FeatureChain(List.of(part, attribute)));

        var mutationService = new MetamodelMutationElementService();
        var prefix = mutationService.redefineFeatureChain(context, List.of(part));
        assertThat(prefix.isSuccess()).isTrue();
        var localPart = prefix.chain().getFirst();
        assertThat(this.service.getFeatureChainSelectionDialogElements(context)).containsExactly(new FeatureChain(List.of(localPart, attribute)));

        assertThat(mutationService.redefineFeatureChain(context, List.of(localPart, attribute)).isSuccess()).isTrue();
        assertThat(this.service.getFeatureChainSelectionDialogElements(context)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "sysmllibrary:///test", "kermllibrary:///test" })
    public void excludesStandardLibraryBranchesButKeepsProjectFeatures(String libraryUri) {
        var libraryDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        var libraryPart = SysmlFactory.eINSTANCE.createPartUsage();
        this.addFeature(libraryDefinition, libraryPart);
        this.addFeature(libraryPart, SysmlFactory.eINSTANCE.createAttributeUsage());
        var library = new ResourceImpl(URI.createURI(libraryUri));
        library.getContents().add(libraryDefinition);
        var projectDefinition = SysmlFactory.eINSTANCE.createPartDefinition();
        var projectAttribute = SysmlFactory.eINSTANCE.createAttributeUsage();
        this.addFeature(projectDefinition, projectAttribute);
        var context = SysmlFactory.eINSTANCE.createPartUsage();
        this.addType(context, libraryDefinition);
        this.addType(context, projectDefinition);

        assertThat(this.service.getFeatureChainSelectionDialogElements(context)).containsExactly(new FeatureChain(List.of(projectAttribute)));
    }

    @Test
    public void leavesUnsupportedFeatureValidationToTheRedefinitionService() {
        var definition = SysmlFactory.eINSTANCE.createPartDefinition();
        var context = SysmlFactory.eINSTANCE.createPartUsage();
        this.addType(context, definition);
        var directed = SysmlFactory.eINSTANCE.createAttributeUsage();
        directed.setDirection(FeatureDirectionKind.IN);
        this.addFeature(definition, directed);
        var parameter = SysmlFactory.eINSTANCE.createAttributeUsage();
        var parameterMembership = SysmlFactory.eINSTANCE.createParameterMembership();
        definition.getOwnedRelationship().add(parameterMembership);
        parameterMembership.getOwnedRelatedElement().add(parameter);
        var feature = SysmlFactory.eINSTANCE.createFeature();
        this.addFeature(definition, feature);

        assertThat(this.service.getFeatureChainSelectionDialogElements(context))
                .containsExactly(new FeatureChain(List.of(directed)), new FeatureChain(List.of(parameter)), new FeatureChain(List.of(feature)));
    }

    @Test
    public void emptyContextHasNoSelectableChains() {
        assertThat(this.service.getFeatureChainSelectionDialogElements(SysmlFactory.eINSTANCE.createPartDefinition())).isEmpty();
    }

    private void addFeature(Type owner, Feature feature) {
        var membership = SysmlFactory.eINSTANCE.createFeatureMembership();
        owner.getOwnedRelationship().add(membership);
        membership.getOwnedRelatedElement().add(feature);
    }

    private void addType(Feature feature, Type type) {
        var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        feature.getOwnedRelationship().add(typing);
        typing.setType(type);
        typing.setTypedFeature(feature);
    }
}
