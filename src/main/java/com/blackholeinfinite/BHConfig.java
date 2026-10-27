package com.blackholeinfinite;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Todas as opcoes. Nao existe nenhum tamanho/massa maxima configuravel. */
public class BHConfig {
    public static Configuration cfg;

    // crescimento
    public static double startMass, blockMass, entityMass, growthMultiplier, accelerationFactor;
    // gravidade: raio = gravBase * massa^gravExponent (1->5, 100->50, 1e6->5000)
    public static double gravBase, gravExponent, gravIntensity, suctionSpeed, maxEntitySpeed;
    // destruicao / visual
    public static double destroyBase, destroyExponent, visualRatio, killZoneMin;
    // desempenho (controlam trabalho por tick, NAO a evolucao)
    public static int absorbBase, maxAbsorbPerTick, columnsPerTick, maxScanRadius, loadChunksPerTick,
            maxActiveHoles, syncInterval, maxVisualBlocksPerSync;
    public static boolean playersPulled, playersAbsorbed, entitiesEnabled, visualEffects, sounds,
            globalExpansion, absorbLiquids, protectUnbreakable, generatorEnabled;
    public static Set<String> protectedBlocks = new HashSet<String>();
    public static Set<String> absorbableBlocks = new HashSet<String>();

    private static double d(String cat, String key, double def, String c) {
        return cfg.get(cat, key, def, c).getDouble(def);
    }

    public static void load(File f) {
        cfg = new Configuration(f);
        String G = "growth", P = "gravity", D = "destruction", V = "visual", PF = "performance", B = "blocks", E = "entities", M = "misc";

        startMass = Math.max(1e-3, d(G, "startMass", 1.0, "Massa inicial"));
        blockMass = Math.max(0, d(G, "massPerBlock", 1.0, "Massa ganha por bloco absorvido"));
        entityMass = Math.max(0, d(G, "massPerEntity", 2.0, "Massa por entidade (itens: multiplicada pelo tamanho da pilha)"));
        growthMultiplier = Math.max(0, d(G, "growthSpeed", 1.0, "Multiplicador da velocidade de crescimento"));
        accelerationFactor = Math.max(0, d(G, "accelerationFactor", 1.0, "Quanto a velocidade de absorcao acelera com log10(massa)"));

        gravBase = Math.max(0.1, d(P, "gravityRadiusBase", 5.0, "Formula: alcance = base * massa^expoente"));
        gravExponent = Math.max(0.05, d(P, "gravityRadiusExponent", 0.5, "Expoente do alcance gravitacional"));
        gravIntensity = Math.max(0, d(P, "gravityIntensity", 0.04, "Aceleracao base de atracao (blocos/tick^2)"));
        suctionSpeed = Math.max(0, d(P, "suctionSpeed", 1.0, "Multiplicador da velocidade de sucção"));
        maxEntitySpeed = Math.max(0.1, d(P, "maxEntitySpeed", 3.0, "Velocidade maxima que a atracao imprime em entidades"));

        destroyBase = Math.max(0.5, d(D, "destroyRadiusBase", 2.0, "Raio de destruicao = base * massa^expoente"));
        destroyExponent = Math.max(0.05, d(D, "destroyRadiusExponent", 0.45, "Mantenha menor que o expoente gravitacional"));
        killZoneMin = Math.max(0.5, d(D, "entityKillRadiusMin", 1.5, "Raio minimo de absorcao de entidades"));
        visualRatio = Math.max(0.01, d(V, "visualToDestroyRatio", 0.5, "Raio visual da bola = raio de destruicao x esta razao (a bola cresce junto com o buraco negro)"));

        absorbBase = cfg.getInt("absorbBasePerTick", PF, 6, 1, 1000000, "Blocos absorvidos por tick com massa 1 (cresce com a massa)");
        maxAbsorbPerTick = cfg.getInt("maxAbsorbPerTick", PF, 1500, 1, 10000000, "Teto de blocos absorvidos por tick POR buraco negro (protege o servidor)");
        columnsPerTick = cfg.getInt("columnsPerTick", PF, 192, 1, 1000000, "Colunas de 1x256 blocos examinadas por tick");
        maxScanRadius = cfg.getInt("maxScanRadiusBlocks", PF, 512, 16, 100000, "Raio maximo varrido POR TICK-CICLO (o raio logico continua crescendo; so chunks carregados sao varridos)");
        loadChunksPerTick = cfg.getInt("loadChunksPerTick", PF, 1, 0, 64, "Chunks que podem ser carregados por tick (so se globalExpansion=true)");
        maxActiveHoles = cfg.getInt("maxActiveHoles", PF, 32, 0, 100000, "Quantidade de buracos negros simultaneos (0 = sem limite)");
        syncInterval = cfg.getInt("syncIntervalTicks", PF, 4, 1, 200, "Intervalo de sincronizacao com clientes");
        maxVisualBlocksPerSync = cfg.getInt("flyingBlocksPerSync", PF, 60, 0, 1000, "Blocos absorvidos enviados aos clientes para a animacao de succao");

        playersPulled = cfg.getBoolean("playersPulled", E, true, "Jogadores (survival) sao atraidos");
        playersAbsorbed = cfg.getBoolean("killPlayers", E, true, "Jogadores (survival) morrem ao chegar ao nucleo");
        entitiesEnabled = cfg.getBoolean("entitiesEnabled", E, true, "Atrai/absorve itens, animais, monstros e outras entidades");

        visualEffects = cfg.getBoolean("visualEffects", V, true, "Efeitos visuais e particulas (cliente)");
        sounds = cfg.getBoolean("sounds", V, true, "Sons graves (servidor)");

        globalExpansion = cfg.getBoolean("globalExpansion", M, false, "Se true, carrega alguns chunks por tick para continuar expandindo (pesado)");
        generatorEnabled = cfg.getBoolean("generatorEnabled", M, true, "Permite usar o item Black Hole Generator");

        absorbLiquids = cfg.getBoolean("absorbLiquids", B, true, "Absorver liquidos");
        protectUnbreakable = cfg.getBoolean("protectUnbreakable", B, true, "Proteger blocos com dureza negativa (bedrock, portais, barreira)");
        protectedBlocks = norm(cfg.getStringList("protectedBlocks", B, new String[]{
                "minecraft:bedrock", "minecraft:portal", "minecraft:end_portal", "minecraft:end_portal_frame",
                "minecraft:barrier", "minecraft:command_block"}, "Blocos que nunca sao absorvidos"));
        absorbableBlocks = norm(cfg.getStringList("absorbableBlocksWhitelist", B, new String[0],
                "Se nao vazia, SO estes blocos sao absorvidos (vazia = todos exceto protegidos)"));

        if (cfg.hasChanged()) cfg.save();
    }

    private static Set<String> norm(String[] in) {
        Set<String> s = new HashSet<String>();
        for (String x : in) {
            x = x.trim().toLowerCase(Locale.ROOT);
            if (x.isEmpty()) continue;
            if (x.indexOf(':') < 0) x = "minecraft:" + x;
            s.add(x);
        }
        return s;
    }
}
