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

package net.fabricmc.fabric.test.rendering.client;

import java.util.Objects;

import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.test.rendering.TooltipComponentTestInit;

public class TooltipComponentTests {

	public static void onInitializeClient() {
		TooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof TooltipComponentTestInit.Data d) {
				return TooltipComponent.of(Text.literal(d.string()).setStyle(Style.EMPTY.withColor(Formatting.GREEN)).asOrderedText());
			}

			return null;
		});

		// Test that TooltipComponent.of works with custom TooltipData
		Objects.requireNonNull(TooltipComponent.of(new TooltipComponentTestInit.Data("Hello world!")));
	}
}
