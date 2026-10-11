package com.example.kadma.dialogue;

/**
 * Presentation helper for chat based dialogue.
 *
 * <p>Choices are clickable chat components that execute a server command. Everything
 * the player sees is built here from server data only.</p>
 */
public final class DialogueChatSender {

    public static final String COMMAND = DialogueService.COMMAND;

    private DialogueChatSender() {
    }

    /** The exact command a choice line runs. */
    public static String command(String dialogueId, String nodeId, int choiceIndex) {
        return COMMAND + dialogueId + " " + nodeId + " " + choiceIndex;
    }

    /** Root hint shown after the first conversation so the player can reopen it. */
    public static String reopenCommand(String dialogueId, String nodeId) {
        return "/kadma dialogue open " + dialogueId + (nodeId.isEmpty() ? "" : " " + nodeId);
    }
}
