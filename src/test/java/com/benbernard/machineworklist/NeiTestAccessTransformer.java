package com.benbernard.machineworklist;

import java.io.IOException;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;

/** NEI's NBT access rules in MCP names, for the isolated deobfuscated test runtime. */
public class NeiTestAccessTransformer extends AccessTransformer {

    public NeiTestAccessTransformer() throws IOException {
        super("nei-test-at.cfg");
    }
}
