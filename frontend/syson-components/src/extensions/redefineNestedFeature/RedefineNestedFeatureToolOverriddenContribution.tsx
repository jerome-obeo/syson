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
import { gql, useMutation } from '@apollo/client';
import {
  GQLMessage,
  Selection,
  useMultiToast,
  useSelection,
  useWorkbench,
} from '@eclipse-sirius/sirius-components-core';
import {
  DiagramContext,
  DiagramContextValue,
  EdgeData,
  NodeData,
  useDialog,
} from '@eclipse-sirius/sirius-components-diagrams';
import {
  fuzzyMatch,
  GQLToolVariable,
  isSingleClickOnDiagramElementTool,
  PaletteToolOverriddenContributionComponentProps,
  ToolListItem,
  usePalette,
} from '@eclipse-sirius/sirius-components-palette';
import { Edge, Node, useStoreApi } from '@xyflow/react';
import { useContext } from 'react';

const invokeToolMutation = gql`
  mutation redefineNestedFeature($input: InvokeSingleClickOnDiagramElementToolInput!) {
    invokeSingleClickOnDiagramElementTool(input: $input) {
      __typename
      ... on InvokeSingleClickOnDiagramElementToolSuccessPayload {
        id
        newSelection {
          entries {
            id
          }
        }
        messages {
          body
          level
        }
      }
      ... on ErrorPayload {
        messages {
          body
          level
        }
      }
    }
  }
`;

interface InvokeToolData {
  invokeSingleClickOnDiagramElementTool: {
    __typename: string;
    id: string;
    messages: GQLMessage[];
    newSelection?: Selection | null;
  };
}

export const RedefineNestedFeatureToolOverriddenContribution = ({
  representationElementIds,
  tool,
  onInvoked,
  isToolInPalette,
  searchedValue,
}: PaletteToolOverriddenContributionComponentProps) => {
  const { editingContextId, diagramId, readOnly, registerPostToolSelection } =
    useContext<DiagramContextValue>(DiagramContext);
  const { setSelection } = useSelection();
  const workbench = useWorkbench();
  const { addMessages, addErrorMessage } = useMultiToast();
  const { showDialog } = useDialog();
  const { hidePalette } = usePalette();
  const store = useStoreApi<Node<NodeData>, Edge<EdgeData>>();
  const [invokeTool, { loading }] = useMutation<InvokeToolData>(invokeToolMutation);

  if (searchedValue && !fuzzyMatch(tool.label, searchedValue).matches) {
    return null;
  }

  const nodeId = representationElementIds[0];
  const node = nodeId ? store.getState().nodeLookup.get(nodeId) : undefined;
  const disabled = readOnly || loading || !isToolInPalette || representationElementIds.length !== 1 || !node;

  const onToolClick = () => {
    if (disabled || !node || !isSingleClickOnDiagramElementTool(tool)) {
      return;
    }
    const executeTool = async (variables: GQLToolVariable[]) => {
      try {
        const { data } = await invokeTool({
          variables: {
            input: {
              id: crypto.randomUUID(),
              editingContextId,
              representationId: diagramId,
              diagramElementIds: representationElementIds,
              toolId: tool.id,
              startingPositionX: 0,
              startingPositionY: 0,
              variables,
            },
          },
        });
        const payload = data?.invokeSingleClickOnDiagramElementTool;
        if (!payload) {
          addErrorMessage('The tool did not return a result.');
          return;
        }
        addMessages(payload.messages);
        if (
          payload.__typename === 'InvokeSingleClickOnDiagramElementToolSuccessPayload' &&
          !payload.messages.some((message) => message.level === 'ERROR')
        ) {
          const selection = payload.newSelection;
          if (!selection?.entries.length) {
            addErrorMessage('The tool did not return an element to select.');
            return;
          }
          registerPostToolSelection(payload.id, selection);
          setSelection(selection);
          workbench
            .getWorkbenchPanelHandles()
            .flatMap((panel) => panel.getWorkbenchViewHandles())
            .find((view) => view.id === 'explorer')
            ?.applySelection?.(selection);
        }
      } catch {
        addErrorMessage('Unable to redefine the selected feature chain.');
      }
    };
    onInvoked();
    showDialog(
      tool.dialogDescriptionId,
      [{ name: 'targetObjectId', value: node.data.targetObjectId }],
      executeTool,
      () => {}
    );
    hidePalette();
  };

  return (
    <ToolListItem
      tool={tool}
      disabled={disabled}
      selected={false}
      searchedValue={searchedValue}
      onToolClick={onToolClick}
    />
  );
};
