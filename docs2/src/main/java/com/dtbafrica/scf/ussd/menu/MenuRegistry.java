package com.dtbafrica.scf.ussd.menu;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Spring collects every MenuNode bean; this asserts the tree is complete at startup.
 * A node declared in {@link NodeId} but never implemented fails the context refresh
 * rather than surfacing when a supplier first reaches that screen.
 */
@Component
public class MenuRegistry {

    private final Map<NodeId, MenuNode> nodes = new EnumMap<>(NodeId.class);

    public MenuRegistry(List<MenuNode> beans) {
        for (MenuNode node : beans) {
            MenuNode existing = nodes.put(node.id(), node);
            if (existing != null) {
                throw new IllegalStateException(
                        "Two MenuNode beans claim %s: %s and %s".formatted(
                                node.id(),
                                existing.getClass().getName(),
                                node.getClass().getName()));
            }
        }
        EnumSet<NodeId> missing = EnumSet.allOf(NodeId.class);
        missing.removeAll(nodes.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("No MenuNode bean for: " + missing);
        }
    }

    public MenuNode get(NodeId id) {
        MenuNode node = nodes.get(id);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node " + id);
        }
        return node;
    }
}
