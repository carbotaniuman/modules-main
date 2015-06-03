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
package de.cubeisland.engine.module.roles.sponge.collection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.config.RoleConfig;
import de.cubeisland.engine.module.roles.sponge.RolesPermissionService;
import de.cubeisland.engine.module.roles.sponge.subject.RoleSubject;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.world.WorldManager;
import de.cubeisland.engine.reflect.Reflector;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.world.World;

import static de.cubeisland.engine.module.core.filesystem.FileExtensionFilter.YAML;
import static org.spongepowered.api.service.permission.PermissionService.SUBJECTS_GROUP;

public class RoleCollection extends BaseSubjectCollection
{
    private final Map<String, String> mirrors;
    private final Map<String, RoleSubject> subjects = new ConcurrentHashMap<>();
    private Roles module;
    private RolesPermissionService service;
    private PermissionManager manager;
    private Reflector reflector;

    public RoleCollection(Roles module, RolesPermissionService service, PermissionManager manager, Reflector reflector, WorldManager wm)
    {
        super(SUBJECTS_GROUP);
        this.module = module;
        this.service = service;
        this.manager = manager;
        this.reflector = reflector;

        mirrors = readMirrors(service.getConfig().mirrors.roles);
        // TODO add missing selfreferencing mirrors

        Path modulePath = module.getProvided(Path.class);
        try
        {
            Files.createDirectories(modulePath.resolve("global"));
            Files.createDirectories(modulePath.resolve("world"));
            for (Path ctxType : Files.newDirectoryStream(modulePath))
            {
                if (Files.isDirectory(ctxType)) // ContextType
                {
                    for (Path ctxName : Files.newDirectoryStream(ctxType))
                    {
                        if (Files.isDirectory(ctxName))
                        {
                            for (Path file : Files.newDirectoryStream(ctxName, YAML))
                            {
                                RoleSubject subject = getRoleSubject(service, reflector, ctxType.getFileName().toString(), ctxName.getFileName().toString(), file.toFile());
                                subjects.put(subject.getIdentifier(), subject);
                            }
                        }
                        else
                        {
                            RoleSubject subject = getRoleSubject(service, reflector, ctxType.getFileName().toString(), "", ctxName.toFile());
                            subjects.put(subject.getIdentifier(), subject);
                        }
                    }

                    if ("world".equals(ctxType.getFileName().toString()))
                    {
                        wm.getWorlds().stream().map(World::getName)
                          .forEach(world -> {
                              if (!mirrors.containsKey(readMirror(world)) && !Files.exists(ctxType.resolve(world)))
                              {
                                  try
                                  {
                                      Files.createDirectories(ctxType.resolve(world));
                                  }
                                  catch (IOException e)
                                  {
                                      throw new IllegalStateException(e);
                                  }
                              }
                          });
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private RoleSubject getRoleSubject(RolesPermissionService service, Reflector reflector, String ctxType, String ctxName, File file)
    {
        RoleConfig config = reflector.create(RoleConfig.class);
        config.setFile(file);
        config.reload();
        Context context = new Context(ctxType, ctxName);
        return new RoleSubject(module, service, config, context, manager);
    }

    @Override
    public RoleSubject get(String identifier)
    {
        // TODO apply mirrors
        RoleSubject roleSubject = subjects.get(identifier);
        if (roleSubject == null)
        {
            if (!identifier.startsWith("role:"))
            {
                // must be <ctxType>|<ctxName>|<roleName>
                throw new IllegalArgumentException("Provided identifier is not a role: " + identifier);
            }
            String name = identifier.substring(5);
            String[] split = name.split("\\|");
            String ctxType = split[0];
            String ctxName = "";
            if (split.length == 3)
            {
                ctxName = split[1];
                name = split[2];
            }
            else if (split.length == 2)
            {
                name = split[1];
            }
            else
            {
                throw new IllegalArgumentException("Provided identifier has an invalid context: " + identifier);
            }
            Path path = module.getProvided(Path.class).resolve(ctxType);
            if (!ctxName.isEmpty())
            {
                path = path.resolve(ctxName);
            }
            RoleConfig config = reflector.create(RoleConfig.class);
            config.roleName = name;
            config.setFile(path.resolve(name + ".yml").toFile());
            roleSubject = new RoleSubject(module, service, config, "global".equals(ctxType) ? null : new Context(ctxType, ctxName), manager);

            subjects.put(identifier, roleSubject);
        }
        return roleSubject;
    }

    @Override
    public boolean hasRegistered(String identifier)
    {
        return subjects.containsKey(identifier);
    }

    @Override
    public Iterable<Subject> getAllSubjects()
    {
        return new ArrayList<>(subjects.values());
    }

    public boolean rename(RoleSubject role, String newName)
    {
        // TODO rename
        return false;
    }

    public void delete(RoleSubject r)
    {
        // TODO delete
    }
}