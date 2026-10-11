package com.example.kadma.quest;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.ConfigManager;
import com.example.kadma.config.ItemConfig;
import com.example.kadma.config.ObjectiveDefinition;
import com.example.kadma.config.QuestConfig;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.InventoryUtil;
import com.example.kadma.util.TextUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Server-side objective evaluation.
 *
 * <p>Nothing here is triggered by a client packet. Events come from Forge's server
 * events (interact, death, tick position sampling) and the server recomputes counts
 * from the player's real inventory.</p>
 */
public class ObjectiveValidator {

    private final KadmaServices services;

    public ObjectiveValidator(KadmaServices services) {
        this.services = services;
    }

    // ---------------------------------------------------------------- movement

    /** Sample one player's position for goto / enter_layer / survive / timer. */
    public void tickPlayer(ServerPlayer player, PlayerData data, long worldTick) {
        for (com.example.kadma.data.QuestProgress progress : data.activeQuests) {
            if (progress.abandoned || progress.complete) {
                continue;
            }
            QuestConfig definition = services.config().quest(progress.questId);
            if (definition == null) {
                continue;
            }
            QuestInstance instance = new QuestInstance(definition, progress);
            for (Objective objective : instance.objectives()) {
                if (objective.complete()) {
                    continue;
                }
                ObjectiveDefinition spec = objective.definition;
                switch (objective.type()) {
                    case GOTO:
                        evaluateGoto(instance, objective, player);
                        break;
                    case ENTER_LAYER:
                        evaluateEnterLayer(instance, objective, player);
                        break;
                    case SURVIVE:
                        evaluateSurvive(instance, objective, player);
                        break;
                    case TIMER:
                        evaluateTimer(instance, objective, player);
                        break;
                    case COLLECT:
                    case DELIVER:
                        evaluateCollect(instance, objective, player);
                        break;
                    case REACH_FLAG:
                        evaluateReachFlag(instance, objective, data);
                        break;
                    default:
                        break;
                }
                if (objective.complete()) {
                    onObjectiveDone(instance, objective, player, data);
                }
            }
            if (instance.allComplete() && !progress.complete) {
                services.quests().completeQuest(player, data, instance.id(), true);
            }
        }
    }

    private void evaluateGoto(QuestInstance instance, Objective objective, ServerPlayer player) {
        ObjectiveDefinition spec = objective.definition;
        if (!spec.zone.isEmpty()) {
            if (services.zones().isInside(player, spec.zone)) {
                objective.complete(true);
            }
            return;
        }
        if (services.quests().hasGotoAnchor(instance.id())) {
            double distance = services.quests().gotoAnchorDistance(instance.id(), player.position());
            if (distance < 3.0D) {
                objective.complete(true);
            }
            return;
        }
        // zone-less goto objectives are satisfied by moving at least a short distance
        if (objective.current() == 0) {
            objective.setProgress(1);
        }
    }

    private void evaluateEnterLayer(QuestInstance instance, Objective objective, ServerPlayer player) {
        if (services.layers().currentLayerId(player).equals(objective.definition.layer)) {
            objective.complete(true);
        }
    }

    private void evaluateSurvive(QuestInstance instance, Objective objective, ServerPlayer player) {
        ObjectiveDefinition spec = objective.definition;
        boolean inside = spec.zone.isEmpty() || services.zones().isInside(player, spec.zone);
        if (!inside) {
            if (objective.progress.tickBudget > 0 && !spec.persistProgress) {
                objective.setProgress(0);
            }
            return;
        }
        if (objective.progress.tickBudget == 0) {
            objective.progress.tickBudget = spec.ticks;
        }
        objective.add(1);
    }

    private void evaluateTimer(QuestInstance instance, Objective objective, ServerPlayer player) {
        objective.add(1);
    }

    private void evaluateCollect(QuestInstance instance, Objective objective, ServerPlayer player) {
        ObjectiveDefinition spec = objective.definition;
        int available = InventoryUtil.countStoryItem(player, spec.itemId);
        if (spec.itemId.isEmpty()) {
            Item item = InventoryUtil.resolveItem(spec.itemId);
            available = item == null ? 0 : InventoryUtil.countItem(player, item);
        }
        if (available >= objective.amount()) {
            objective.setProgress(objective.amount());
        } else {
            objective.setProgress(available);
        }
    }

    private void evaluateReachFlag(QuestInstance instance, Objective objective, PlayerData data) {
        ObjectiveDefinition spec = objective.definition;
        String actual = data.flagValue(spec.flagKey);
        boolean hit = actual != null && (spec.flagValue.isEmpty() || spec.flagValue.equals(actual));
        if (hit) {
            objective.complete(true);
        }
    }

