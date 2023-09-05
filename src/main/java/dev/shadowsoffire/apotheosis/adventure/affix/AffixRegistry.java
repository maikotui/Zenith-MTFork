package dev.shadowsoffire.apotheosis.adventure.affix;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.shadowsoffire.apotheosis.adventure.Adventure.Affixes;
import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.Adventure;
import dev.shadowsoffire.apotheosis.adventure.AdventureModule;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.*;
import dev.shadowsoffire.apotheosis.adventure.affix.socket.SocketAffix;
import dev.shadowsoffire.apotheosis.adventure.client.AdventureModuleClient;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class AffixRegistry extends DynamicRegistry<Affix> {

    public static final AffixRegistry INSTANCE = new AffixRegistry();

    private Multimap<AffixType, DynamicHolder<Affix>> byType = ImmutableMultimap.of();

    public AffixRegistry() {
        super(AdventureModule.LOGGER, "affixes", true, true);
    }

    @Override
    protected void beginReload() {
        if (!Apotheosis.enableAdventure) return;
        super.beginReload();
        this.byType = ImmutableMultimap.of();
    }

    @Override
    protected void onReload() {
        if (!Apotheosis.enableAdventure) return;
        super.onReload();
        ImmutableMultimap.Builder<AffixType, DynamicHolder<Affix>> builder = ImmutableMultimap.builder();
        this.registry.values().forEach(a -> builder.put(a.type, this.holder(a.getId())));
        this.byType = builder.build();
        Preconditions.checkArgument(Affixes.SOCKET.get() instanceof SocketAffix, "Socket Affix not registered!");
        Preconditions.checkArgument(Affixes.DURABLE.get() instanceof DurableAffix, "Durable Affix not registered!");
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            AdventureModuleClient.checkAffixLangKeys();
        }
        RarityRegistry.INSTANCE.validateLootRules();
    }

    @Override
    protected void registerBuiltinSerializers() {
        this.registerSerializer(Apotheosis.loc("attribute"), AttributeAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("mob_effect"), PotionAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("damage_reduction"), DamageReductionAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("catalyzing"), CatalyzingAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("cleaving"), CleavingAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("enlightened"), EnlightenedAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("executing"), ExecutingAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("festive"), FestiveAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("magical"), MagicalArrowAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("omnetic"), OmneticAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("psychic"), PsychicAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("radial"), RadialAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("retreating"), RetreatingAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("spectral"), SpectralShotAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("telepathic"), TelepathicAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("thunderstruck"), ThunderstruckAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("socket"), SocketAffix.SERIALIZER);
        this.registerSerializer(Apotheosis.loc("durable"), DurableAffix.SERIALIZER);
    }

    public Multimap<AffixType, DynamicHolder<Affix>> getTypeMap() {
        return this.byType;
    }

}
