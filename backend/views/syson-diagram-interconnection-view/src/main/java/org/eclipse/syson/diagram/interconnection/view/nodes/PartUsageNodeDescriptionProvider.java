/*******************************************************************************
 * Copyright (c) 2023 Obeo.
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
package org.eclipse.syson.diagram.interconnection.view.nodes;

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.sirius.components.view.builder.IViewDiagramElementFinder;
import org.eclipse.sirius.components.view.builder.generated.DiagramBuilders;
import org.eclipse.sirius.components.view.builder.generated.FreeFormLayoutStrategyDescriptionBuilder;
import org.eclipse.sirius.components.view.builder.generated.ViewBuilders;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.INodeDescriptionProvider;
import org.eclipse.sirius.components.view.diagram.DiagramDescription;
import org.eclipse.sirius.components.view.diagram.NodeContainmentKind;
import org.eclipse.sirius.components.view.diagram.NodeDescription;
import org.eclipse.sirius.components.view.diagram.NodePalette;
import org.eclipse.sirius.components.view.diagram.NodeStyleDescription;
import org.eclipse.sirius.components.view.diagram.NodeTool;
import org.eclipse.sirius.components.view.diagram.NodeToolSection;
import org.eclipse.sirius.components.view.diagram.SynchronizationPolicy;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.AQLConstants;
import org.eclipse.syson.util.SysMLMetamodelHelper;
import org.eclipse.syson.util.ViewConstants;

/**
 * Used to create the part definition node description.
 *
 * @author arichard
 */
public class PartUsageNodeDescriptionProvider implements INodeDescriptionProvider {

    public static final String NAME = "IV Node PartUsage";

    private final ViewBuilders viewBuilderHelper = new ViewBuilders();

    private final DiagramBuilders diagramBuilderHelper = new DiagramBuilders();

    private final IColorProvider colorProvider;

    public PartUsageNodeDescriptionProvider(IColorProvider colorProvider) {
        this.colorProvider = Objects.requireNonNull(colorProvider);
    }

    @Override
    public NodeDescription create() {
        String domainType = SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getPartUsage());
        return this.diagramBuilderHelper.newNodeDescription()
                .childrenLayoutStrategy(new FreeFormLayoutStrategyDescriptionBuilder().build())
                .defaultHeightExpression("400")
                .defaultWidthExpression("700")
                .domainType(domainType)
                .labelExpression("aql:self.getContainerLabel()")
                .name(NAME)
                .semanticCandidatesExpression(AQLConstants.AQL_SELF)
                .style(this.createPartUsageNodeStyle())
                .userResizable(true)
                .synchronizationPolicy(SynchronizationPolicy.SYNCHRONIZED)
                .build();
    }

    @Override
    public void link(DiagramDescription diagramDescription, IViewDiagramElementFinder cache) {
        var optPartUsageNodeDescription = cache.getNodeDescription(PartUsageNodeDescriptionProvider.NAME);
        var optChildPartUsageNodeDescription = cache.getNodeDescription(ChildPartUsageNodeDescriptionProvider.NAME);
        var optPortUsageBorderNodeDescription = cache.getNodeDescription(PortUsageBorderNodeDescriptionProvider.NAME);

        if (optPartUsageNodeDescription.isPresent() && optChildPartUsageNodeDescription.isPresent() && optPortUsageBorderNodeDescription.isPresent()) {
            NodeDescription nodeDescription = optPartUsageNodeDescription.get();
            diagramDescription.getNodeDescriptions().add(nodeDescription);
            nodeDescription.getChildrenDescriptions().add(optChildPartUsageNodeDescription.get());
            nodeDescription.getBorderNodesDescriptions().add(optPortUsageBorderNodeDescription.get());
            nodeDescription.setPalette(this.createNodePalette(cache));
        }
    }

    private NodeStyleDescription createPartUsageNodeStyle() {
        return this.diagramBuilderHelper.newRectangularNodeStyleDescription()
                .borderColor(this.colorProvider.getColor(ViewConstants.DEFAULT_BORDER_COLOR))
                .borderRadius(10)
                .color(this.colorProvider.getColor(ViewConstants.DEFAULT_BACKGROUND_COLOR))
                .displayHeaderSeparator(false)
                .labelColor(this.colorProvider.getColor(ViewConstants.DEFAULT_LABEL_COLOR))
                .showIcon(true)
                .withHeader(true)
                .build();
    }

    private NodePalette createNodePalette(IViewDiagramElementFinder cache) {

        return this.diagramBuilderHelper.newNodePalette()
                .toolSections(this.createNodeToolSection(cache), this.addElementsToolSection(cache))
                .build();
    }

    private NodeToolSection createNodeToolSection(IViewDiagramElementFinder cache) {
        return this.diagramBuilderHelper.newNodeToolSection()
                .name("Create")
                .nodeTools(this.createNodeTool(cache.getNodeDescription(ChildPartUsageNodeDescriptionProvider.NAME).get(), SysmlPackage.eINSTANCE.getPartUsage(), NodeContainmentKind.CHILD_NODE),
                           this.createNodeTool(cache.getNodeDescription(PortUsageBorderNodeDescriptionProvider.NAME).get(), SysmlPackage.eINSTANCE.getPortUsage(), NodeContainmentKind.BORDER_NODE))
                .build();
    }

    private NodeTool createNodeTool(NodeDescription nodeDescription, EClass eClass, NodeContainmentKind nodeKind) {
        var builder = this.diagramBuilderHelper.newNodeTool();

        var setValue = this.viewBuilderHelper.newSetValue()
                .featureName(SysmlPackage.eINSTANCE.getElement_DeclaredName().getName())
                .valueExpression(eClass.getName());

        var changeContextNewInstance = this.viewBuilderHelper.newChangeContext()
                .expression("aql:newInstance")
                .children(setValue.build());

        var createEClassInstance = this.viewBuilderHelper.newCreateInstance()
                .typeName(SysMLMetamodelHelper.buildQualifiedName(eClass))
                .referenceName(SysmlPackage.eINSTANCE.getRelationship_OwnedRelatedElement().getName())
                .variableName("newInstance")
                .children(changeContextNewInstance.build());

        var createView = this.diagramBuilderHelper.newCreateView()
                .containmentKind(nodeKind)
                .elementDescription(nodeDescription)
                .parentViewExpression("aql:selectedNode")
                .semanticElementExpression("aql:newInstance")
                .variableName("newInstanceView");

        var changeContexMembership = this.viewBuilderHelper.newChangeContext()
                .expression("aql:newFeatureMembership")
                .children(createEClassInstance.build(), createView.build());

        var createMembership = this.viewBuilderHelper.newCreateInstance()
                .typeName(SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getFeatureMembership()))
                .referenceName(SysmlPackage.eINSTANCE.getElement_OwnedRelationship().getName())
                .variableName("newFeatureMembership")
                .children(changeContexMembership.build());

        return builder
                .name(eClass.getName())
                .body(createMembership.build())
                .build();
    }

    private NodeToolSection addElementsToolSection(IViewDiagramElementFinder cache) {
        return this.diagramBuilderHelper.newNodeToolSection()
                .name("Add")
                .nodeTools(this.addExistingElementsTool())
                .build();
    }

    private NodeTool addExistingElementsTool() {
        var builder = this.diagramBuilderHelper.newNodeTool();

        var addExistingelements = this.viewBuilderHelper.newChangeContext()
                .expression("aql:self.addExistingElements(editingContext, diagramContext, selectedNode, convertedNodes)");

        return builder
                .name("Add existing elements")
                .body(addExistingelements.build())
                .build();
    }
}