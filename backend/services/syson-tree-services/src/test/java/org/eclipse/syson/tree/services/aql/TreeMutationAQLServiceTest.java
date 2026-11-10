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

import org.eclipse.sirius.components.core.api.IFeedbackMessageService;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.Type;
import org.eclipse.syson.tree.selectiondialog.FeatureChain;
import org.junit.jupiter.api.Test;

/**
 * Tests the adaptation of feature chain redefinition results and feedback for AQL callers.
 *
 * @author Jerome Gout
 */
public class TreeMutationAQLServiceTest {

    private final TreeMutationAQLService service = new TreeMutationAQLService(new IFeedbackMessageService.NoOp());

    @Test
    public void returnsTheWholeLocalChainAndReusesItOnRepeatedInvocations() {
        var definition = SysmlFactory.eINSTANCE.createPartDefinition();
        var context = SysmlFactory.eINSTANCE.createPartUsage();
        var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        context.getOwnedRelationship().add(typing);
        typing.setType(definition);
        typing.setTypedFeature(context);
        var part = SysmlFactory.eINSTANCE.createPartUsage();
        var attribute = SysmlFactory.eINSTANCE.createAttributeUsage();
        this.addFeature(definition, part);
        this.addFeature(part, attribute);
        var chain = new FeatureChain(List.of(part, attribute));

        var result = this.service.redefineFeatureChain(context, chain);

        assertThat(result).hasSize(2);
        var localPart = result.getFirst();
        var localAttribute = result.getLast();
        assertThat(context.getOwnedFeature()).containsExactly(localPart);
        assertThat(localPart.getOwnedFeature()).containsExactly(localAttribute);
        assertThat(localPart.getOwnedRedefinition()).singleElement().satisfies(redefinition -> assertThat(redefinition.getRedefinedFeature()).isSameAs(part));
        assertThat(localAttribute.getOwnedRedefinition()).singleElement().satisfies(redefinition -> assertThat(redefinition.getRedefinedFeature()).isSameAs(attribute));
        assertThat(this.service.redefineFeatureChain(context, chain)).containsExactly(localPart, localAttribute);
        assertThat(context.getOwnedFeature()).containsExactly(localPart);
        assertThat(localPart.getOwnedFeature()).containsExactly(localAttribute);
        assertThat(part.getOwnedFeature()).containsExactly(attribute);
    }

    @Test
    public void reportsARejectedNestedSegmentWithoutCreatingAPartialChain() {
        var definition = SysmlFactory.eINSTANCE.createPartDefinition();
        var context = SysmlFactory.eINSTANCE.createPartUsage();
        var typing = SysmlFactory.eINSTANCE.createFeatureTyping();
        context.getOwnedRelationship().add(typing);
        typing.setType(definition);
        typing.setTypedFeature(context);
        var part = SysmlFactory.eINSTANCE.createPartUsage();
        var parameter = SysmlFactory.eINSTANCE.createAttributeUsage();
        parameter.setDirection(FeatureDirectionKind.IN);
        this.addFeature(definition, part);
        this.addFeature(part, parameter);
        var relationships = List.copyOf(context.getOwnedRelationship());

        assertThat(this.service.redefineFeatureChain(context, new FeatureChain(List.of(part, parameter)))).isNull();

        assertThat(context.getOwnedRelationship()).containsExactlyElementsOf(relationships);
        assertThat(context.getOwnedFeature()).isEmpty();
    }

    @Test
    public void missingSelectionReturnsNullWithoutChangingContext() {
        var context = SysmlFactory.eINSTANCE.createPartDefinition();
        context.setDeclaredName("Context");

        assertThat(this.service.redefineFeatureChain(context, null)).isNull();

        assertThat(context.getOwnedRelationship()).isEmpty();
    }

    @Test
    public void missingContextReturnsNull() {
        var feature = SysmlFactory.eINSTANCE.createPartUsage();

        assertThat(this.service.redefineFeatureChain(null, new FeatureChain(List.of(feature)))).isNull();
    }

    private void addFeature(Type owner, Feature feature) {
        var membership = SysmlFactory.eINSTANCE.createFeatureMembership();
        owner.getOwnedRelationship().add(membership);
        membership.getOwnedRelatedElement().add(feature);
    }
}