    // ------------------------------------------------------------- interactions

    /** NPC interaction. */
    public void onInteractNpc(ServerPlayer player, PlayerData data, String npcId) {
        for (com.example.kadma.data.QuestProgress progress : data.activeQuests) {
            if (progress.abandoned || progress.complete) {
                continue;
            }
            QuestConfig definition = services.config().quest(progress.questId);
            if (definition == null) {
                continue;
            }
            QuestInstance instance = new QuestInstance(definition, progress);
            boolean changed = false;
            for (Objective objective : instance.objectives()) {
                if (objective.complete()) {
                    continue;
                }
                ObjectiveDefinition spec = objective.definition;
                if (objective.type() == ObjectiveType.TALK && matchesNpc(spec, npcId)) {
                    objective.complete(true);
                    changed = true;
                } else if (objective.type() == ObjectiveType.INTERACT && matchesNpc(spec, npcId)) {
                    objective.complete(true);
                    changed = true;
                } else if (objective.type() == ObjectiveType.DELIVER && matchesNpc(spec, npcId)) {
                    int held = InventoryUtil.countStoryItem(player, spec.itemId);
                    if (held >= objective.amount()) {
                        if (spec.consume) {
                            InventoryUtil.removeStoryItem(player, spec.itemId, objective.amount());
                        }
                        objective.setProgress(objective.amount());
                        changed = true;
                    }
                }
                if (objective.complete() && changed) {
                    onObjectiveDone(instance, objective, player, data);
                }
            }
            if (changed && instance.allComplete()) {
                services.quests().completeQuest(player, data, instance.id(), true);
            }
        }
    }

    private boolean matchesNpc(ObjectiveDefinition spec, String npcId) {
        if (npcId == null || npcId.isEmpty()) {
            return false;
        }
        String a = spec.npcId.isEmpty() ? spec.npc : spec.npcId;
        return !a.isEmpty() && a.equals(npcId);
    }

    /** Block right click. */
    public void onInteractBlock(ServerPlayer player, PlayerData data, String blockId) {
        for (com.example.kadma.data.QuestProgress progress : data.activeQuests) {
            if (progress.abandoned || progress.complete) {
                continue;
            }
            QuestConfig definition = services.config().quest(progress.questId);
            if (definition == null) {
                continue;
            }
            QuestInstance instance = new QuestInstance(definition, progress);
            boolean changed = false;
            for (Objective objective : instance.objectives()) {
                if (objective.complete() || objective.type() != ObjectiveType.INTERACT) {
                    continue;
                }
                ObjectiveDefinition spec = objective.definition;
                if (spec.blockId.isEmpty()) {
                    continue;
                }
                String normalised = spec.blockId.contains(":") ? spec.blockId : "minecraft:" + spec.blockId;
                if (normalised.equals(blockId) || spec.blockId.equals(blockId)) {
                    objective.add(1);
                    changed = true;
                }
                if (objective.complete()) {
                    onObjectiveDone(instance, objective, player, data);
                }
            }
            if (changed && instance.allComplete()) {
                services.quests().completeQuest(player, data, instance.id(), true);
            }
        }
    }

    /** Right clicking with a story item in hand. */
    public void onUseItem(ServerPlayer player, PlayerData data, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String logical = logicalIdOf(stack);
        for (com.example.kadma.data.QuestProgress progress : data.activeQuests) {
            if (progress.abandoned || progress.complete) {
                continue;
            }
            QuestConfig definition = services.config().quest(progress.questId);
            if (definition == null) {
                continue;
            }
            QuestInstance instance = new QuestInstance(definition, progress);
            boolean changed = false;
            for (Objective objective : instance.objectives()) {
                if (objective.complete()) {
                    continue;
                }
                ObjectiveDefinition spec = objective.definition;
                if (objective.type() == ObjectiveType.USE_ITEM
                        && (spec.itemId.equals(logical) || spec.itemId.equals(InventoryUtil.resolveItem(spec.itemId) != null
                        && InventoryUtil.resolveItem(spec.itemId) == stack.getItem()))) {
                    objective.add(1);
                    changed = true;
                }
                if (objective.type() == ObjectiveType.COLLECT && spec.itemId.equals(logical)) {
                    evaluateCollect(instance, objective, player);
                    if (objective.complete()) {
                        changed = true;
                    }
                }
                if (objective.complete() && changed) {
                    onObjectiveDone(instance, objective, player, data);
                }
            }
            if (changed && instance.allComplete()) {
                services.quests().completeQuest(player, data, instance.id(), true);
            }
        }
    }

