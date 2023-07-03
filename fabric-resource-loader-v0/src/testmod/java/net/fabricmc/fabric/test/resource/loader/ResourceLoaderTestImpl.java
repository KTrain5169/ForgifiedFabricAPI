/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.test.resource.loader;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(ResourceLoaderTestImpl.MODID)
public class ResourceLoaderTestImpl {
    public static final String MODID = "fabric_resource_loader_v0_testmod";

    public ResourceLoaderTestImpl() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            MinecraftForge.EVENT_BUS.addListener(LanguageTestMod::onInitializeServer);
        }
        BuiltinResourcePackTestMod.onInitialize();
        ResourceReloadListenerTestMod.onInitialize();
        VanillaBuiltinResourcePackInjectionTestMod.onInitialize(bus);
    }
}
