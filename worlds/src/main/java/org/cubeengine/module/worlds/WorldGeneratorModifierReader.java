/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.worlds;

import static java.util.stream.Collectors.toList;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorldGeneratorModifierReader implements ArgumentReader<WorldGeneratorModifier>, Completer
{
    @Override
    public WorldGeneratorModifier read(Class aClass, CommandInvocation invocation) throws ReaderException
    {
        String token = invocation.currentToken();
        Optional<WorldGeneratorModifier> generator = Sponge.getRegistry().getType(WorldGeneratorModifier.class, token);
        if (generator.isPresent())
        {
            invocation.consume(1);
            return generator.get();
        }
        throw new ReaderException("No Generator found for {}", token);
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        String token = invocation.currentToken();
        return Sponge.getRegistry().getAllOf(WorldGeneratorModifier.class).stream()
                .filter(wgm -> wgm.getId().startsWith(token))
                .map(CatalogType::getId)
                .collect(toList());
    }
}
