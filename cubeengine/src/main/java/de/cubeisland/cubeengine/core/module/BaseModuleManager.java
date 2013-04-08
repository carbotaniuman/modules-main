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
package de.cubeisland.cubeengine.core.module;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;

import de.cubeisland.cubeengine.core.Core;
import de.cubeisland.cubeengine.core.filesystem.FileExtentionFilter;
import de.cubeisland.cubeengine.core.module.event.ModuleDisabledEvent;
import de.cubeisland.cubeengine.core.module.event.ModuleEnabledEvent;
import de.cubeisland.cubeengine.core.module.exception.CircularDependencyException;
import de.cubeisland.cubeengine.core.module.exception.IncompatibleCoreException;
import de.cubeisland.cubeengine.core.module.exception.IncompatibleDependencyException;
import de.cubeisland.cubeengine.core.module.exception.InvalidModuleException;
import de.cubeisland.cubeengine.core.module.exception.MissingDependencyException;
import de.cubeisland.cubeengine.core.module.exception.MissingPluginDependencyException;
import de.cubeisland.cubeengine.core.module.exception.ModuleException;
import de.cubeisland.cubeengine.core.util.Profiler;
import de.cubeisland.cubeengine.core.util.Version;

import gnu.trove.map.hash.THashMap;
import org.apache.commons.lang.Validate;

import static de.cubeisland.cubeengine.core.logger.LogLevel.*;

public abstract class BaseModuleManager implements ModuleManager
{
    private final Logger logger;
    protected final Core core;
    private final ModuleLoader loader;
    private final Map<String, Module> modules;
    private final Map<String, ModuleInfo> moduleInfos;
    private final Map<Class<? extends Module>, Module> classMap;
    private final CoreModule coreModule;

    public BaseModuleManager(Core core, ClassLoader parentClassLoader)
    {
        this.core = core;
        this.logger = core.getLog();
        this.loader = new ModuleLoader(core, parentClassLoader);
        this.modules = new ConcurrentHashMap<String, Module>();
        this.moduleInfos = new ConcurrentHashMap<String, ModuleInfo>();
        this.classMap = new THashMap<Class<? extends Module>, Module>();
        this.coreModule = new CoreModule();
        this.coreModule.initialize(core, new ModuleInfo(core), core.getFileManager().getDataFolder(), core.getLog(), null, null);
    }

