package de.cubeisland.cubeengine.core.command.reflected.readable;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.CommandFactory;
import de.cubeisland.cubeengine.core.command.reflected.ReflectedCommandFactory;
import de.cubeisland.cubeengine.core.logger.LogLevel;
import de.cubeisland.cubeengine.core.module.Module;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static de.cubeisland.cubeengine.core.util.Misc.arr;

public class ReadableCommandFactory extends ReflectedCommandFactory<ReadableCommand>
{
    private static final Logger LOGGER = CubeEngine.getLogger();

    public Class<ReadableCommand> getCommandType()
    {
        return ReadableCommand.class;
    }

    protected Class<? extends Annotation> getAnnotationType()
    {
        return ReadableCmd.class;
    }

    protected ReadableCommand buildCommand(Module module, Object holder, Method method, Annotation rawAnnotation)
    {
        ReadableCmd annotation = (ReadableCmd)rawAnnotation;

        String[] commandNames = annotation.names();
        if (commandNames.length == 0)
        {
            commandNames = arr(method.getName());
        }

        String name = commandNames[0].trim().toLowerCase(Locale.ENGLISH);
        List<String> aliases = new ArrayList<String>(commandNames.length - 1);
        for (int i = 1; i < commandNames.length; ++i)
        {
            aliases.add(commandNames[i].toLowerCase(Locale.ENGLISH));
        }

        Pattern pattern;
        try
        {
            pattern = Pattern.compile(annotation.pattern(), annotation.patternFlags());
        }
        catch (PatternSyntaxException e)
        {
            LOGGER.log(LogLevel.WARNING, "The pattern of a readable command failed to compile! ''{0}.{1}''", arr(holder.getClass().getSimpleName(), method.getName()));
            return null;
        }

        ReadableCommand cmd = new ReadableCommand(
            module,
            holder,
            method,
            name,
            annotation.desc(),
            annotation.usage(),
            aliases,
            pattern
        );
        cmd.setAsync(annotation.async());
        cmd.setLoggable(annotation.loggable());
        return cmd;
    }
}