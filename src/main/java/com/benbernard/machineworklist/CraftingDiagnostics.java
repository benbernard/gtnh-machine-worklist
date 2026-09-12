package com.benbernard.machineworklist;

/** Compact local request diagnostics; no inventory contents, identifiers or packet trace. */
final class CraftingDiagnostics {

    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager
        .getLogger("MachineWorklistCrafting");

    private CraftingDiagnostics() {}

    static void event(String message) {
        LOG.info(message);
    }
}
