/*******************************************************************************
 * Copyright (c) 2024 Obeo.
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
package org.eclipse.syson.sysml;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.jupiter.api.Test;

/**
 * Test of ASTTransformer class.
 * 
 * @author gescande
 */
public class ASTTransformerTest {
  
    /**
     * Test null AST
     */
    @Test
    void convertNullTest() {
        ASTTransformer transformer = new ASTTransformer();

        Resource result = transformer.convertResource(null,  new ResourceSetImpl());

        assertEquals(false, result.getAllContents().hasNext());
    }

    /**
     * Test empty string
     */
    @Test
    void convertEmptyStringTest() {
        ASTTransformer transformer = new ASTTransformer();

        String fileContent = "";
        Resource result = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()), new ResourceSetImpl());

        assertEquals(false, result.getAllContents().hasNext());
    }

    /**
     * Test Empty AST
     */
    @Test
    void convertEmptyJsonTest() {
        ASTTransformer transformer = new ASTTransformer();

        String fileContent = "{}";
        Resource result = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()), new ResourceSetImpl());

        assertEquals(false, result.getAllContents().hasNext());
    }

    /**
     * Test Empty Array AST
     */
    @Test
    void convertEmptyJsonArrayTest() {
        ASTTransformer transformer = new ASTTransformer();

        String fileContent = "[]";
        Resource result = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()), new ResourceSetImpl());

        assertEquals(false, result.getAllContents().hasNext());
    }

    /**
     * Test Root namespace
     */
    @Test
    void convertEmptyNamespareTest() {
        ASTTransformer transformer = new ASTTransformer();

        String fileContent = """
            {
                "$type": "Namespace",
                "children": []
            }
            """;
        Resource result = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()), new ResourceSetImpl());

        var content = result.getAllContents();
        assertEquals(true, content.hasNext());
        EObject object = content.next();
        assertEquals(false, content.hasNext());
        assertInstanceOf(Namespace.class, object);
    }

    /**
     * Test Containement reference
     */
    @Test
    void convertNamespareWithPackageTest() {
        ASTTransformer transformer = new ASTTransformer();
        String fileContent = """
            {
                "$type": "Namespace",
                "children": [
                    {
                        "$type": "OwningMembership",
                        "target": {
                            "$type": "Package",
                            "declaredName": "package"
                        }
                    }
                ]
            }
            """;
        Resource result = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()), new ResourceSetImpl());

        var content = result.getContents();
        assertEquals(1, content.size());
        EObject object = content.get(0);
        assertInstanceOf(Namespace.class, object);

        Namespace namespace = (Namespace) object;
        assertEquals(1, namespace.getOwnedElement().size());
        assertNotNull(namespace.getOwnedRelationship().get(0).getOwnedElement());
        assertEquals(1, namespace.getMember().size());
        EObject packageObject = namespace.getMember().get(0);
        assertInstanceOf(Package.class, packageObject);

        assertEquals("package", ((Package) packageObject).getDeclaredName());
        assertEquals("package", ((Package) packageObject).getQualifiedName());
    }

    /**
     * Test FeatureTyping
     */
    @Test
    void convertFeatureTypingTest() {
        ASTTransformer transformer = new ASTTransformer();

        Resource testResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertFeatureTypingTest/model.ast.json").readAllBytes());
            testResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  new ResourceSetImpl());
        } catch (IOException e) {
        }

        Namespace namespace = (Namespace) testResource.getContents().get(0);
        Package packageObject = (Package) namespace.getMember().get(0);

        EObject attributeDefinition = packageObject.getMember().get(0);
        assertInstanceOf(AttributeDefinition.class, attributeDefinition);
        AttributeDefinition typedAttributeDefinition = (AttributeDefinition) attributeDefinition;
        assertEquals("attributeDefinition", typedAttributeDefinition.getDeclaredName());
        assertEquals("package::attributeDefinition", typedAttributeDefinition.getQualifiedName());
        
        EObject portDefinition = packageObject.getMember().get(1);
        assertInstanceOf(PortDefinition.class, portDefinition);
        PortDefinition typedPortDefinition = (PortDefinition) portDefinition;
        assertEquals("portDefinition", typedPortDefinition.getDeclaredName());
        
        assertEquals(1, typedPortDefinition.getMember().size());
        EObject referenceUsage = typedPortDefinition.getMember().get(0);
        assertInstanceOf(ReferenceUsage.class, referenceUsage);
        ReferenceUsage typedReferenceUsage = (ReferenceUsage) referenceUsage;
        assertEquals("referenceUsage", typedReferenceUsage.getDeclaredName());

        assertEquals(1, typedReferenceUsage.getOwnedTyping().size());
        FeatureTyping featureTyping = typedReferenceUsage.getOwnedTyping().get(0);

        assertEquals(typedReferenceUsage, featureTyping.getTypedFeature());
        assertEquals(typedAttributeDefinition, featureTyping.getType());

        assertEquals(1, typedReferenceUsage.getOwnedRelationship().size());
        assertEquals(1, typedReferenceUsage.getOwnedSpecialization().size());
        Specialization specialisation = typedReferenceUsage.getOwnedSpecialization().get(0);
        assertEquals(typedAttributeDefinition, specialisation.getGeneral());
    }

    /**
     * Test Redefines
     */
    @Test
    void convertRedefinesTest() {
        ASTTransformer transformer = new ASTTransformer();

        Resource testResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertRedefinesTest/model.ast.json").readAllBytes());
            testResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  new ResourceSetImpl());
        } catch (IOException e) {
        }

        Namespace namespace = (Namespace) testResource.getContents().get(0);
        Package packageObject = (Package) namespace.getMember().get(0);

        EObject attributeUsage = packageObject.getMember().get(0);
        assertInstanceOf(AttributeUsage.class, attributeUsage);
        AttributeUsage typedAttributeUsage = (AttributeUsage) attributeUsage;
        assertEquals("Packets::'packet header'", typedAttributeUsage.getQualifiedName());


        EObject redefinedAttributeUsage = packageObject.getMember().get(1);
        assertInstanceOf(AttributeUsage.class, redefinedAttributeUsage);
        AttributeUsage typedRedefinedAttributeUsage = (AttributeUsage) redefinedAttributeUsage;
        assertEquals("Packets::'packet secondary header'", typedRedefinedAttributeUsage.getQualifiedName());

        assertEquals(1, typedRedefinedAttributeUsage.getOwnedRelationship().size());
        assertEquals(1, typedRedefinedAttributeUsage.getOwnedRedefinition().size());
        Redefinition redefinition = typedRedefinedAttributeUsage.getOwnedRedefinition().get(0);

        assertEquals(typedAttributeUsage, redefinition.getRedefinedFeature());
        assertEquals(typedRedefinedAttributeUsage, redefinition.getRedefiningFeature());
    }

    /**
     * Test NamespaceImport
     */
    @Test
    void convertNamespaceImportTest() {
        ASTTransformer transformer = new ASTTransformer();

        ResourceSet resourceSet = new ResourceSetImpl();

        // Import Namespace ScalarValues
        Resource namespaceResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertNamespaceImportTest/namespace.ast.json").readAllBytes());
            namespaceResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  resourceSet);
        } catch (IOException e) {
        }

        Resource testResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertNamespaceImportTest/model.ast.json").readAllBytes());
            testResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  resourceSet);
        } catch (IOException e) {
        }

        Namespace namespace = (Namespace) testResource.getContents().get(0);
        Package packageObject = (Package) namespace.getMember().get(0);

        EObject namespaceImport = packageObject.getOwnedRelationship().get(0);
        assertInstanceOf(NamespaceImport.class, namespaceImport);
        NamespaceImport typedNamespaceImport = (NamespaceImport) namespaceImport;
        assertEquals("ScalarValues", typedNamespaceImport.getImportedNamespace().getName());

        Namespace importedNamespace = (Namespace) namespaceResource.getContents().get(0);
        Package importedPackage = (Package) importedNamespace.getMember().get(0);
        assertEquals(importedPackage, typedNamespaceImport.getImportedNamespace());
    }

    /**
     * Test Conjugated
     */
    @Test
    void convertConjugatedPortTest() {
        ASTTransformer transformer = new ASTTransformer();

        Resource testResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertConjugatedPortTest/model.ast.json").readAllBytes());
            testResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  new ResourceSetImpl());
        } catch (IOException e) {
        }

        Namespace namespace = (Namespace) testResource.getContents().get(0);
        Package packageObject = (Package) namespace.getMember().get(0);

        AttributeDefinition tempAttributeDefinition = (AttributeDefinition) packageObject.getMember().get(0);
        assertEquals("Temp", tempAttributeDefinition.getName());
        
        PortDefinition tempPortPortDefinition = (PortDefinition) packageObject.getMember().get(1);
        assertEquals("TempPort", tempPortPortDefinition.getName());
        AttributeUsage temperatureAttributeUsage = (AttributeUsage) tempPortPortDefinition.getOwnedAttribute().get(0);
        assertEquals("temperature", temperatureAttributeUsage.getName());
        assertEquals(tempAttributeDefinition, temperatureAttributeUsage.getOwnedSpecialization().get(0).getGeneral());

        PartDefinition tempPortClassicPartDefinition = (PartDefinition) packageObject.getMember().get(2);
        assertEquals("TempPortClassic", tempPortClassicPartDefinition.getName());
        PortUsage tempPortClassicPortUsage = (PortUsage) tempPortClassicPartDefinition.getOwnedPort().get(0);
        assertEquals("tempPortClassic", tempPortClassicPortUsage.getName());
        assertEquals(tempPortPortDefinition, tempPortClassicPortUsage.getOwnedSpecialization().get(0).getGeneral());

        PartDefinition tempPortConjPartDefinition = (PartDefinition) packageObject.getMember().get(3);
        assertEquals("TempPortConj", tempPortConjPartDefinition.getName());
        PortUsage tempPortConjPortUsage = (PortUsage) tempPortConjPartDefinition.getOwnedPort().get(0);
        assertEquals("tempPortConj", tempPortConjPortUsage.getName());

        assertEquals(1, tempPortConjPortUsage.getOwnedTyping().size());
        ConjugatedPortTyping tempPortConjConjugatedPortTyping = (ConjugatedPortTyping) tempPortConjPortUsage.getOwnedTyping().get(0);
        
    }

    /**
     * Test Subclassification
     */
    @Test
    void convertSubclassificationTest() {
        ASTTransformer transformer = new ASTTransformer();

        Resource testResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertSubclassificationTest/model.ast.json").readAllBytes());
            testResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  new ResourceSetImpl());
        } catch (IOException e) {
        }

        Namespace namespace = (Namespace) testResource.getContents().get(0);
        Package packageObject = (Package) namespace.getMember().get(0);
        
        PartDefinition part1Definition = (PartDefinition) packageObject.getMember().get(0);
        assertEquals("Part1", part1Definition.getName());
        AttributeUsage attribute1AttributeUsage = (AttributeUsage) part1Definition.getOwnedAttribute().get(0);
        assertEquals("attribute1", attribute1AttributeUsage.getName());

        PartDefinition part2Definition = (PartDefinition) packageObject.getMember().get(1);
        assertEquals("Part2", part2Definition.getName());
        AttributeUsage attribute2AttributeUsage = (AttributeUsage) part2Definition.getOwnedAttribute().get(0);
        assertEquals("attribute2", attribute2AttributeUsage.getName());

        assertEquals(1, part2Definition.getOwnedSubclassification().size());
        assertEquals(part1Definition, part2Definition.getOwnedSubclassification().get(0).getSuperclassifier());
        assertEquals(part2Definition, part2Definition.getOwnedSubclassification().get(0).getSubclassifier());
        
    }

    /**
     * Test Alias
     */
    @Test
    void convertAliasTest() {
        ASTTransformer transformer = new ASTTransformer();

        Resource testResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertAliasTest/model.ast.json").readAllBytes());
            testResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  new ResourceSetImpl());
        } catch (IOException e) {
        }

        Namespace namespace = (Namespace) testResource.getContents().get(0);
        Package packageObject = (Package) namespace.getMember().get(0);
        Package definitionsPackage = (Package) packageObject.getMember().get(0);
        assertEquals("Definitions", definitionsPackage.getName());
        assertEquals("AliasImport::Definitions", definitionsPackage.getQualifiedName());
        PartDefinition vehiculePartDefinition = (PartDefinition) definitionsPackage.getOwnedElement().get(0);
        assertEquals("Vehicle", vehiculePartDefinition.getName());

        Membership carMembership = (Membership) definitionsPackage.getOwnedMembership().get(1);
        assertEquals("Car", carMembership.getName());
        assertEquals(vehiculePartDefinition, carMembership.getMemberElement());
        assertEquals(definitionsPackage, carMembership.getOwningRelatedElement());
        assertEquals(definitionsPackage, carMembership.getMembershipOwningNamespace());
        

        assertEquals(definitionsPackage.getOwnedMembership().get(0), namespace.resolve("AliasImport::Definitions::Vehicle"));

        assertEquals(vehiculePartDefinition, namespace.resolve("AliasImport::Definitions::Vehicle").getMemberElement());
        assertEquals(vehiculePartDefinition, namespace.resolve("AliasImport::Definitions::Car").getMemberElement());

        // TODO a checker corriger    
        //assertEquals("AliasImport::Definitions::Car", carMembership.getQualifiedName());
        //assertEquals(definitionsPackage, carMembership.getOwningNamespace());
        //assertEquals(definitionsPackage, carMembership.getOwner());

        Package usagesPackage = (Package) packageObject.getMember().get(1);
        assertEquals("Usages", usagesPackage.getName());

        PartUsage vehiculePartUsage = (PartUsage) usagesPackage.getOwnedElement().get(0);
        assertEquals("vehicle", vehiculePartUsage.getName());

        assertEquals(vehiculePartDefinition, vehiculePartUsage.getOwnedSpecialization().get(0).getGeneral());
    }


    /**
     * Test NamespaceImportValue
     */
    @Test
    void convertNamespaceImportValueTest() {
        ASTTransformer transformer = new ASTTransformer();

        ResourceSet resourceSet = new ResourceSetImpl();

        // Import Namespace ScalarValues
        Resource namespaceResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertNamespaceImportValueTest/namespace.ast.json").readAllBytes());
            namespaceResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  resourceSet);
        } catch (IOException e) {
        }

        Resource testResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertNamespaceImportValueTest/model.ast.json").readAllBytes());
            testResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  resourceSet);
        } catch (IOException e) {
        }

        Namespace importedNamespace = (Namespace) namespaceResource.getContents().get(0);
        Package package1Object = (Package) importedNamespace.getMember().get(0);
        PartDefinition part1Definition = (PartDefinition) package1Object.getMember().get(0);

        Namespace testNamespace = (Namespace) testResource.getContents().get(0);
        Package package2Object = (Package) testNamespace.getMember().get(0);

        NamespaceImport namespaceImport = (NamespaceImport) package2Object.getOwnedRelationship().get(0);
        assertEquals(package1Object, namespaceImport.getImportedNamespace());
        assertEquals(package2Object, namespaceImport.getImportOwningNamespace());
        assertEquals(null, namespaceImport.getOwningNamespace());
        assertEquals(testNamespace, namespaceImport.getOwningRelatedElement().getOwningNamespace());
        PartDefinition part2Definition = (PartDefinition) package2Object.getMember().get(0);

        assertEquals(1, part2Definition.getOwnedSubclassification().size());
        assertEquals(part1Definition, part2Definition.getOwnedSubclassification().get(0).getSuperclassifier());
        assertEquals(part2Definition, part2Definition.getOwnedSubclassification().get(0).getSubclassifier());
    }

    /**
     * Test convertImportTest
     */
    @Test
    void convertImportTest() {
        ASTTransformer transformer = new ASTTransformer();

        // Import Namespace ScalarValues
        Resource namespaceResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertImportTest/model.ast.json").readAllBytes());
            namespaceResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  new ResourceSetImpl());
        } catch (IOException e) {
        }

        Namespace testNamespace = (Namespace) namespaceResource.getContents().get(0);

        PartUsage p11 = (PartUsage) testNamespace.resolve("ImportTest::Pkg1::p11").getMemberElement();
        PartDefinition p211 = (PartDefinition) testNamespace.resolve("ImportTest::Pkg2::Pkg21::Pkg211::P211").getMemberElement();
        PartDefinition p12 = (PartDefinition) testNamespace.resolve("ImportTest::Pkg1::P12").getMemberElement();

        assertEquals(p211, p11.getOwnedSpecialization().get(0).getGeneral());
        assertEquals(p12, p211.getOwnedSubclassification().get(0).getSuperclassifier());
    }

    /**
     * Test Containement reference
     */
    @Test
    void convertDocumentationTest() {
        ASTTransformer transformer = new ASTTransformer();
        String fileContent = """
            {
                "$type": "Namespace",
                "children": [
                    {
                        "$type": "OwningMembership",
                        "target": {
                            "$type": "Documentation",
                            "body": "/* TEST */"
                        }
                    }
                ]
            }
            """;
        Resource result = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()), new ResourceSetImpl());

        var content = result.getContents();
        assertEquals(1, content.size());
        EObject object = content.get(0);
        assertInstanceOf(Namespace.class, object);

        Namespace namespace = (Namespace) object;
        assertEquals(1, namespace.getOwnedElement().size());
        assertNotNull(namespace.getOwnedRelationship().get(0).getOwnedElement());
        assertEquals(1, namespace.getMember().size());
        EObject documentationObject = namespace.getMember().get(0);
        assertInstanceOf(Documentation.class, documentationObject);

        assertEquals("TEST", ((Documentation) documentationObject).getBody());
    }


    /**
     * Test convertAssignmentTEst
     */
    @Test
    void convertAssignment1Test() {
        ASTTransformer transformer = new ASTTransformer();

        // Import Namespace ScalarValues
        Resource namespaceResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertAssignmentTest/assignment1.ast.json").readAllBytes());
            namespaceResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  new ResourceSetImpl());
        } catch (IOException e) {
        }

        Namespace namespace = (Namespace) namespaceResource.getContents().get(0);
        Package packageAssignment1 = (Package) namespace.getMember().get(0);

        PartDefinition partDefinitionCounter = (PartDefinition) packageAssignment1.getMember().get(0);

        AttributeUsage attributeUsagecount = (AttributeUsage) partDefinitionCounter.getMember().get(0);

        assertEquals(0, ((LiteralInteger) attributeUsagecount.getValuation().getValue()).getValue());
        
        ActionUsage actionUsageincr = (ActionUsage) partDefinitionCounter.getMember().get(1);

        AssignmentActionUsage assignmentActionUsage = (AssignmentActionUsage) actionUsageincr.getMember().get(0);

        //TODO Check
        assertEquals(attributeUsagecount, assignmentActionUsage.getReferent());

        //TODO Check
        //assertEquals(1, ((LiteralInteger) assignmentActionUsage.getTargetArgument()).getValue());

        Expression expression = assignmentActionUsage.getValueExpression(); //Expression -> 1

        //TODO Check
        assertEquals(null, expression);
        
    }

    /**
     * Test convertAssignmentTEst
     */
    @Test
    void convertAssignment2Test() {
        ASTTransformer transformer = new ASTTransformer();

        // Import Namespace ScalarValues
        Resource namespaceResource = null;
        try {
            String fileContent = new String(getClass().getClassLoader().getResourceAsStream("ASTTransformerTest/convertAssignmentTest/assignment2.ast.json").readAllBytes());
            namespaceResource = transformer.convertResource(new ByteArrayInputStream(fileContent.getBytes()),  new ResourceSetImpl());
        } catch (IOException e) {
        }

        Namespace namespace = (Namespace) namespaceResource.getContents().get(0);
        Package packageAssignment1 = (Package) namespace.getMember().get(0);

        ActionUsage actionUsageCounter = (ActionUsage) packageAssignment1.getMember().get(0);
        AttributeUsage attributeUsagecount = (AttributeUsage) actionUsageCounter.getMember().get(0);
        AssignmentActionUsage assignmentActionUsage = (AssignmentActionUsage) actionUsageCounter.getMember().get(1);

        //TODO Check
        assertEquals(attributeUsagecount, assignmentActionUsage.getReferent());

        //TODO Check
        OperatorExpression operatorExpression = (OperatorExpression) assignmentActionUsage.getTargetArgument();
        //assertEquals("+", operatorExpression.getOperator());
        // To implement assertEquals(1, (LiteralInteger) operatorExpression.getOperand().get(0));

        Expression expression = assignmentActionUsage.getValueExpression(); //Expression -> 1

        //TODO Check
        assertEquals(null, expression);

        
    }
}