    private String logicalIdOf(ItemStack stack) {
        for (java.util.Map.Entry<String, ItemConfig> entry : services.config().items().entrySet()) {
            if (InventoryUtil.isStoryItem(stack, entry.getKey())) {
                return entry.getKey();
            }
        }
        return "";
    }

    /** Entity death. */
    public void onKill(ServerPlayer killer, PlayerData data, LivingEntity dead, Entity direct) {
        for (com.example.kadma.data.QuestProgress progress : data.activeQuests) {
            if (progress.abandoned || progress.complete) {
                continue;
            }
            QuestConfig definition = services.config().quest(progress.questId);
            if (definition == null) {
                continue;
            }
            QuestInstance instance = new QuestInstance(definition, progress);
            boolean changed = false;
            for (Objective objective : instance.objectives()) {
                if (objective.complete() || objective.type() != ObjectiveType.KILL) {
                    continue;
                }
                ObjectiveDefinition spec = objective.definition;
                if (!spec.entityType.isEmpty() && !matchesEntityType(dead, spec.entityType)) {
                    continue;
                }
                if (!spec.zone.isEmpty() && !services.zones().isInside(dead.position(), spec.zone)) {
                    continue;
                }
                objective.add(1);
                changed = true;
                if (objective.complete()) {
                    onObjectiveDone(instance, objective, killer, data);
                }
            }
            if (changed && instance.allComplete()) {
                services.quests().completeQuest(killer, data, instance.id(), true);
            }
        }
    }

    private boolean matchesEntityType(LivingEntity entity, String wanted) {
        if (wanted == null || wanted.isEmpty()) {
            return true;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(entity.getType()).toString();
        if (id.equals(wanted)) {
            return true;
        }
        String simple = wanted.contains(":") ? wanted.substring(wanted.indexOf(':') + 1) : wanted;
        return simple.equals(id) || simple.equals(entity.getType().getDescriptionId());
    }

    /** Dialogue choice objective. Called from the dialogue command after validation. */
    public void onChoose(ServerPlayer player, PlayerData data, String dialogueId, String nodeId, int choiceIndex) {
        for (com.example.kadma.data.QuestProgress progress : data.activeQuests) {
            if (progress.abandoned || progress.complete) {
                continue;
            }
            QuestConfig definition = services.config().quest(progress.questId);
            if (definition == null) {
                continue;
            }
            QuestInstance instance = new QuestInstance(definition, progress);
            boolean changed = false;
            for (Objective objective : instance.objectives()) {
                if (objective.complete() || objective.type() != ObjectiveType.CHOOSE) {
                    continue;
                }
                ObjectiveDefinition spec = objective.definition;
                boolean dialogueMatches = spec.dialogueId.isEmpty() || spec.dialogueId.equals(dialogueId);
                boolean nodeMatches = spec.nodeId.isEmpty() || spec.nodeId.equals(nodeId);
                boolean choiceMatches = spec.choiceIndex < 0 || spec.choiceIndex == choiceIndex;
                if (dialogueMatches && nodeMatches && choiceMatches) {
                    objective.complete(true);
                    changed = true;
                    onObjectiveDone(instance, objective, player, data);
                }
            }
            if (changed && instance.allComplete()) {
                services.quests().completeQuest(player, data, instance.id(), true);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private void onObjectiveDone(QuestInstance instance, Objective objective, ServerPlayer player, PlayerData data) {
        services.data().markPlayer(player.getUUID());
        if (services.config().settings().features.visualEffects) {
            services.visual().objectiveDone(player, objective);
        }
        player.sendSystemMessage(TextUtil.ok("\u0426\u0435\u043b\u044c: " + objective.definition.description));
    }

    /** Number of entities in a zone, used by "kill waves" event phases. */
    public int countEntitiesIn(ServerLevel level, String zoneId, String entityType) {
        com.example.kadma.zone.ZoneRegion zone = services.zones().byId(zoneId);
        if (zone == null) {
            return 0;
        }
        AABB box = zone.box.inflate(2.0D);
        List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> !(entity instanceof ServerPlayer));
        int count = 0;
        for (LivingEntity entity : living) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            if (id.equals(entityType) || entityType.endsWith(id)) {
                count++;
            }
        }
        return count;
    }

    public List<LivingEntity> taggedTargets(ServerLevel level, String questId) {
        List<LivingEntity> found = new java.util.ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof LivingEntity living
                    && questId.equals(com.example.kadma.util.EntityUtil.questTargetId(living))) {
                found.add(living);
            }
        }
        return found;
    }
}