    public Module getModule(String name)
    {
        if (name == null)
        {
            return null;
        }
        return this.modules.get(name.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public <T extends Module> T getModule(Class<T> mainClass)
    {
        return (T)this.classMap.get(mainClass);
    }

    /**
     * This method returns a collection of the modules
     *
     * @return the modules
     */
    public Collection<Module> getModules()
    {
        return this.modules.values();
    }

    /**
     * Loads a module
     *
     * @param moduleFile the file to load the module from
     * @return the loaded module
     *
     * @throws InvalidModuleException           if the file is not a valid module
     * @throws CircularDependencyException      if the module defines a circular dependencies
     * @throws MissingDependencyException       if the module has a missing dependency
     * @throws IncompatibleDependencyException  if the module needs a newer dependency
     * @throws IncompatibleCoreException        if the module depends on a newer core
     * @throws MissingPluginDependencyException if the module depends on a missing plugin
     */
    public synchronized Module loadModule(File moduleFile) throws InvalidModuleException, CircularDependencyException, MissingDependencyException, IncompatibleDependencyException, IncompatibleCoreException, MissingPluginDependencyException
    {
        Validate.notNull(moduleFile, "The file must not be null!");
        if (!moduleFile.isFile())
        {
            throw new IllegalArgumentException("The given File is does not exist is not a normal file!");
        }

        ModuleInfo info = this.loader.loadModuleInfo(moduleFile);
        if (info == null)
        {
            throw new InvalidModuleException("Failed to load the module info for file '" + moduleFile.getName() + "'!");
        }

        ModuleInfo oldInfo = this.moduleInfos.put(info.getId(), info);
        if (oldInfo != null)
        {
            Module oldModule = this.modules.get(oldInfo.getId());
            if (oldModule != null)
            {
                this.unloadModule(oldModule);
            }
        }

        Module module = this.loadModule(info.getName(), this.moduleInfos);

        return module;
    }

    /**
     * This method loads all modules from a directory
     *
     * @param directory the directory to load from
     */
    public synchronized void loadModules(File directory)
    {
        Validate.notNull(directory, "The directory must not be null!");
        if (!directory.isDirectory())
        {
            throw new IllegalArgumentException("The given File is no directory!");
        }

        Module module;
        ModuleInfo info;
        this.logger.log(NOTICE, "Loading modules...");
        for (File file : directory.listFiles((FileFilter)FileExtentionFilter.JAR))
        {
            try
            {
                info = this.loader.loadModuleInfo(file);
                module = this.getModule(info.getId());
                if (module != null)
                {
                    if (module.getInfo().getVersion().compareTo(info.getVersion()) >= 0)
                    {
                        this.logger.log(WARNING, "A newer or equal version of the module '" + info.getName() + "' is already loaded!");
                        continue;
                    }
                    else
                    {
                        this.unloadModule(module);
                        this.logger.log(NOTICE, "A newer version of '" + info.getName() + "' will replace the currently loaded version!");
                    }
                }
                this.moduleInfos.put(info.getId(), info);
            }
            catch (InvalidModuleException e)
            {
                this.logger.log(ERROR, e.getLocalizedMessage(), e);
            }
        }

        for (String moduleName : this.moduleInfos.keySet())
        {
            try
            {
                this.loadModule(moduleName, this.moduleInfos);
            }
            catch (InvalidModuleException e)
            {
                this.moduleInfos.remove(moduleName);
                this.logger.log(DEBUG, "Failed to load the module '" + moduleName + "'", e);
            }
            catch (ModuleException e)
            {
                this.moduleInfos.remove(moduleName);
                this.logger.log(ERROR, "Failed to load the module '" + moduleName + "'", e);
            }
        }
        this.logger.log(NOTICE, "Finished loading modules!");
    }

    private Module loadModule(String name, Map<String, ModuleInfo> moduleInfos) throws CircularDependencyException, MissingDependencyException, InvalidModuleException, IncompatibleDependencyException, IncompatibleCoreException, MissingPluginDependencyException
    {
        return this.loadModule(name, moduleInfos, new Stack<String>());
    }

    protected abstract void validatePluginDependencies(Set<String> plugins) throws MissingPluginDependencyException;

    protected abstract Map<Class, Object> getPluginClassMap();

    @SuppressWarnings("unchecked")
    protected Module loadModule(String name, Map<String, ModuleInfo> moduleInfos, Stack<String> loadStack) throws CircularDependencyException, MissingDependencyException, InvalidModuleException, IncompatibleDependencyException, IncompatibleCoreException, MissingPluginDependencyException
    {
        name = name.toLowerCase(Locale.ENGLISH);
        Module module = this.modules.get(name);
        if (module != null)
        {
            return module;
        }

        if (loadStack.contains(name))
        {
            throw new CircularDependencyException(loadStack.pop(), name);
        }
        ModuleInfo info = moduleInfos.get(name);
        if (info == null)
        {
            return null;
        }
        loadStack.push(name);

        this.validatePluginDependencies(info.getPluginDependencies());

        for (String loadAfterModule : info.getLoadAfter())
        {
            if (!loadStack.contains(loadAfterModule))
            {
                this.loadModule(loadAfterModule, moduleInfos, loadStack);
            }
        }

        Module depModule;
        String depName;
        for (Map.Entry<String, Version> dep : info.getSoftDependencies().entrySet())
        {
            depName = dep.getKey();
            depModule = this.loadModule(depName, moduleInfos, loadStack);
            if (dep.getValue().isNewerThan(Version.ZERO) && depModule.getInfo().getVersion().isOlderThan(dep.getValue()))
            {
                this.logger.log(WARNING, "The module " + name + " requested a newer version of " + depName + "!");
            }
        }
        for (Map.Entry<String, Version> dep : info.getDependencies().entrySet())
        {
            depName = dep.getKey();
            depModule = this.loadModule(depName, moduleInfos, loadStack);
            if (depModule == null)
            {
                throw new MissingDependencyException(depName);
            }
            else
            {
                if (dep.getValue().isNewerThan(Version.ZERO) && depModule.getInfo().getVersion().isOlderThan(dep.getValue()))
                {
                    throw new IncompatibleDependencyException(name, depName, dep.getValue(), depModule.getInfo().getVersion());
                }
            }
        }
        module = this.loader.loadModule(info);
        loadStack.pop();

        Map<Class, Object> pluginClassMap = this.getPluginClassMap();
        Version requiredVersion;
        Module injectedModule;
        Class fieldType;
        for (Field field : module.getClass().getDeclaredFields())
        {
            fieldType = field.getType();
            if (Module.class.isAssignableFrom(fieldType))
            {
                injectedModule = this.classMap.get((Class<? extends Module>)fieldType);
                if (injectedModule == null)
                {
                    continue;
                }
                if (fieldType == module.getClass())
                {
                    continue;
                }
                requiredVersion = module.getInfo().getSoftDependencies().get(injectedModule.getId());
                if (requiredVersion != null && requiredVersion.isNewerThan(Version.ZERO) && injectedModule.getInfo().getVersion().isOlderThan(requiredVersion))
                {
                    continue;
                }
                field.setAccessible(true);
                try
                {
                    if (field.get(module) == null)
                    {
                        field.set(module, injectedModule);
                    }
                }
                catch (Exception e)
                {
                    module.getLog().log(WARNING, "Failed to inject a dependency: {0}", injectedModule.getName());
                }
            }
            else
            {
                if (Plugin.class.isAssignableFrom(fieldType))
                {
                    Object plugin = pluginClassMap.get(fieldType);
                    if (plugin == null)
                    {
                        continue;
                    }
                    field.setAccessible(true);
                    try
                    {
                        field.set(module, plugin);
                    }
                    catch (Exception e)
                    {
                        module.getLog().log(WARNING, "Failed to inject a plugin dependency: {0}", String.valueOf(plugin));
                    }
                }
            }
        }

        if (this.enableModule(module))
        {
            this.modules.put(module.getId(), module);
            this.classMap.put(module.getClass(), module);

            return module;
        }

        return null;
    }

    /**
     * Enables a module
     *
     * @param module the module
     * @return true if it succeeded
     */
    public synchronized boolean enableModule(Module module)
    {
        module.getLog().log(INFO, "Enabling version {0}...", module.getVersion());
        Profiler.startProfiling("enable-module");
        boolean result = module.enable();
        final long enableTime = Profiler.endProfiling("enable-module", TimeUnit.MICROSECONDS);
        if (!result)
        {
            module.getLog().log(ERROR, " Module failed to load.");
        }
        else
        {
            this.core.getEventManager().fireEvent(new ModuleEnabledEvent(this.core, module));
            module.getLog().log(INFO, "Successfully enabled within {0} microseconds!", enableTime);
        }
        return result;
    }

    /**
     * This method enables all modules that provide world generators
     */
    public void enableWorldGeneratorModules()
    {
        for (Module module : this.modules.values())
        {
            if (module.getInfo().providesWorldGenerator())
            {
                this.enableModule(module);
            }
        }
    }

    /**
     * This method enables all modules or at least all that don't provide world generators
     *
     * @param worldGenerators whether to also load the world generator-providing modules
     */
    public void enableModules(boolean worldGenerators)
    {
        for (Module module : this.modules.values())
        {
            if (!module.getInfo().providesWorldGenerator() || worldGenerators)
            {
                this.enableModule(module);
            }
        }
    }

    /**
     * This method disables a module
     *
     * @param module the module
     */
    public synchronized void disableModule(Module module)
    {
        Profiler.startProfiling("disable-module");
        module.disable();
        this.core.getUserManager().cleanup(module);
        this.core.getEventManager().removeListeners(module);
        this.core.getPermissionManager().removePermissions(module);
        this.core.getTaskManager().cancelTasks(module);
        this.core.getCommandManager().removeCommands(module);
        this.core.getApiServer().unregisterApiHandlers(module);

        this.core.getEventManager().fireEvent(new ModuleDisabledEvent(this.core, module));
        module.getLog().log(INFO, "Module disabled within {0} microseconds", Profiler.endProfiling("disable-module", TimeUnit.MICROSECONDS));
    }

    /**
     * This method tries to unload a module be removing as many references as possible.
     * this means:
     * - disable all modules that depend in the given module
     * - disable the module
     * - remove its ClassLoader and all the reference to it
     * - remove the module from the module map
     *
     * @param module the module to unload
     */
    public void unloadModule(Module module)
    {
        if (this.modules.remove(module.getId()) == null)
        {
            return;
        }
        Profiler.startProfiling("unload-" + module.getId());
        Set<Module> disable = new HashSet<Module>();
        for (Module m : this.modules.values())
        {
            if (m.getInfo().getDependencies().containsKey(module.getId()) || m.getInfo().getSoftDependencies().containsKey(module.getId()))
            {
                disable.add(m);
            }
        }

        for (Module m : disable)
        {
            this.unloadModule(m);
        }

        this.disableModule(module);
        this.loader.unloadModule(module);
        this.moduleInfos.remove(module.getId());

        // null all the fields referencing this module
        for (Module m : this.modules.values())
        {
            Class moduleClass = m.getClass();
            for (Field field : moduleClass.getDeclaredFields())
            {
                if (field.getType() == module.getClass())
                    try
                    {
                        field.setAccessible(true);
                        field.set(m, null);
                    }
                    catch (Exception ignored)
                    {}
            }
        }

        ClassLoader classLoader = module.getClassLoader();
        if (classLoader instanceof ModuleClassLoader)
        {
            ((ModuleClassLoader)classLoader).shutdown();
        }

        System.gc();
        System.gc();
        this.logger.log(DEBUG, "Unloading '" + module.getName() + "' took {0} milliseconds!", Profiler.endProfiling("unload-" + module.getId(), TimeUnit.MILLISECONDS));
    }

    public void reloadModule(Module module)
    {
        module.reload();
    }

    public int reloadModules()
    {
        int modules = 0;
        Iterator<Module> iter = this.getModules().iterator();
        while (iter.hasNext())
        {
            iter.next().reload();
            ++modules;
        }
        return modules;
    }

    /**
     * This method disables all modules
     */
    public void disableModules()
    {
        for (Module module : this.modules.values())
        {
            this.disableModule(module);
        }
    }

    /**
     * This method disables all modules
     */
    public void unloadModules()
    {
        Iterator<Map.Entry<String, Module>> iter = this.modules.entrySet().iterator();
        while (iter.hasNext())
        {
            this.unloadModule(iter.next().getValue());
            iter.remove();
        }
        this.modules.clear();
    }

    @Override
    public void clean()
    {
        this.logger.log(DEBUG, "unload modules");
        Profiler.startProfiling("unload-modules");
        this.unloadModules();
        this.logger.log(DEBUG, "Unloading the module took {0} milliseconds!", Profiler.endProfiling("unload-modules", TimeUnit.MILLISECONDS));
        this.modules.clear();
        this.moduleInfos.clear();
        this.logger.log(DEBUG, "shutting down the loader");
        this.loader.shutdown();
    }

    /**
     * Returns a dummy module
     *
     * @return the singleton instance of the dummy CoreModule
     */
    public CoreModule getCoreModule()
    {
        return this.coreModule;
    }
}
