package org.sinytra.fabric.command_api;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;

public class FabricCommandApiV2 implements ModInitializer {
    @SuppressWarnings("rawtypes")
    private static final Map<Class, ArgumentSerializer<?, ?>> ARGUMENT_TYPE_CLASSES = new HashMap<>();
    private static final Map<Identifier, ArgumentSerializer<?, ?>> ARGUMENT_TYPES = new HashMap<>();

    @Override
    public void onInitialize() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(RegisterEvent.class, event -> {
            event.register(RegistryKeys.COMMAND_ARGUMENT_TYPE, helper -> {
                ARGUMENT_TYPE_CLASSES.forEach(ArgumentTypes::registerByClass);
                ARGUMENT_TYPES.forEach(helper::register);
            });
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> CommandRegistrationCallback.EVENT.invoker().register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));
    }

    public static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void registerArgumentType(Identifier id, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer) {
        ARGUMENT_TYPE_CLASSES.put(clazz, serializer);
        ARGUMENT_TYPES.put(id, serializer);
    }
}
