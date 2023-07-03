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

package net.fabricmc.fabric.impl.resource.loader;

import java.nio.file.Path;
import java.util.List;

import com.mojang.datafixers.util.Either;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.language.IModInfo;

import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;

import net.fabricmc.fabric.impl.client.resource.loader.ResourceLoaderClient;
import net.fabricmc.loader.api.ModContainer;

@Mod("fabric_resource_loader_v0")
public class ResourceLoaderImpl {
	private static final boolean FABRIC_LOADED;

	static {
		boolean loaded;
		try {
			Class.forName("net.fabricmc.loader.api.ModContainer");
			loaded = true;
		} catch (ClassNotFoundException e) {
			loaded = false;
		}
		FABRIC_LOADED = loaded;
	}

	public ResourceLoaderImpl() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		if (FMLLoader.getDist() == Dist.CLIENT) {
			// Run first
			bus.addListener(ResourceLoaderClient::onClientResourcesReload);
		}
		bus.addListener(ResourceLoaderImpl::addPackFinders);
		MinecraftForge.EVENT_BUS.addListener(ResourceLoaderImpl::onServerDataReload);
	}

	private static void addPackFinders(AddPackFindersEvent event) {
		event.addRepositorySource(new ModResourcePackCreator(event.getPackType()));
	}

	private static void onServerDataReload(AddReloadListenerEvent event) {
		List<ResourceReloader> listeners = ResourceManagerHelperImpl.sort(ResourceType.SERVER_DATA, event.getListeners());
		listeners.forEach(event::addListener);
	}

	public static List<Path> getFabricModContainerPaths(Either<ModContainer, IModInfo> container) {
		if (FABRIC_LOADED) {
			return SafeApiHandler.getFabricModContainerPaths(container);
		}
		return List.of(container.right().orElseThrow().getOwningFile().getFile().findResource("."));
	}

	public static Either getFabricModContainerMetadata(Either<ModContainer, IModInfo> container) {
		if (FABRIC_LOADED) {
			return SafeApiHandler.getFabricModContainerMetadata(container);
		}
		return container;
	}
}
