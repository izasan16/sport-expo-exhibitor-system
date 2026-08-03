import type { DataNode } from 'antd/es/tree';
import type { PermissionResourceType, PermissionTreeNode } from '../../../app/types/rbac';

export function buildCheckedPermissionKeys(permissionCodes: string[]) {
  return [...new Set(permissionCodes)].sort();
}

export function diffPermissionSelection(
  oldCodes: string[],
  newCodes: string[],
  labels: Record<string, string>,
) {
  const oldSet = new Set(oldCodes);
  const newSet = new Set(newCodes);
  return {
    added: [...newSet].filter((code) => !oldSet.has(code)).map((code) => labels[code] ?? code),
    removed: [...oldSet].filter((code) => !newSet.has(code)).map((code) => labels[code] ?? code),
  };
}

export function buildPermissionLabelMap(nodes: PermissionTreeNode[]) {
  const labels: Record<string, string> = {};
  const visit = (items: PermissionTreeNode[]) => {
    items.forEach((item) => {
      labels[item.code] = item.name;
      visit(item.children ?? []);
    });
  };
  visit(nodes);
  return labels;
}

export function flattenPermissionTree(nodes: PermissionTreeNode[]) {
  const flat: PermissionTreeNode[] = [];
  const visit = (items: PermissionTreeNode[]) => {
    items.forEach((item) => {
      flat.push(item);
      visit(item.children ?? []);
    });
  };
  visit(nodes);
  return flat;
}

export function filterPermissionTreeByType(
  nodes: PermissionTreeNode[],
  resourceType: PermissionResourceType | 'ALL',
): PermissionTreeNode[] {
  if (resourceType === 'ALL') {
    return nodes;
  }

  return nodes
    .map((node) => {
      const children = filterPermissionTreeByType(node.children ?? [], resourceType);
      if (node.resourceType === resourceType || children.length > 0) {
        return { ...node, children };
      }
      return null;
    })
    .filter((node): node is PermissionTreeNode => Boolean(node));
}

export function toPermissionTreeData(nodes: PermissionTreeNode[]): DataNode[] {
  return nodes.map((node) => ({
    key: node.code,
    title: node.name,
    children: toPermissionTreeData(node.children ?? []),
  }));
}
